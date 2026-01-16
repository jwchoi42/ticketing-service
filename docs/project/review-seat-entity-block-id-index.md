# SeatEntity blockId 인덱스 비판적 검토

## 현재 상태

`SeatEntity`의 `blockId`에는 **인덱스가 없습니다**.

```java
// SeatEntity.java:23
private Long blockId;  // 인덱스 없음, FK 제약조건도 없음
```

## 문제점 분석

### 1. 성능 문제 - Full Table Scan 발생 가능

`SeatRepository`에서 blockId를 사용하는 쿼리가 2개 있습니다:

```java
// SeatRepository.java:10-12
List<SeatEntity> findByBlockId(Long blockId);
Optional<SeatEntity> findByBlockIdAndRowNumberAndSeatNumber(Long blockId, Integer rowNumber, Integer seatNumber);
```

이 쿼리들이 호출되는 곳:
- `SitePersistenceAdapter.loadSeatsByBlockId()` - 좌석 목록 조회
- `AllocationPersistenceAdapter.loadAllocationStatusesByBlockId()` - **SSE 스트리밍 초기 데이터**
- `AllocationPersistenceAdapter.loadAllocationStatusesSince()` - **SSE 폴링 (주기적 호출)**

**경기장 좌석 수가 수만~수십만 row일 경우, 인덱스 없이는 매번 Full Table Scan이 발생합니다.**

### 2. 데이터 무결성 문제

현재 `@ManyToOne` 관계 없이 단순 `Long`으로 저장하고 있어서:
- FK 제약조건이 없음
- 존재하지 않는 `blockId`를 가진 좌석 생성 가능
- 데이터 정합성 보장 안 됨

### 3. 복합 인덱스 미고려

`findByBlockIdAndRowNumberAndSeatNumber()` 쿼리를 위해서는 `blockId` 단일 인덱스보다 `(blockId, rowNumber, seatNumber)` 복합 인덱스가 더 효율적입니다.

## 권장 사항

**옵션 A: 단일 인덱스 (최소한)**
```java
@Table(name = "seats", indexes = {
    @Index(name = "idx_seats_block_id", columnList = "blockId")
})
```

**옵션 B: 복합 인덱스 + 유니크 제약 (권장)**
```java
@Table(name = "seats", indexes = {
    @Index(name = "idx_seats_block_id", columnList = "blockId"),
    @Index(name = "idx_seats_block_row_seat", columnList = "blockId, rowNumber, seatNumber", unique = true)
})
```

복합 유니크 인덱스는:
- 두 쿼리 모두 커버
- 같은 블록 내 동일 좌석 중복 방지
- 데이터 무결성 향상

## 트레이드오프

| 항목 | 인덱스 없음 | 인덱스 있음 |
|------|------------|-------------|
| SELECT 성능 | O(n) Full Scan | O(log n) Index Seek |
| INSERT 성능 | 빠름 | 약간 느림 |
| 저장 공간 | 적음 | 인덱스 크기만큼 증가 |

좌석 데이터는 **한 번 생성 후 거의 변경되지 않고, 조회가 빈번**하므로 인덱스 추가가 합리적입니다. 특히 SSE 스트리밍에서 주기적으로 `findByBlockId()`를 호출하고 있어 인덱스 없이는 성능 병목이 될 가능성이 높습니다.
