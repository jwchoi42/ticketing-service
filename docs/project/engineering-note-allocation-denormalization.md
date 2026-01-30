# Engineering Note: Allocation 테이블 비정규화 전략

> **배경**: 좌석 현황 조회 성능 최적화를 위해 allocation 테이블에 matchId, blockId, seatId를 비정규화하여 저장

---

## 1. 문제 상황

기존 구조에서 좌석 현황 조회 시 JOIN 필요:

```sql
-- 이전: seat 테이블과 JOIN 필수
SELECT a.*, s.block_id
FROM allocation a
JOIN seat s ON a.seat_id = s.id
WHERE s.match_id = ? AND s.block_id = ?
```

**문제점:**
- 고부하 상황(1000 VU)에서 JOIN 비용 증가
- Request Collapsing 등 서비스 레벨 최적화 필요

---

## 2. 해결: 비정규화

```sql
-- 현재: allocation 단독 조회
SELECT * FROM allocation
WHERE match_id = ? AND block_id = ?
```

**변경 사항:**
- `allocation` 테이블에 `match_id`, `block_id`, `seat_id` 직접 저장
- seat 테이블 JOIN 제거

---

## 3. DB 쿼리 성능 측정 (EXPLAIN ANALYZE)

### 3.1 비정규화 쿼리 (단일 테이블)

```sql
EXPLAIN ANALYZE
SELECT * FROM allocations WHERE match_id = 1 AND block_id = 1;
```

```
Bitmap Heap Scan on allocations  (cost=5.31..122.25 rows=100 width=74)
  ->  Bitmap Index Scan on idx_match_block_updated  (cost=0.00..5.29 rows=100 width=0)
        Index Cond: ((match_id = 1) AND (block_id = 1))

Execution Time: 0.14 ~ 0.39 ms (평균 0.19 ms)
```

### 3.2 정규화 쿼리 (JOIN)

```sql
EXPLAIN ANALYZE
SELECT a.* FROM allocations a
JOIN seats s ON a.seat_id = s.id
WHERE a.match_id = 1 AND s.block_id = 1;
```

```
Hash Join  (cost=11.29..276.55 rows=100 width=74)
  Hash Cond: (a.seat_id = s.id)
  ->  Seq Scan on allocations a  (cost=0.00..239.00 rows=10000 width=74)  ← 전체 스캔!
        Filter: (match_id = 1)
  ->  Hash
        ->  Index Scan using idx_seats_block_id on seats s

Execution Time: 1.45 ~ 1.74 ms (평균 1.59 ms)
```

### 3.3 결과 비교

| 쿼리 방식 | 실행 시간 | 비고 |
|-----------|-----------|------|
| **비정규화** (단일 테이블) | **~0.19 ms** | Index Scan |
| **정규화** (JOIN) | ~1.59 ms | Seq Scan 발생 |

**비정규화가 약 8배 빠름** - JOIN 시 allocations 테이블 전체 스캔이 발생하기 때문

---

## 4. 부하 테스트 결과 (2026-01-30)

### 4.1 테스트 환경
- 서버: EC2 (http://3.38.125.37)
- 시나리오: 1000 VU, 2분, ramp-up/down

### 4.2 결과 비교

| 전략 | avg | med | p(95) | p(99) | 처리량 |
|------|-----|-----|-------|-------|--------|
| none (캐시 없음) | 941ms | 321ms | 1.81s | 30.2s | 638/s |
| collapsing | 999ms | 320ms | 2.21s | 30.2s | 615/s |

**핵심 발견:**
- Request Collapsing이 오히려 **6% 느림**
- 쿼리가 이미 충분히 빠름 (med ~320ms는 네트워크 포함)
- 동기화 오버헤드가 이득보다 큼

---

## 5. 기능별 비정규화 영향 분석

### 5.1 쓰기 작업 (Hold/Release/Confirm)

모든 쓰기 작업은 동일한 패턴으로 단일 레코드를 조회:

```java
// AllocationService.java
loadAllocationPort.loadAllocationByMatchAndSeatWithLock(matchId, seatId)
```

```sql
SELECT * FROM allocations
WHERE match_id = ? AND seat_id = ?
FOR UPDATE
```

**비정규화 영향: 없음**
- `matchId + seatId` Unique Index로 단일 레코드 조회
- `blockId`는 쓰기 작업에서 사용하지 않음
- 조회한 비정규화 필드는 그대로 유지하여 저장

### 5.2 읽기 작업 (좌석 현황 조회)

블록 단위로 다수 레코드를 조회:

```java
// AllocationRepositoryQueryImpl.java
.where(
    allocationEntity.matchId.eq(matchId),
    allocationEntity.blockId.eq(blockId)  // ← 비정규화 필드 사용
)
```

**비정규화 영향: 8배 빠름**
- JOIN 없이 Index Scan만 사용
- `idx_match_block_updated` 인덱스 활용

### 5.3 기능별 영향 요약

| 기능 | 쿼리 방식 | 비정규화 효과 |
|------|----------|--------------|
| Hold (점유) | matchId + seatId (단일) | 없음 |
| Release (해제) | matchId + seatId (단일) | 없음 |
| Confirm (확정) | matchId + seatId (단일) | 없음 |
| **현황 조회** | matchId + blockId (블록) | **8배 빠름** |

### 5.4 데이터 일관성 관리

**비정규화 필드 설정 시점:**
```java
// MatchService.prePopulateAllocations() - 경기 생성 시
Allocation.availableForMatch(
    matchId,           // ← 비정규화
    seat.getBlockId(), // ← 비정규화 (seat에서 복사)
    seat.getId()
)
```

**일관성 보장:**
- 경기 생성 시 pre-populate로 한 번만 설정
- 이후 쓰기 작업에서는 기존 값 유지
- Seat 정보 변경 시 수동 동기화 필요 (드문 경우)

---

## 6. 결론

### 6.1 비정규화의 효과

| 항목 | 정규화 (JOIN) | 비정규화 |
|------|---------------|----------|
| 쿼리 복잡도 | JOIN 필요 | **단일 테이블** |
| 쿼리 속도 | ~1.59 ms | **~0.19 ms (8배 빠름)** |
| 서비스 최적화 | Collapsing 필요 | **불필요** |
| 저장 공간 | 효율적 | 약간 증가 |
| 데이터 일관성 | **보장** | 수동 관리 필요 |

### 6.2 권장 사항

1. **비정규화 유지** - 조회 성능이 우선인 read-heavy 워크로드에 적합
2. **Request Collapsing 제거 고려** - 현재 구조에서 효과 없음
3. **캐시는 선택적** - DB 쿼리가 충분히 빠르므로 필요시에만 적용

### 6.3 트레이드오프

**비정규화 선택 이유:**
- 좌석 현황은 read-heavy (조회 >> 수정)
- 조회 성능이 사용자 경험에 직접 영향
- 데이터 불일치 가능성 낮음 (seat 정보는 거의 변경 안됨)

---

*작성일: 2026-01-30*
