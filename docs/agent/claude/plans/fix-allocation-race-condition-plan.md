# Fix Race Condition in Seat Allocation

## Problem
`AllocationService.java:57-58` has a race condition when allocating seats:

```java
Allocation allocation = loadAllocationPort.loadAllocationByMatchAndSeatWithLock(matchId, seatId)
        .orElse(Allocation.available(seatId));  // ← Race condition here
```

When no allocation record exists:
1. `PESSIMISTIC_WRITE` lock acquires nothing (no row to lock)
2. Both threads get `Optional.empty()` → create new `Allocation.available(seatId)`
3. Both call `recordAllocationPort.recordAllocation()` → one fails with unique constraint violation

## Solution: PostgreSQL Upsert with Exception Handling

Use atomic INSERT with ON CONFLICT, plus graceful exception handling as a safety net.

## Files to Modify

### 1. AllocationRepository.java
**Path:** `backend/src/main/java/dev/ticketing/core/site/adapter/out/persistence/allocation/AllocationRepository.java`

Add native upsert query:
```java
@Modifying
@Query(value = """
    INSERT INTO allocations (user_id, match_id, seat_id, status, hold_expires_at, updated_at)
    VALUES (:userId, :matchId, :seatId, 'HOLD', :holdExpiresAt, NOW())
    ON CONFLICT (match_id, seat_id) DO UPDATE
    SET user_id = CASE
        WHEN allocations.status = 'AVAILABLE'
             OR (allocations.status = 'HOLD' AND allocations.hold_expires_at < NOW())
        THEN EXCLUDED.user_id
        ELSE allocations.user_id
    END,
    status = CASE
        WHEN allocations.status = 'AVAILABLE'
             OR (allocations.status = 'HOLD' AND allocations.hold_expires_at < NOW())
        THEN 'HOLD'
        ELSE allocations.status
    END,
    hold_expires_at = CASE
        WHEN allocations.status = 'AVAILABLE'
             OR (allocations.status = 'HOLD' AND allocations.hold_expires_at < NOW())
        THEN EXCLUDED.hold_expires_at
        ELSE allocations.hold_expires_at
    END,
    updated_at = NOW()
    RETURNING *
    """, nativeQuery = true)
Optional<AllocationEntity> tryHoldSeat(
    @Param("userId") Long userId,
    @Param("matchId") Long matchId,
    @Param("seatId") Long seatId,
    @Param("holdExpiresAt") LocalDateTime holdExpiresAt
);
```

### 2. RecordAllocationPort.java
**Path:** `backend/src/main/java/dev/ticketing/core/site/application/port/out/persistence/allocation/RecordAllocationPort.java`

Add new method:
```java
Optional<Allocation> tryHoldSeat(Long userId, Long matchId, Long seatId, LocalDateTime holdExpiresAt);
```

### 3. AllocationPersistenceAdapter.java
**Path:** `backend/src/main/java/dev/ticketing/core/site/adapter/out/persistence/allocation/AllocationPersistenceAdapter.java`

Implement the new port method:
```java
@Override
public Optional<Allocation> tryHoldSeat(Long userId, Long matchId, Long seatId, LocalDateTime holdExpiresAt) {
    return allocationRepository.tryHoldSeat(userId, matchId, seatId, holdExpiresAt)
            .filter(entity -> userId.equals(entity.getUserId())
                    && entity.getStatus() == AllocationStatus.HOLD)
            .map(AllocationEntity::toDomain);
}
```

### 4. AllocationService.java
**Path:** `backend/src/main/java/dev/ticketing/core/site/application/service/AllocationService.java`

Refactor `allocateSeat()` method:
- First try to load existing allocation with lock (handles updates)
- If no existing allocation, use atomic `tryHoldSeat()`
- If atomic insert fails (another user won), throw `SeatAlreadyHeldException`

### 5. (Optional) New Exception Class
**Path:** `backend/src/main/java/dev/ticketing/core/site/application/service/exception/SeatAllocationConflictException.java`

For more specific error messaging when race condition is detected.

## Implementation Steps

1. Add `tryHoldSeat()` native query to `AllocationRepository`
2. Add port method to `RecordAllocationPort`
3. Implement in `AllocationPersistenceAdapter`
4. Refactor `AllocationService.allocateSeat()` to use atomic operation
5. Add concurrency integration test

## Verification

1. **Unit Tests:** Verify `tryHoldSeat()` returns correct results
2. **Concurrency Test:** Run two parallel threads trying to allocate the same seat
   - Exactly one should succeed
   - The other should get `SeatAlreadyHeldException` (not 500 error)
3. **Existing Tests:** Run `./gradlew test` to ensure no regressions
4. **Manual Test:** Use two browser tabs to simultaneously click the same seat
