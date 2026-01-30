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
- **기각 이유**: 쿼리 자체가 느리면 첫 번째 요청도 느림

### 대안 3: Materialized View
- DB 레벨에서 비정규화된 뷰 관리
- **기각 이유**: 애플리케이션 코드 복잡성, DB 종속성

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
