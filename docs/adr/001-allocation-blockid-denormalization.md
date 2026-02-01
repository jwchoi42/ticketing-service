# ADR-001: Allocation 테이블에 blockId 비정규화

## 상태

**승인됨** (2026-01-28)

## 컨텍스트

티켓팅 시스템에서 좌석 현황 조회는 가장 빈번하게 호출되는 핵심 API입니다.

### 현재 상황
- 사용자는 특정 경기(`matchId`)의 특정 구역(`blockId`) 좌석 현황을 조회
- `blockId`는 `seats` 테이블에만 존재
- 좌석 현황 조회 시 `allocations`와 `seats` 테이블 JOIN 필요

### 문제점
```sql
-- 기존 쿼리 (JOIN 필요)
SELECT a.*, s.block_id
FROM allocations a
JOIN seats s ON a.seat_id = s.id
WHERE a.match_id = ? AND s.block_id = ?
```

- `allocations` 테이블에 `(match_id, block_id)` 인덱스 사용 불가
- JOIN 연산 비용 발생
- 부하 테스트 결과: 평균 응답 시간 ~10ms, p(99) ~45ms

### 성능 요구사항
- 티켓 오픈 시 수천 명 동시 접속
- 실시간 좌석 현황 조회 (폴링 또는 SSE)
- 목표: p(95) < 50ms

## 결정

**`allocations` 테이블에 `blockId` 컬럼을 추가하여 비정규화한다.**

```java
@Table(name = "allocations", indexes = {
    @Index(name = "idx_match_seat_unique", columnList = "matchId, seatId", unique = true),
    @Index(name = "idx_match_block", columnList = "matchId, blockId"),
    @Index(name = "idx_match_block_updated", columnList = "matchId, blockId, updatedAt"),
    @Index(name = "idx_reservation_id", columnList = "reservationId")
})
public class AllocationEntity {
    private Long matchId;
    private Long blockId;  // 비정규화 추가
    private Long seatId;
    // ...
}
```

### 변경된 쿼리
```sql
-- 비정규화 후 (JOIN 제거)
SELECT * FROM allocations
WHERE match_id = ? AND block_id = ?
```

## 근거

### 1. 접근 패턴이 명확함
- `WHERE matchId = ? AND blockId = ?`는 티켓팅 시스템의 핵심 쿼리
- 도메인 지식으로 예측 가능 (추측이 아님)

### 2. 핫패스 쿼리
- 좌석 현황 조회는 가장 빈번한 API 호출
- 티켓 오픈 시 초당 수백~수천 건 요청

### 3. 데이터 불변성
- 좌석의 `blockId`는 **절대 변하지 않음**
- 정합성 위험 없음 (좌석이 다른 구역으로 이동하지 않음)

#### 티켓팅 시스템에서 변하는 것 vs 안 변하는 것

| 데이터 | 변경 여부 | 설명 |
|--------|----------|------|
| **좌석 점유 상태** | ✅ 자주 변함 | AVAILABLE → HELD → RESERVED |
| **좌석의 blockId** | ❌ 절대 안 변함 | A구역 좌석은 항상 A구역 |

"티켓팅 시스템은 좌석 점유가 계속 변하는데 비정규화해도 되나?"라는 의문이 들 수 있습니다.

- **변하는 것**: 좌석의 점유 상태 (allocation status)
- **안 변하는 것**: 좌석이 어느 구역에 속하는지 (blockId)

비정규화한 `blockId`는 불변 데이터이므로 정합성 문제가 없습니다.

#### 비정규화하면 안 되는 예시

```
allocations 테이블에 seatPrice 추가 (좌석 가격)
```

- 좌석 가격은 **변할 수 있음** (할인, 시즌, 등급 조정 등)
- 비정규화하면 seats.price와 allocations.seatPrice 불일치 위험
- → 이 경우는 비정규화 **하면 안 됨**

```
변하는 데이터 비정규화 → ❌ 정합성 위험
불변 데이터 비정규화  → ✅ 안전 (blockId가 이 케이스)
```

### 4. 비정규화 비용 낮음
- 단일 컬럼 추가
- 저장 공간 증가: 8 bytes × row 수 (무시 가능)

### 5. 나중에 변경 비용 높음
- 프로덕션 마이그레이션 필요
- 대량 데이터 UPDATE (backfill)
- 인덱스 재생성
- 다운타임 또는 복잡한 무중단 마이그레이션

## 대안 검토

### 대안 1: JOIN 유지 + 캐시
- Redis/Caffeine 캐시로 성능 보완
- **기각 이유**: 캐시 미스 시 여전히 느림, 캐시 무효화 복잡성

### 대안 2: JOIN 유지 + Request Collapsing
- 동시 요청 병합으로 DB 부하 감소
- **기각 이유**: 비정규화가 더 단순하고 성능도 동등하거나 우수

#### 실제 부하 테스트 비교 (원격 서버, 500 VU, Burst)

| 방식 | 평균 | p(95) | 처리량 | 구현 복잡도 |
|------|------|-------|--------|------------|
| **비정규화 + none** | 419ms | 1.1s | **799 req/s** | 단순 |
| JOIN + collapsing | 429ms | 1.08s | 708 req/s | 복잡 |

- 비정규화가 처리량 **12% 높음**
- 구현 복잡도는 비정규화가 **훨씬 단순**
- Request Collapsing은 느린 쿼리의 "응급 처치", 비정규화는 "근본 해결"

**결론**: 같은 성능이면 단순한 구조(비정규화)가 이김

### 대안 3: Materialized View
- DB 레벨에서 비정규화된 뷰 관리
- **기각 이유**: 애플리케이션 코드 복잡성, DB 종속성, 실시간 갱신 불가

#### Materialized View 상세 분석

```sql
CREATE MATERIALIZED VIEW allocation_status_view AS
SELECT a.*, s.block_id
FROM allocations a
JOIN seats s ON a.seat_id = s.id;
```

| 항목 | 설명 |
|------|------|
| REFRESH 시점 | JOIN 수행 (비용 발생) |
| 조회 시점 | 이미 조인된 결과 읽음 (빠름) |
| allocation 변경 시 | **자동 갱신 안 됨** |

```sql
-- 수동 REFRESH 필요
REFRESH MATERIALIZED VIEW allocation_status_view;
```

| DB | 자동 갱신 지원 |
|----|---------------|
| PostgreSQL | ❌ 없음 |
| Oracle | ✅ ON COMMIT REFRESH 가능 |

**allocation처럼 자주 변경되는 테이블에는 부적합**.

### 대안 4: 정규화 유지 + seat_id 인덱스 추가

정규화된 상태에서 `(matchId, seatId)` 인덱스를 추가하면?

```sql
-- 블록 단위 조회 시 JOIN 필수
SELECT a.*
FROM allocations a
JOIN seats s ON a.seatId = s.id
WHERE a.matchId = ? AND s.blockId = ?
```

**결론: 비정규화보다 느림**

| 항목 | 비정규화 (block_id) | 정규화 + seat_id 인덱스 |
|------|---------------------|------------------------|
| JOIN | 없음 | 필수 |
| 인덱스 스캔 | 1회 | 2회 (seats + allocation) |
| 쿼리 복잡도 | 단순 | 복잡 |

```
비정규화: allocation (idx_match_block) → 바로 결과
정규화:   seats (blockId) → seatId 목록 → allocation (matchId, seatId) → 결과
```

### 대안 5: 정규화 + DB 캐시 (shared_buffers)

PostgreSQL의 shared_buffers(MySQL의 buffer pool과 유사)로 JOIN 비용을 줄일 수 있을까?

#### shared_buffers가 캐싱하는 것

```
테이블/인덱스의 물리적 8KB 페이지 (디스크 블록)
```

- ✅ seats 테이블 페이지
- ✅ allocation 테이블 페이지
- ✅ 인덱스 페이지
- ❌ **JOIN 결과** ← 캐싱 안 됨

seats 테이블은 정적 데이터라 메모리에 상주하므로 디스크 I/O는 줄어들지만:

| 항목 | 비정규화 | 정규화 + shared_buffers |
|------|----------|------------------------|
| 디스크 I/O | 1 테이블 | 거의 없음 (seats 캐시됨) |
| JOIN CPU 비용 | 없음 | **여전히 존재** |
| 인덱스 스캔 | 1회 | 2회 |

**결론**: 차이가 줄어들지만 (1ms 내외) 여전히 비정규화가 빠름.

### 대안 6: Query Cache

MySQL 8.0에서 Query Cache가 제거된 이유:

```
테이블에 INSERT/UPDATE/DELETE 발생
→ 해당 테이블 관련 모든 캐시 무효화
→ 무효화 시 전역 mutex lock 필요
→ 동시성 병목
```

**쓰기가 조금만 있어도 오히려 성능 저하**. 읽기 전용 워크로드에서만 유효했음.

PostgreSQL에는 Query Cache 자체가 없음.

#### 쿼리 결과 캐싱이 필요하다면

| 방법 | 설명 |
|------|------|
| 애플리케이션 캐시 | Redis, Caffeine 등 (현재 프로젝트에서 사용 중) |
| Materialized View | 실시간 아님 (REFRESH 필요) |

**자주 변경되는 데이터**에서는 비정규화 또는 애플리케이션 캐시가 현실적인 선택.

### 최적화 전략 선택 흐름

JOIN 쿼리가 느릴 때 시니어 엔지니어의 사고 흐름:

```
JOIN + none 성능 부족
    │
    ├─ 1. 인덱스 최적화 (가장 먼저)
    ├─ 2. 쿼리 튜닝
    ├─ 3. 비정규화        ← 근본 해결
    ├─ 4. 캐시 (Redis/Caffeine)
    └─ 5. Request Collapsing ← 응급 처치
```

#### 비정규화 vs Request Collapsing 선택 기준

| 상황 | 선택 |
|------|------|
| 스키마 변경 **가능** + 데이터 **불변** | **비정규화** |
| 스키마 변경 **불가** (레거시, 외부 DB) | Request Collapsing |
| 데이터가 **변함** (정합성 위험) | Request Collapsing 또는 캐시 |
| 외부 API 호출 (내가 최적화 불가) | Request Collapsing |

#### 핵심 원칙

```
"왜 느린가?" → 원인 제거 (비정규화, 인덱스)
"일단 빠르게" → 증상 완화 (Collapsing, 캐시)
```

**근본 원인을 해결할 수 있으면 해결하고, 못하면 우회한다.**

이 케이스에서는:
- 스키마 변경 가능 ✅
- blockId 불변 ✅
- → **비정규화가 정답**

> Request Collapsing을 먼저 떠올린다면, "내가 컨트롤할 수 없는 것"에 익숙해진 환경(레거시, 외부 의존성)에서 일해온 경험 때문일 수 있습니다.

## 결과

### 성능 개선 (부하 테스트, 1000 VU, 2분)

| 지표 | JOIN 방식 | 비정규화 방식 | 개선율 |
|------|----------|--------------|--------|
| 평균 | 10.24ms | 6.02ms | 41% |
| p(95) | 21.07ms | 9.77ms | 54% |
| p(99) | 44.92ms | 12.82ms | 71% |

### 트레이드오프

| 항목 | 영향 |
|------|------|
| 저장 공간 | 미미한 증가 (8 bytes/row) |
| 쓰기 복잡성 | Allocation 생성 시 blockId도 함께 저장 필요 |
| 데이터 정합성 | 위험 없음 (blockId 불변) |

## 관련 문서

- [좌석 현황 조회 쿼리 최적화 과정](../project/allocation-query-optimization.md)
- [Request Collapsing 구현 과정](../project/request-collapsing-implementation.md)

## 원칙: 정규화가 기본, 비정규화는 필요할 때만

### 정규화 유지가 원칙인 이유

| 항목 | 정규화 | 비정규화 |
|------|--------|----------|
| 데이터 정합성 | 단일 소스 | 중복 → 불일치 위험 |
| 스키마 복잡도 | 단순 | 복잡 |
| 쓰기 복잡성 | 한 곳만 수정 | 여러 곳 수정 필요 |
| 유지보수 | 쉬움 | 어려움 |
| 유연성 | 변경 용이 | 변경 시 영향 범위 큼 |

### 비정규화 결정 흐름

```
성능 요구사항 충족?
    ├─ Yes → 정규화 유지 ✓
    └─ No  → 비정규화 검토
                 └─ 정합성 위험 낮은가?
                      ├─ Yes → 비정규화 진행
                      └─ No  → 다른 최적화 방법 검토 (캐시, 인덱스 등)
```

**비정규화는 성능을 위해 정합성/단순성을 희생하는 트레이드오프입니다.**

### 이 케이스가 비정규화에 적합했던 이유

| 조건 | 일반적인 경우 | blockId 케이스 |
|------|-------------|----------------|
| 성능 요구사항 | 충족 가능 | **충족 불가** (수천 명 동시 접속) |
| 정합성 위험 | 있음 | **없음** (불변 데이터) |
| 쿼리 빈도 | 보통 | **매우 높음** (핫패스) |

세 조건이 모두 맞았기 때문에 비정규화가 합리적인 결정이었습니다.

> **"비정규화는 최적화다. 최적화는 필요할 때만 한다."**

---

## 비정규화 결정 체크리스트 (일반화)

향후 유사한 결정 시 참고:

### Step 1: 비정규화가 필요한가?

```
□ 현재 성능이 요구사항을 충족하지 못하는가?
□ 인덱스, 캐시 등 다른 최적화로 해결이 어려운가?
```

- 하나라도 ❌ → **정규화 유지**

### Step 2: 비정규화해도 안전한가?

```
□ 쿼리 패턴이 명확한가?
□ 해당 쿼리가 핫패스인가?
□ 비정규화할 데이터가 불변인가?
□ 도메인 지식으로 예측 가능한가? (추측 아닌가?)
```

- 4개 모두 ✅ → **비정규화 진행**
- 하나라도 ❌ → **정규화 유지, 다른 방법 검토**
