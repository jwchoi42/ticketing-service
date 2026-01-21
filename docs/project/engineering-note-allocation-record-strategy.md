# Engineering Note: Allocation 레코드 생성 전략 비교

> **배경**: 좌석 선점 시 race condition 수정 과정에서 두 가지 접근 방식을 비교 분석

---

## 1. 문제 상황

`AllocationService.java:57-58`에서 race condition 발생:

```java
Allocation allocation = loadAllocationPort.loadAllocationByMatchAndSeatWithLock(matchId, seatId)
        .orElse(Allocation.available(seatId));  // ← Race condition
```

**문제 시나리오:**
1. `PESSIMISTIC_WRITE` 락이 행이 없으면 아무것도 잠그지 못함
2. 두 스레드 모두 `Optional.empty()` 수신 → 새 `Allocation.available(seatId)` 생성
3. 둘 다 `recordAllocation()` 호출 → unique constraint violation 발생

---

## 2. 해결 방안 비교

### 2.1 방안 A: On-demand Upsert (현재 구현)

```
좌석 선점 요청 시 → INSERT ON CONFLICT로 레코드 생성/수정
```

**구현 방식:**
```java
// AllocationRepository.java
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
    ...
    """, nativeQuery = true)
int tryHoldSeat(...);
```

**장점:**
- 저장 공간 효율적 (실제 사용된 좌석만 저장)
- 경기 생성 시 추가 작업 없음
- 초기 데이터 없이도 동작

**단점:**
- Upsert 쿼리가 복잡함 (다중 CASE 문)
- 레코드 존재 여부에 따른 분기 로직 필요
- PostgreSQL 종속적 쿼리 (DB 이식성 낮음)
- 테스트/디버깅 복잡도 증가

---

### 2.2 방안 B: Pre-populate with AVAILABLE (권장)

```
경기 생성 시 → 모든 좌석에 대해 AVAILABLE 레코드 bulk insert
좌석 선점 시 → SELECT FOR UPDATE + UPDATE
```

**구현 방식:**
```java
// MatchService.java - 경기 생성 시
@Transactional
public Match createMatch(CreateMatchCommand command) {
    Match match = matchRepository.save(new Match(...));

    // 모든 좌석에 대해 AVAILABLE allocation 생성
    List<Allocation> allocations = seatRepository.findAll().stream()
        .map(seat -> Allocation.available(match.getId(), seat.getId()))
        .toList();

    allocationRepository.saveAll(allocations);  // bulk insert
    return match;
}

// AllocationService.java - 좌석 선점 시
@Transactional
public void allocateSeat(AllocateSeatCommand command) {
    // 레코드가 항상 존재하므로 단순한 락 사용 가능
    Allocation allocation = loadAllocationPort
        .loadAllocationByMatchAndSeatWithLock(matchId, seatId)
        .orElseThrow(() -> new AllocationNotFoundException(matchId, seatId));

    // 상태 검증 및 업데이트
    if (allocation.canBeHeldBy(userId)) {
        allocation.hold(userId, expiresAt);
        recordAllocationPort.recordAllocation(allocation);
    } else {
        throw new SeatAlreadyHeldException(matchId, seatId);
    }
}
```

**장점:**
- 단순한 비즈니스 로직 (레코드 존재 보장)
- 표준 JPA로 처리 가능 (DB 종속성 없음)
- 기존 `PESSIMISTIC_WRITE` 락으로 충분
- 코드 가독성 및 유지보수성 향상
- 기존 조회 로직 단순화 가능

**단점:**
- 저장 공간 증가 (예: 10경기 × 10,000좌석 = 100,000 레코드)
- 경기 생성 시 bulk insert 필요 (일회성 비용)

---

## 3. 상세 비교 분석

### 3.1 정량적 비교

| 기준 | On-demand Upsert | Pre-populate |
|------|------------------|--------------|
| 코드 복잡도 | 높음 (CASE 문, 분기 로직) | **낮음** (단순 UPDATE) |
| DB 이식성 | PostgreSQL 종속 | **DB 무관** |
| 저장 공간 | **효율적** (사용된 좌석만) | 비효율적 (전체 좌석) |
| 동시성 처리 | 복잡한 upsert | **단순한 락** |
| 경기 생성 비용 | **없음** | bulk insert 필요 |
| 쿼리 성능 | INSERT 우선 시도 | **단순 SELECT FOR UPDATE** |
| 테스트 용이성 | 복잡 (upsert 검증) | **단순** (상태 변경만 검증) |

### 3.2 저장 공간 분석

```
가정:
- 좌석 수: 10,000개 / 경기장
- 경기 수: 100경기 / 시즌
- allocation 레코드 크기: ~100 bytes

Pre-populate 방식:
- 총 레코드: 10,000 × 100 = 1,000,000개
- 총 용량: 1,000,000 × 100 bytes ≈ 100 MB

On-demand 방식 (평균 점유율 70% 가정):
- 총 레코드: 10,000 × 100 × 0.7 = 700,000개
- 총 용량: 700,000 × 100 bytes ≈ 70 MB

차이: 30 MB (현대 데이터베이스에서 무시할 수준)
```

### 3.3 기존 코드 영향 분석

**현재 `loadAllocationStatusesByBlockId` 메서드:**
```java
// AllocationPersistenceAdapter.java:58-81
public List<Allocation> loadAllocationStatusesByBlockId(Long matchId, Long blockId) {
    List<SeatEntity> seats = seatRepository.findByBlockId(blockId);
    List<Allocation> existingAllocations = allocationRepository.findByMatchIdAndSeatIdIn(...);

    // 없는 좌석은 AVAILABLE로 채움 ← Pre-populate 시 불필요
    return seatIds.stream()
        .map(seatId -> allocationMap.getOrDefault(seatId, Allocation.available(seatId)))
        .collect(Collectors.toList());
}
```

**Pre-populate 적용 시:**
```java
public List<Allocation> loadAllocationStatusesByBlockId(Long matchId, Long blockId) {
    List<Long> seatIds = seatRepository.findByBlockId(blockId)
        .stream().map(SeatEntity::getId).toList();

    // 모든 좌석에 레코드 존재 보장 → 단순 조회
    return allocationRepository.findByMatchIdAndSeatIdIn(matchId, seatIds)
        .stream().map(AllocationEntity::toDomain).toList();
}
```

---

## 4. 권장 사항

### 4.1 결론: Pre-populate 방식 권장

**이유:**
1. **단순함이 가장 중요** - 레코드 존재가 보장되면 분기 로직 제거 가능
2. **저장 공간 차이 미미** - 수백 MB 수준으로 현대 DB에서 문제 없음
3. **조회 로직 단순화** - 기존 `getOrDefault` 패턴 제거 가능
4. **DB 독립성** - PostgreSQL 외 다른 DB로 전환 시 유리

### 4.2 마이그레이션 계획

```
Phase 1: 스키마 준비
  - allocation 테이블에 NOT NULL 제약 검토
  - bulk insert를 위한 인덱스 최적화

Phase 2: 신규 경기에 적용
  - MatchService.createMatch()에 allocation 초기화 추가
  - 신규 경기부터 Pre-populate 방식 적용

Phase 3: 기존 데이터 마이그레이션
  - 배치 작업으로 기존 경기에 누락된 allocation 레코드 생성
  - 운영 시간 외 실행 권장

Phase 4: 코드 정리
  - On-demand upsert 로직 제거
  - getOrDefault 패턴 제거
  - 관련 테스트 업데이트
```

### 4.3 주의 사항

1. **bulk insert 성능**
   - `saveAll()` 대신 JDBC batch insert 고려
   - `spring.jpa.properties.hibernate.jdbc.batch_size=50` 설정

2. **경기 삭제 시 cascade**
   - allocation 레코드도 함께 삭제되도록 FK 설정 확인

3. **테스트 데이터 설정**
   - 테스트 시 경기 생성 후 allocation 초기화 필요
   - `@BeforeEach`에서 처리하거나 TestFixture 활용

---

## 5. 참고: 현재 구현 (On-demand Upsert)

race condition 수정을 위해 현재 적용된 방식. Pre-populate로 전환 시 제거 예정.

**관련 파일:**
- `AllocationRepository.java` - `tryHoldSeat()` native query
- `RecordAllocationPort.java` - `tryHoldSeat()` 인터페이스
- `AllocationPersistenceAdapter.java` - `tryHoldSeat()` 구현
- `AllocationService.java` - 분기 로직 포함
- `SeatAllocationConflictException.java` - race condition 예외

---

*작성일: 2026-01-21*
