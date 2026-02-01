# 티켓팅 좌석 현황 조회, 동시 요청 500명에서 p99 54% 단축한 과정

> 좌석 현황 API가 동시 접속자 500명 상황에서 p99 4.84s → 2.21s로 **54% 개선**된 과정을 공유합니다.

---

## 1. 배경 및 상황 (Context)

### 비즈니스 요구사항

티켓팅 서비스의 핵심 기능은 **실시간 좌석 현황 조회**입니다. 사용자가 좌석을 선택할 때, 현재 다른 사람이 점유 중인지 즉시 확인할 수 있어야 합니다.

### 문제 상황

티켓팅 오픈 직후 **동시 접속자 500명**이 특정 블록의 좌석 현황을 조회했을 때 (배포 서버 기준):

| 지표 | 측정값 | 의미 |
|------|--------|------|
| p(99) | **4.84초** | 사용자 5%가 5초 가까이 대기 |
| 평균 응답 | **2초** | 체감 속도 매우 느림 |
| 처리량 | **193 req/s** | 서버 자원 대비 낮은 처리량 |

### 원인 분석

기존 쿼리는 **JOIN**으로 동작했습니다:

```sql
SELECT a.*, s.block_id
FROM allocations a
JOIN seats s ON a.seat_id = s.id
WHERE a.match_id = ? AND s.block_id = ?
```

**EXPLAIN ANALYZE** 결과:

```
Hash Join  (cost=11.29..276.55 rows=100 width=74)
  -> Seq Scan on allocations a  ← 전체 스캔!
        Filter: (match_id = 1)
```

`blockId`가 `seats` 테이블에만 존재해서 `allocations` 테이블에 인덱스를 활용할 수 없었고, **전체 스캔**이 발생했습니다.

---

## 2. 고려된 대안들 (Proposed Options)

### Option A: Redis/Caffeine 캐시

```java
@Cacheable(value = "seatStatus", key = "#matchId + ':' + #blockId")
public AllocationStatusSnapShot getSnapshot(Long matchId, Long blockId) { ... }
```

| 장점 | 단점 |
|------|------|
| 구현 간단 | TTL 동안 **stale data** 반환 가능 |
| DB 부하 대폭 감소 | 병목이 DB → Redis로 이동할 뿐 |

**판단**: 좌석 현황은 실시간성이 생명. stale data로 인한 사용자 혼란 우려.

---

### Option B: Request Collapsing (요청 병합)

동시에 들어온 같은 요청을 하나로 합쳐서 DB 쿼리 1번만 실행.

```java
CompletableFuture<Snapshot> future = inFlightSnapshots.computeIfAbsent(key, k ->
    CompletableFuture.supplyAsync(() -> loadFromDb(matchId, blockId))
);
return future.get(5, TimeUnit.SECONDS);
```

| 시도 | 문제 | 결과 |
|------|------|------|
| whenComplete + computeIfAbsent | Recursive update 에러 | 서버 크래시 |
| supplyAsync 기본 실행 | ForkJoinPool 병목 | 14% 실패 |
| CachedThreadPool | 스레드 폭증 | 48% 실패 |
| future.cancel(true) | 공유 Future 취소 | 98% 실패 |

**판단**: 쿼리가 빠르면 병합 윈도우가 짧아 효과 없음. 느린 쿼리에서만 효과적.

---

### Option C: 비정규화 (blockId 추가)

`AllocationEntity`에 `blockId` 컬럼을 추가하여 JOIN 제거.

```java
@Table(name = "allocations", indexes = {
    @Index(name = "idx_match_block", columnList = "matchId, blockId"),
})
public class AllocationEntity {
    private Long blockId;  // 비정규화 추가
}
```

| 장점 | 단점 |
|------|------|
| JOIN 제거, 쿼리 8배 빨라짐 | 저장 공간 약간 증가 |
| 인덱스 완전 활용 가능 | 데이터 일관성 수동 관리 필요 |

**판단**: read-heavy 워크로드에 적합. `blockId`는 사실상 불변.

---

## 3. 결정 (Decision)

**Option C (비정규화) + Option B (Request Collapsing) 조합 선택**

### 선택 근거

1. **접근 패턴이 명확함**: `WHERE matchId = ? AND blockId = ?`가 핵심 쿼리
2. **read-heavy 워크로드**: 좌석 현황은 조회 >> 수정
3. **불변 데이터**: 좌석의 `blockId`는 변경되지 않음 → 정합성 위험 없음
4. **근본적 해결**: 캐시는 병목 이동, Collapsing은 쿼리 속도에 의존

### 변경 후 쿼리

```sql
SELECT * FROM allocations
WHERE match_id = ? AND block_id = ?
```

**EXPLAIN ANALYZE** 결과:

```
Bitmap Heap Scan on allocations
  -> Bitmap Index Scan on idx_match_block_updated
        Index Cond: ((match_id = 1) AND (block_id = 1))

Execution Time: 0.19 ms  (기존 1.59ms → 8배 향상)
```

---

## 4. 결과 및 영향 (Consequences)

### 성능 개선 (배포 서버, 500 VU Burst 테스트)

| 지표 | Before | After | 개선율 |
|------|--------|-------|--------|
| p(99) | 4.84s | **2.21s** | **54% 단축** |
| p(95) | 4.49s | **1.08s** | **76% 단축** |
| 평균 | 2000ms | **429ms** | **79% 단축** |
| 처리량 | 193 req/s | **708 req/s** | **3.7배 증가** |
| DB 쿼리 | 1.59ms | **0.19ms** | **8배 향상** |

### 긍정적 영향

- ✅ 티켓팅 오픈 시 좌석 현황 로딩 속도 대폭 개선
- ✅ 서버 처리량 증가로 인프라 비용 절감
- ✅ 사용자 이탈률 감소 기대

### 부정적 영향 (Trade-off)

- ⚠️ `allocation` 테이블에 `blockId` 컬럼 추가 → 저장 공간 약간 증가
- ⚠️ `seat.blockId` 변경 시 `allocation.blockId`도 수동 업데이트 필요 (발생 빈도 매우 낮음)

### 배운 교훈

1. **캐시가 항상 정답은 아니다** - 실시간성이 중요하면 쿼리 최적화가 더 근본적
2. **비정규화는 죄가 아니다** - 도메인 지식에 기반한 설계는 Premature Optimization이 아님
3. **측정 없이 최적화 없다** - EXPLAIN ANALYZE, k6 부하 테스트로 정량적 비교 필수

---

## 5. 참고 (Reference)

### 관련 코드

- [`AllocationRepositoryQueryImpl.java`](file:///c:/private-lesson/ticketing-service/backend/src/main/java/dev/ticketing/adapter/out/persistence/allocation/AllocationRepositoryQueryImpl.java) - QueryDSL 쿼리 구현
- [`AllocationEntity.java`](file:///c:/private-lesson/ticketing-service/backend/src/main/java/dev/ticketing/adapter/out/persistence/allocation/AllocationEntity.java) - 인덱스 및 비정규화 필드

### 부하 테스트

- [`infra/k6/scripts/allocation-status-load-test.js`](file:///c:/private-lesson/ticketing-service/infra/k6/scripts/allocation-status-load-test.js)

### 관련 문서

- [`allocation-query-optimization.md`](file:///c:/private-lesson/ticketing-service/docs/project/allocation-query-optimization.md) - 상세 최적화 과정
- [`engineering-note-allocation-denormalization.md`](file:///c:/private-lesson/ticketing-service/docs/project/engineering-note-allocation-denormalization.md) - 비정규화 전략
- [`request-collapsing-implementation.md`](file:///c:/private-lesson/ticketing-service/docs/project/request-collapsing-implementation.md) - Request Collapsing 트러블슈팅

### 커밋

- `1fe8a40` - 비정규화 (blockId 추가)
- `8bcc607` - 인덱스 최적화
