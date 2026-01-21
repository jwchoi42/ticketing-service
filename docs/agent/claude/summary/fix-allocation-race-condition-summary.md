  Summary

  Files Modified

  1. AllocationRepository.java (backend/.../adapter/out/persistence/allocation/)
    - Added tryHoldSeat() native query using PostgreSQL INSERT ON CONFLICT for atomic upsert
    - The query only updates the allocation if the seat is AVAILABLE or has an expired HOLD
  2. RecordAllocationPort.java (backend/.../application/port/out/persistence/allocation/)
    - Added tryHoldSeat(Long userId, Long matchId, Long seatId, LocalDateTime holdExpiresAt) method
  3. AllocationPersistenceAdapter.java (backend/.../adapter/out/persistence/allocation/)
    - Implemented tryHoldSeat() that performs the atomic upsert and then verifies the current user
  acquired the hold
  4. AllocationService.java (backend/.../application/service/)
    - Refactored allocateSeat() to:
        - First check for existing allocations with lock (handles updates)
      - If no existing allocation, use atomic tryHoldSeat() to prevent race conditions
      - Throws SeatAllocationConflictException when another user wins the race

  Files Created

  1. SeatAllocationConflictException.java (backend/.../application/service/exception/)
    - New exception for when a race condition is detected
  2. SeatAllocationConcurrencyTest.java (backend/src/test/.../integration/)
    - Integration tests verifying concurrent seat allocation works correctly
    - Tests both 2-thread and 10-thread scenarios

  How the Fix Works

  The race condition occurred when no allocation record existed - multiple threads could get
  Optional.empty() and all try to insert. The fix uses PostgreSQL's atomic INSERT ON CONFLICT DO
  UPDATE to ensure only one user can acquire a seat, even with concurrent requests. The CASE
  expressions ensure the update only happens if the seat is truly available.