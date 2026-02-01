# 좌석 현황 조회 쿼리 최적화 과정

## 개요

좌석 현황 조회 API의 성능을 개선하기 위해 진행한 최적화 작업들을 기록합니다.

- **최적화 대상**: `AllocationStatusService.getAllocationStatusSnapShotByMatchIdAndBlockId()`
- **주요 변경**: JOIN 제거, 비정규화, 인덱스 추가

---

## 1. 아키텍처 변경: JOIN → 분리 조회

### 이전 방식 (JOIN)

프론트엔드에서 좌석 정보(행/열)와 배정 현황을 함께 필요로 해서, 백엔드에서 JOIN하여 제공했습니다.

```sql
SELECT a.id, a.match_id, s.block_id, a.seat_id, a.status,
       a.hold_expires_at, a.updated_at, s.row_num, s.col_num
FROM allocations a
JOIN seats s ON a.seat_id = s.id
WHERE a.match_id = ? AND s.block_id = ?
```

**문제점:**
- `block_id`가 `seats` 테이블에만 존재
- `allocations` 테이블에 `(match_id, block_id)` 인덱스 사용 불가
- JOIN 연산 비용 발생
- 정적 데이터(좌석)와 동적 데이터(배정)가 혼합되어 캐싱 어려움

### 현재 방식 (분리 조회)

프론트엔드에서 좌석 정보와 배정 현황을 별도로 조회하고 클라이언트에서 매핑합니다.

```sql
-- 1. 좌석 정보 (정적 데이터 - 캐싱 가능)
SELECT * FROM seats WHERE block_id = ?

-- 2. 배정 현황 (동적 데이터)
SELECT id, match_id, block_id, seat_id, status, hold_expires_at, updated_at
FROM allocations
WHERE match_id = ? AND block_id = ?
```

**장점:**
- 좌석 정보는 정적 → 브라우저/CDN/Redis 장기 캐싱 가능
- 배정 현황만 반복 조회 → JOIN 비용 제거
- 각 쿼리가 단순해져 인덱스 효율 극대화

### 속도 비교

| 방식 | 첫 요청 | 반복 요청 |
|------|--------|----------|
| **JOIN** | ~20-50ms | ~20-50ms (매번 JOIN) |
| **분리** | ~15ms (좌석) + ~5ms (배정) | **~5ms** (좌석 캐시 히트) |

---

## 2. 비정규화: blockId 추가

### 변경 내용 (2026-01-28)

`AllocationEntity`에 `blockId` 컬럼을 추가하여 비정규화했습니다.

**커밋**: `1fe8a40` (2026-01-28 08:50:07)

```java
// AllocationEntity.java
@Table(name = "allocations", indexes = {
    @Index(name = "idx_match_seat_unique", columnList = "matchId, seatId", unique = true),
    @Index(name = "idx_match_block", columnList = "matchId, blockId"),  // 추가
    @Index(name = "idx_allocations_updated_at", columnList = "updatedAt")
})
public class AllocationEntity {
    // ...
    private Long blockId;  // 비정규화 추가
    // ...
}
```

### 효과

| 항목 | 이전 (JOIN) | 이후 (비정규화) |
|------|------------|----------------|
| **실행 계획** | Nested Loop JOIN | Index Scan |
| **인덱스 활용** | 부분적 | 완전 |
| **I/O** | 2개 테이블 | 1개 테이블 |

### 데이터 규모별 예상 속도

| 배정 데이터 수 | JOIN 방식 | 비정규화 방식 | 개선율 |
|---------------|----------|--------------|--------|
| 1,000건 | ~10-20ms | ~2-5ms | **2-4배** |
| 10,000건 | ~50-100ms | ~5-10ms | **5-10배** |
| 100,000건 | ~200-500ms | ~10-20ms | **10-25배** |

---

## 3. 인덱스 최적화

### 인덱스 변화 이력

| 시점 | 커밋 | 인덱스 |
|------|------|--------|
| 초기 | `78a43bd` | `idx_match_seat_unique`, `idx_allocations_updated_at` |
| 비정규화 | `1fe8a40` (2026-01-28) | + `idx_match_block` |
| 현재 | `8bcc607` (2026-01-30) | + `idx_match_block_updated`, `idx_reservation_id` |

### 현재 인덱스 구성

```java
@Table(name = "allocations", indexes = {
    @Index(name = "idx_match_seat_unique", columnList = "matchId, seatId", unique = true),
    @Index(name = "idx_match_block", columnList = "matchId, blockId"),
    @Index(name = "idx_match_block_updated", columnList = "matchId, blockId, updatedAt"),
    @Index(name = "idx_reservation_id", columnList = "reservationId")
})
```

### 쿼리별 인덱스 활용

| 쿼리 | 사용 인덱스 | 성능 |
|------|------------|------|
| `WHERE matchId = ? AND blockId = ?` | `idx_match_block` | Index Scan |
| `WHERE matchId = ? AND blockId = ? AND updatedAt > ?` | `idx_match_block_updated` | Covering Index |
| `WHERE reservationId = ?` | `idx_reservation_id` | Index Scan |

---

## 4. 현재 쿼리 구현

### AllocationRepositoryQueryImpl.java

```java
@Override
public List<AllocationStatus> findAllocationStatusesByMatchIdAndBlockId(Long matchId, Long blockId) {
    return queryFactory
            .select(Projections.constructor(AllocationStatus.class,
                    allocationEntity.id,
                    allocationEntity.matchId,
                    allocationEntity.blockId,
                    allocationEntity.seatId,
                    allocationEntity.status,
                    allocationEntity.holdExpiresAt,
                    allocationEntity.updatedAt
            ))
            .from(allocationEntity)
            .where(
                    allocationEntity.matchId.eq(matchId),
                    allocationEntity.blockId.eq(blockId)
            )
            .fetch();
}

@Override
public List<AllocationStatus> findAllocationStatusesByBlockIdAndUpdatedAtAfter(
        Long matchId, Long blockId, LocalDateTime since) {
    return queryFactory
            .select(Projections.constructor(AllocationStatus.class,
                    allocationEntity.id,
                    allocationEntity.matchId,
                    allocationEntity.blockId,
                    allocationEntity.seatId,
                    allocationEntity.status,
                    allocationEntity.holdExpiresAt,
                    allocationEntity.updatedAt
            ))
            .from(allocationEntity)
            .where(
                    allocationEntity.matchId.eq(matchId),
                    allocationEntity.blockId.eq(blockId),
                    allocationEntity.updatedAt.after(since)
            )
            .fetch();
}
```

---

## 5. Request Collapsing과의 관계

### Request Collapsing이 효과적이지 않은 이유

위의 최적화로 DB 쿼리가 이미 매우 빠릅니다 (~5-10ms).

Request Collapsing은 **쿼리 실행 중 요청이 쌓여야** 효과가 있습니다:

```
DB 쿼리 시간: 100ms (느림)
─────────────────────────────────────────────────→
│<──────── 100ms 쿼리 실행 중 ────────>│
│                                        │
요청 A (리더) ─┐                         │
요청 B ───────┼── 모두 같은 결과 공유 ──┤ → DB 쿼리 1번
요청 C ───────┘                         │
```

```
DB 쿼리 시간: 5ms (빠름)
─────────────────────────────────────────────────→
│<─5ms─>│
│       │
요청 A ──┘     요청 B ──┘     요청 C ──┘
         ↓            ↓            ↓
      쿼리 1번     쿼리 1번     쿼리 1번  → 병합 효과 없음
```

### 부하 테스트 결과

모든 테스트는 **1000 VU, 2분** 동안 실행되었습니다.

#### 로컬 테스트 비교 (동일 환경에서 쿼리 방식만 다름)

##### 테스트 A: JOIN 방식 - 느린 쿼리

```sql
-- JOIN 방식 (blockId 인덱스 없음)
SELECT a.*, s.block_id FROM allocations a
JOIN seats s ON a.seat_id = s.id
WHERE a.match_id = ? AND s.block_id = ?
```

| 전략 | 처리량 | 평균 | p(95) | p(99) | 실패율 |
|------|--------|------|-------|-------|--------|
| none (기준선) | 742.5 req/s | 10.24ms | 21.07ms | 44.92ms | 0.00% |
| **collapsing** | 744.1 req/s | **6.02ms** | **9.77ms** | **12.82ms** | 0.00% |

**Collapsing 개선율**: 평균 **41%**, p(95) **54%**, p(99) **71%**

##### 테스트 B: 비정규화 방식 - 빠른 쿼리

```sql
-- 비정규화 방식 (blockId 인덱스 있음)
SELECT * FROM allocations
WHERE match_id = ? AND block_id = ?
```

| 전략 | 처리량 | 평균 | p(95) | p(99) | 실패율 |
|------|--------|------|-------|-------|--------|
| none (기준선) | 743.1 req/s | 9.23ms | 17.13ms | 29.64ms | 0.00% |
| **collapsing** | 744.9 req/s | **5.57ms** | **9.18ms** | **13.53ms** | 0.00% |

**Collapsing 개선율**: 평균 **40%**, p(95) **46%**, p(99) **54%**

##### 로컬 테스트 종합 비교

| 쿼리 방식 | 전략 | 평균 | p(95) | p(99) |
|----------|------|------|-------|-------|
| JOIN (느림) | none | 10.24ms | 21.07ms | 44.92ms |
| JOIN (느림) | collapsing | 6.02ms | 9.77ms | 12.82ms |
| 비정규화 (빠름) | none | 9.23ms | 17.13ms | 29.64ms |
| 비정규화 (빠름) | collapsing | **5.57ms** | **9.18ms** | **13.53ms** |

**핵심 발견:**
- 비정규화만으로 p(99) **34% 개선** (44.92ms → 29.64ms)
- Collapsing 추가 시 p(99) **54% 추가 개선** (29.64ms → 13.53ms)
- 최종 결과: JOIN 대비 p(99) **70% 개선** (44.92ms → 13.53ms)

---

#### 원격 서버 테스트 (참고용)

| 전략 | 처리량 | p(95) | p(99) | 실패율 |
|------|--------|-------|-------|--------|
| none (기준선) | 614.8 req/s | 803ms | 1.23s | 0.00% |
| collapsing | 520.6 req/s | 912ms | 9.69s | 0.56% |

**참고**: 원격 서버에서는 네트워크 지연, 서버 스펙 등 다른 요인이 영향을 미침

---

---

#### 원격 서버 테스트 (네트워크 지연 환경)

| 전략 | 처리량 | 평균 | p(95) | p(99) | 실패율 |
|------|--------|------|-------|-------|--------|
| none | 694.2 req/s | 79.99ms | 196.39ms | 398.68ms | 0.00% |
| collapsing | 694.5 req/s | 77.97ms | 218.44ms | 399.84ms | 0.00% |

**결과**: 거의 동일 (collapsing이 p(95)에서 오히려 11% 느림)

---

### 로컬 vs 원격 환경 비교

| 환경 | 전략 | 평균 | 네트워크 지연 | Collapsing 효과 |
|------|------|------|--------------|----------------|
| **로컬** | none | 9.23ms | 0ms | - |
| **로컬** | collapsing | 5.57ms | 0ms | **40% 개선** |
| **원격** | none | 79.99ms | ~70ms | - |
| **원격** | collapsing | 77.97ms | ~70ms | **효과 없음** |

---

### 원격에서 Collapsing이 효과 없는 이유

```
로컬:
요청 ─────→ 서버 (쿼리 ~9ms) ─────→ 응답
      0ms           9ms              0ms
총 응답: ~9ms

원격:
요청 ─────→ 네트워크 ─────→ 서버 (쿼리 ~9ms) ─────→ 네트워크 ─────→ 응답
      0ms        ~35ms              9ms              ~35ms          0ms
총 응답: ~79ms
```

1. **네트워크 지연이 지배적**: 실제 쿼리 시간(~9ms)보다 네트워크 왕복(~70ms)이 훨씬 큼
2. **병합 윈도우 불일치**: 쿼리가 ~9ms 동안 실행될 때만 요청이 병합됨
3. **요청 도착 분산**: 네트워크 지연 때문에 요청들이 같은 시점에 서버에 도착하지 않음

---

---

#### Burst 테스트 (sleep 없음, 원격 서버)

일반 부하 테스트에서는 `sleep(1)`로 요청 간격을 두었습니다. Burst 테스트에서는 sleep 없이 최대한 빠르게 요청을 전송하여 동시 요청 확률을 높였습니다.

**테스트 조건**: 500 VU, 40초 (ramp-up 10초 → 유지 20초 → ramp-down 10초)

| 전략 | 처리량 | 평균 | p(95) | p(99) | 실패율 |
|------|--------|------|-------|-------|--------|
| none | 799 req/s | 419.92ms | 1.1s | 2.37s | 0.00% |
| collapsing | 706 req/s | 417.75ms | 1.16s | 2.49s | 0.00% |

**결과 분석**:
- 평균 응답 시간: 거의 동일 (-0.5%)
- p(95): collapsing이 오히려 5.5% 느림
- p(99): collapsing이 5.1% 느림
- 처리량: collapsing이 12% 낮음

**결론**: sleep을 제거하여 요청 빈도를 높여도 원격 환경에서는 collapsing 효과가 없음. 오히려 collapsing 오버헤드로 약간의 성능 저하 발생.

---

#### JOIN 방식 Burst 테스트 (느린 쿼리, 원격 서버)

비정규화된 빠른 쿼리(~9ms)에서는 collapsing 효과가 없었습니다. **쿼리가 느리면 어떨까?** JOIN 방식으로 변경하여 테스트했습니다.

**테스트 조건**: 500 VU, 40초 (ramp-up 10초 → 유지 20초 → ramp-down 10초), JOIN 쿼리

```sql
-- JOIN 방식 (느린 쿼리)
SELECT a.*, s.block_id FROM allocations a
JOIN seats s ON a.seat_id = s.id
WHERE a.match_id = ? AND s.block_id = ?
```

| 전략 | 처리량 | 평균 | p(95) | p(99) | 실패율 |
|------|--------|------|-------|-------|--------|
| none | 193 req/s | **2000ms** | **4.49s** | **4.84s** | 0.00% |
| collapsing | 708 req/s | **429ms** | **1.08s** | **2.21s** | 0.00% |

**개선율**:

| 지표 | 개선율 |
|------|--------|
| 평균 | **79% 개선** (2000ms → 429ms) |
| p(95) | **76% 개선** (4.49s → 1.08s) |
| p(99) | **54% 개선** (4.84s → 2.21s) |
| 처리량 | **3.7배 증가** (193 → 708 req/s) |

**핵심 발견**: 쿼리가 느리면 원격 환경에서도 Request Collapsing이 **매우 효과적**!

```
비정규화 (빠른 쿼리 ~9ms):  collapsing 효과 없음
JOIN (느린 쿼리 ~2s):       collapsing 79% 개선
```

쿼리 실행 시간이 길어지면 그 동안 동시 요청이 쌓여서 병합 효과가 발생합니다.

---

### Request Collapsing 효과 요약

| 환경 | 쿼리 속도 | Request Collapsing 효과 |
|------|-----------|------------------------|
| 로컬 (네트워크 지연 없음) | 빠름 (~5ms) | ✅ **효과적** (40% 개선) |
| 로컬 (네트워크 지연 없음) | 느림 (~10ms) | ✅ **매우 효과적** (54% 개선) |
| 원격 (네트워크 지연 있음) | 빠름 (비정규화) | ❌ **효과 없음** |
| 원격 (네트워크 지연 있음) | 느림 (JOIN) | ✅ **매우 효과적** (79% 개선) |

**핵심 결론**:
- Request Collapsing 효과는 **쿼리 속도**에 달림
- **빠른 쿼리** (~9ms): 병합 윈도우가 짧아 효과 미미
- **느린 쿼리** (~2s): 병합 윈도우가 길어 효과 극대화
- 실제 프로덕션에서 쿼리가 빠르면 **캐시(Redis/Caffeine)가 더 효과적**

---

## 6. 최적화 전략 선택 가이드

| 상황 | 권장 전략 |
|------|----------|
| DB 쿼리 빠름 (~5-10ms) | 캐시 (Redis/Caffeine) 또는 없음 |
| DB 쿼리 느림 (~100ms+) | Request Collapsing |
| 정적 데이터 | 장기 캐싱 (브라우저/CDN) |
| 동적 데이터, 읽기 많음 | 짧은 TTL 캐시 |

---

## 7. 설계 시점의 비정규화 결정

### 두 가지 접근법

| 접근법 | 흐름 | 장점 | 단점 |
|--------|------|------|------|
| 정규화 먼저 | 설계 → 정규화 → 구현 → 성능 테스트 → 비정규화 | YAGNI, 단순한 초기 설계 | 마이그레이션 비용, 스키마 변경 위험 |
| 처음부터 비정규화 | 접근 패턴 분석 → 비정규화 결정 → 구현 | 마이그레이션 없음, 처음부터 최적 성능 | 예측 틀리면 불필요한 복잡성 |

### 판단 기준: 예측 가능성

| 상황 | 권장 접근법 |
|------|------------|
| 접근 패턴이 **불확실**함 | 정규화 먼저 |
| 접근 패턴이 **명확**함 | 처음부터 비정규화 |

### blockId 비정규화: 처음부터 해야 하는 케이스

이 프로젝트에서 `blockId` 비정규화는 **처음부터 설계해야 하는 케이스**입니다.

**이유:**

1. **접근 패턴이 명확함**
   - 티켓팅 시스템의 핵심 쿼리: "이 경기의 이 구역 좌석 현황"
   - `WHERE matchId = ? AND blockId = ?`가 주요 쿼리인 건 도메인 지식

2. **성능 요구사항이 명확함**
   - 티켓 오픈 시 수천 명 동시 접속
   - 실시간 좌석 현황 조회가 핫패스

3. **비정규화 비용이 낮음**
   - `blockId` 하나만 추가
   - 좌석의 `blockId`는 **절대 변하지 않음** → 정합성 위험 없음

4. **나중에 변경 비용이 높음**
   - 프로덕션 데이터 마이그레이션
   - 인덱스 재생성
   - 다운타임 또는 복잡한 무중단 마이그레이션

### 처음부터 비정규화 결정 체크리스트

```
□ 쿼리 패턴이 명확한가?
□ 해당 쿼리가 핫패스인가? (자주 호출되는가?)
□ 비정규화할 데이터가 불변인가? (변경 시 정합성 문제 없는가?)
□ 도메인 지식으로 예측 가능한가? (추측이 아닌가?)
```

- 4개 모두 ✅ → **처음부터 비정규화**
- 하나라도 불확실 → **정규화 먼저, 나중에 결정**

### "Premature Optimization" 오해

> "Premature optimization is the root of all evil" - Donald Knuth

이 말은 **추측에 기반한 최적화**를 경계하는 것이지, **도메인 지식에 기반한 설계**를 하지 말라는 뜻이 아닙니다.

| 구분 | 예시 | 판단 |
|------|------|------|
| ❌ 추측 기반 | "나중에 빠를 것 같아서" | Premature Optimization |
| ✅ 도메인 지식 기반 | "티켓팅에서 matchId+blockId 조회는 핵심" | 합리적인 설계 결정 |

---

## 결론

1. **JOIN 제거** + **비정규화** (`blockId` 추가)로 쿼리 단순화
2. **복합 인덱스** 추가로 Index Scan 보장
3. **정적/동적 데이터 분리**로 캐싱 최적화
4. 쿼리 속도 **5-10배 개선** (~50ms → ~5ms)
5. **Request Collapsing**은 쿼리가 느릴 때만 효과적 (빠른 쿼리에서는 오버헤드만 발생)
6. **비정규화 시점**: 접근 패턴이 명확하고 도메인 지식으로 예측 가능하면 처음부터 설계
