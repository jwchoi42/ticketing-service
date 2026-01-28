# AllocationStatusService 성능 개선 분석

## 현재 문제점

### 증상
- CPU 사용량 급증
- DBCP pending 상태 다수 발생
- 동시 접속자 증가 시 응답 지연

### 원인 분석

#### 1. 폴링 기반 SSE 브로드캐스트 (`SseAllocationStatusBroadcaster`)
```java
@Scheduled(fixedRate = 1000)  // 1초마다 실행
public void pollAndBroadcast() {
    emitters.forEach((key, emitterList) -> {
        // 구독된 블록마다 DB 조회
        changesUseCase.getAllocationChangesSince(matchId, blockId, lastCheckTime);
    });
}
```
- **N개 블록 구독 시 → 초당 N개 쿼리 발생**
- 변경이 없어도 매초 조회

#### 2. SSE 구독 시 스냅샷 조회
```java
public SseEmitter subscribe(Long matchId, Long blockId) {
    // 클라이언트 접속마다 전체 블록 좌석 상태 조회
    AllocationStatusSnapShot snapshot = snapshotUseCase.getAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId);
}
```
- 동시 접속자 급증 시 DB 부하 폭발

#### 3. 인덱스 구조
```java
@Index(name = "idx_allocations_updated_at", columnList = "updatedAt")  // 단일 컬럼
```
- `loadAllocationStatusesSince(matchId, blockId, since)` 쿼리에 최적화되지 않음

---

## 해결책 비교

| 방안 | 효과 | 복잡도 | 권장 |
|------|------|--------|------|
| **1. Event-Driven (Push)** | 쿼리 횟수 대폭 감소 | 중 | ⭐ 최우선 |
| 2. 캐시 (Spring Cache) | 반복 조회 감소 | 낮음 | 임시 방편 |
| 3. 복합 인덱스 추가 | 쿼리 성능 향상 | 낮음 | 병행 적용 |
| 4. Redis Pub/Sub | 스케일아웃 대비 | 높음 | 향후 확장 시 |

---

## 1. Event-Driven 방식 상세

### 핵심 아이디어
> **폴링(Pull)** → **이벤트 발행(Push)** 전환

변경이 발생할 때만 알림을 보내므로, 불필요한 DB 조회 제거.

### 현재 흐름 (Polling)

```
[클라이언트] ──SSE 구독──▶ [SseAllocationStatusBroadcaster]
                                    │
                                    ▼
                          ┌─────────────────────┐
                          │  @Scheduled(1초)    │
                          │  pollAndBroadcast() │
                          └─────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              [DB 조회]       [DB 조회]       [DB 조회]
              Block 1         Block 2         Block N
                    │               │               │
                    ▼               ▼               ▼
              변경 있음?       변경 없음        변경 있음?
                    │                               │
                    └───────────────┬───────────────┘
                                    ▼
                          [SSE 이벤트 전송]
```

**문제**: 변경 여부와 관계없이 **초당 N개 쿼리** 발생

### 개선 흐름 (Event-Driven)

```
[좌석 배정/해제]
       │
       ▼
┌─────────────────────────┐
│ AllocationService       │
│ - hold(), confirm(),    │
│   release()             │
└─────────────────────────┘
       │
       │ ApplicationEvent 발행
       ▼
┌─────────────────────────┐
│ AllocationChangedEvent  │
│ - matchId, blockId      │
│ - AllocationStatus      │
└─────────────────────────┘
       │
       │ @TransactionalEventListener
       ▼
┌─────────────────────────┐
│ SseAllocationStatus     │
│ Broadcaster             │
│ - broadcast(event)      │◀── DB 조회 없음!
└─────────────────────────┘
       │
       ▼
[해당 블록 구독자에게만 SSE 전송]
```

**효과**: 변경 시에만 이벤트 발생, **DB 조회 0회**

---

## Load Test 흐름 변화

### 테스트 대상 API
```
GET /api/matches/{matchId}/blocks/{blockId}/seats
```

### 현재 (Polling 방식)

```
┌──────────────────────────────────────────────────────────────────┐
│  k6 Load Test (1000 VU, 1초 간격)                                │
│                                                                  │
│  [VU 1] ──GET /seats──▶ [Controller] ──▶ [DB 조회] ──▶ 응답     │
│  [VU 2] ──GET /seats──▶ [Controller] ──▶ [DB 조회] ──▶ 응답     │
│  ...                                                             │
│  [VU 1000] ──GET /seats──▶ [Controller] ──▶ [DB 조회] ──▶ 응답  │
│                                                                  │
│  + @Scheduled pollAndBroadcast() 1초마다 추가 쿼리               │
└──────────────────────────────────────────────────────────────────┘

DB 부하:
- REST API: 1000 VU × 1 req/sec = 1000 쿼리/초
- SSE 폴링: 구독된 블록 수 × 1 쿼리/초 (추가)
- 합계: 매우 높음
```

### 개선 후 (Event-Driven + 캐시)

```
┌──────────────────────────────────────────────────────────────────┐
│  k6 Load Test (1000 VU, 1초 간격)                                │
│                                                                  │
│  [VU 1] ──GET /seats──▶ [Controller] ──▶ [캐시 HIT] ──▶ 응답    │
│  [VU 2] ──GET /seats──▶ [Controller] ──▶ [캐시 HIT] ──▶ 응답    │
│  ...                                                             │
│  [VU 1000] ──GET /seats──▶ [Controller] ──▶ [캐시 HIT] ──▶ 응답 │
│                                                                  │
│  + @Scheduled pollAndBroadcast() 제거됨 (이벤트 기반)            │
└──────────────────────────────────────────────────────────────────┘

DB 부하:
- REST API: 캐시 미스 시에만 조회 (TTL 기반)
- SSE: 변경 시에만 이벤트 발행 (DB 조회 없음)
- 합계: 대폭 감소
```

### 예상 개선 효과

| 지표 | 현재 | 개선 후 | 비고 |
|------|------|---------|------|
| DB 쿼리/초 | ~1000+ | ~10 이하 | 캐시 TTL에 따라 변동 |
| DBCP pending | 다수 | 거의 없음 | 커넥션 여유 |
| CPU 사용량 | 높음 | 낮음 | 폴링 제거 |
| p95 응답시간 | 불안정 | 안정적 | < 500ms 목표 |

---

## 구현 계획

### Phase 1: Event-Driven 전환 (폴링 제거)
1. `AllocationChangedEvent` 도메인 이벤트 정의
2. `AllocationService`에서 상태 변경 시 이벤트 발행
3. `SseAllocationStatusBroadcaster`에서 `@Scheduled` 제거, 이벤트 리스너로 전환

### Phase 2: 캐시 적용 (REST API 최적화)
1. `@Cacheable` 주석 해제 (현재 비활성화됨)
2. 캐시 eviction 전략 결정 (TTL vs 이벤트 기반)

### Phase 3: 인덱스 최적화
```java
@Index(name = "idx_match_block_updated", columnList = "matchId, blockId, updatedAt")
```

---

## 참고
- 현재 코드: `SseAllocationStatusBroadcaster.java:103-145`
- Load Test: `infra/k6/scripts/allocation-status-load-test.js`

---

## 추가 분석: Event-Driven이 항상 좋은 건 아니다

### 문제 제기

> "티켓팅 오픈 시 변경이 초당 수천 건 발생하면,
> Event-Driven이 폴링보다 더 부하가 크지 않나?"

**맞는 말이다.**

### 쉬운 비유로 이해하기

#### 현재 방식 (Polling)
```
매니저가 1초마다 직원들한테 물어봄:
"뭐 바뀐 거 있어?" → 직원이 서류함(DB) 뒤져서 확인 → "없어요" or "있어요"

문제: 바뀐 게 없어도 매번 서류함을 뒤진다 (DB 조회)
```

#### Event-Driven 방식
```
직원이 뭔가 바뀌면 직접 매니저한테 보고:
"이거 바뀌었어요!" → 매니저가 팀원들한테 전파

장점: 서류함 안 뒤져도 됨 (DB 조회 없음)
단점: 1초에 1000명이 보고하면? 매니저가 1000번 전파해야 함
```

### 숫자로 비교

**상황: 블록 10개 구독 중, 초당 1000건 좌석 변경 발생**

| 방식 | DB 조회 | SSE 전송 | 총 작업량 |
|------|---------|----------|-----------|
| Polling (현재) | 10회/초 | 변경 있을 때만 | DB가 병목 |
| Event-Driven | 0회/초 | 1000회/초 | SSE가 병목 |

→ **병목이 DB에서 SSE로 옮겨갈 뿐**, 근본 해결이 아님

### 그래서 진짜 문제가 뭔데?

```
현재 코드의 진짜 문제:

GET /api/matches/{matchId}/blocks/{blockId}/seats
    ↓
AllocationStatusService.getAllocationStatusSnapShotByMatchIdAndBlockId()
    ↓
DB 조회 (매번!)  ← 이게 문제!
    ↓
응답
```

**1000명이 동시에 요청하면 → 1000번 DB 조회**

폴링이 문제가 아니라, **같은 데이터를 매번 DB에서 읽는 게 문제**.

### 가장 단순한 해결책: 캐시

```
GET /api/matches/{matchId}/blocks/{blockId}/seats
    ↓
캐시 확인 → HIT? → 바로 응답 (DB 조회 X)
    ↓ MISS
DB 조회 → 캐시 저장 → 응답
```

**1000명이 동시에 요청해도:**
- 첫 1명: DB 조회 + 캐시 저장
- 나머지 999명: 캐시에서 읽기 (DB 조회 X)

### 현실적인 우선순위

```
1순위: 캐시 활성화
       - 현재 주석 처리된 @Cacheable 활성화
       - 즉시 효과, 코드 변경 최소

2순위: 복합 인덱스 추가
       - (matchId, blockId, updatedAt)
       - 캐시 미스 시에도 빠른 조회

3순위: (필요 시) Event-Driven + Debouncing
       - 500ms마다 변경 모아서 전송
       - 실시간성이 중요할 때만 고려
```

### 결론

| 해결책 | 효과 | 복잡도 | 추천 |
|--------|------|--------|------|
| 캐시 | DB 조회 90%+ 감소 | 낮음 | ⭐ 먼저 하자 |
| Event-Driven | 폴링 쿼리 제거 | 중간 | 캐시로 부족할 때 |
| Event + Debounce | 부하 분산 | 높음 | 나중에 |

**"가장 단순한 해결책이 가장 좋은 해결책"**

---

## Request Collapsing: 캐시 없이 동시 요청 최적화

### 캐시의 문제점

1. **일관성 문제**: TTL 동안 stale data 반환 가능
2. **병목 이동**: DB 부하 → Redis 부하로 옮겨갈 뿐

### Request Collapsing이란?

> 동시에 들어온 **같은 요청을 하나로 합쳐서** DB 쿼리 1번만 실행

#### 캐시 vs Request Collapsing

```
[캐시]
09:00:00.000  A 요청 → DB 조회 → 결과 저장 (TTL 10초)
09:00:00.001  B 요청 → 캐시에서 읽기
09:00:05.000  C 요청 → 캐시에서 읽기 (5초 전 데이터, stale 가능)
09:00:10.000  캐시 만료 → 다시 DB 조회

문제: TTL 동안 데이터가 바뀌어도 옛날 데이터 반환
```

```
[Request Collapsing]
09:00:00.000  A 요청 → DB 조회 시작 (진행 중...)
09:00:00.001  B 요청 → "A가 조회 중이네, 기다림"
09:00:00.002  C 요청 → "A가 조회 중이네, 기다림"
09:00:00.050  A 조회 완료 → A, B, C 모두에게 같은 결과 반환

09:00:00.100  D 요청 → 새로 DB 조회 (A 요청 이미 끝났으니까)

핵심: 저장 안 함. 동시 요청만 합침. 항상 최신 데이터.
```

#### 비교 표

| 구분 | 캐시 | Request Collapsing |
|------|------|-------------------|
| 데이터 저장 | O (TTL 동안) | X |
| 일관성 | stale 가능 | 항상 최신 |
| 외부 의존성 | Redis 필요 | 없음 (인메모리) |
| 효과 범위 | 시간 기반 | 동시 요청만 |

---

### 동작 원리

```
동시에 1000개 요청이 들어올 때:

┌─────────────────────────────────────────────────────┐
│  요청 A (첫 번째)                                    │
│  └─▶ inFlight에 Future 등록                         │
│  └─▶ DB 조회 시작                                   │
├─────────────────────────────────────────────────────┤
│  요청 B, C, ... Z (동시에 들어옴)                    │
│  └─▶ inFlight 확인 → "A가 진행 중이네"              │
│  └─▶ A의 Future를 같이 기다림                       │
├─────────────────────────────────────────────────────┤
│  A의 DB 조회 완료                                    │
│  └─▶ A, B, C, ... Z 모두에게 같은 결과 반환         │
│  └─▶ inFlight에서 제거                              │
├─────────────────────────────────────────────────────┤
│  이후 새 요청                                        │
│  └─▶ inFlight 비어있음 → 새로 DB 조회               │
└─────────────────────────────────────────────────────┘
```

---

### 구현

```java
@Service
@RequiredArgsConstructor
public class AllocationStatusService
        implements GetAllocationStatusSnapShotUseCase, GetAllocationStatusChangesUseCase {

    private final LoadAllocationStatusPort loadAllocationStatusPort;

    // 진행 중인 요청 추적
    private final Map<String, CompletableFuture<AllocationStatusSnapShot>> inFlight
        = new ConcurrentHashMap<>();

    @Override
    public AllocationStatusSnapShot getAllocationStatusSnapShotByMatchIdAndBlockId(
            Long matchId, Long blockId) {

        String key = matchId + ":" + blockId;

        CompletableFuture<AllocationStatusSnapShot> future = inFlight.computeIfAbsent(key, k ->
            CompletableFuture
                .supplyAsync(() -> loadAllocationStatusPort
                    .loadAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId))
                .whenComplete((result, ex) -> inFlight.remove(key))  // 완료 시 제거
        );

        try {
            return future.get(5, TimeUnit.SECONDS);  // 타임아웃 설정
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("조회 타임아웃", e);
        } catch (Exception e) {
            throw new RuntimeException("조회 실패", e);
        }
    }
}
```

#### 핵심 포인트

| 코드 | 역할 |
|------|------|
| `ConcurrentHashMap` | 스레드 안전한 진행 중 요청 관리 |
| `computeIfAbsent` | 없으면 생성, 있으면 기존 것 반환 (atomic) |
| `whenComplete` | 성공/실패 모두 inFlight에서 제거 |
| `future.get(5, SECONDS)` | 무한 대기 방지 |

---

### 효과

**동시 1000개 요청 시:**

| 방식 | DB 쿼리 | 비고 |
|------|---------|------|
| 현재 | 1000개 | 각각 조회 |
| 캐시 | 1개 + TTL 동안 0개 | stale 가능 |
| Request Collapsing | 1개 | 항상 최신 |

**멀티 인스턴스 환경:**

| 인스턴스 수 | 동시 1000 요청 | DB 쿼리 |
|-------------|---------------|---------|
| 1개 | 1000 → 1 | 1개 |
| 3개 | 333 + 333 + 334 → 1 + 1 + 1 | 3개 |
| 10개 | 100씩 분배 → 각 1개씩 | 10개 |

→ 인스턴스 수만큼 쿼리 발생하지만, **1000개보다 훨씬 적음**

---

### 주의사항

#### 1. 에러 전파
```
A 요청이 실패하면 → B, C도 같이 실패
```
- 하지만 DB 장애 상황이면 어차피 다 실패
- 개별 요청이 실패할 이유가 없음 (같은 쿼리니까)

#### 2. 스레드 블로킹
```java
future.get(5, TimeUnit.SECONDS);  // 블로킹 호출
```
- Tomcat 스레드가 대기 상태로 점유됨
- 해결: Virtual Thread (Java 21+) 사용 시 문제 없음

#### 3. 메모리 누수 방지
```java
.whenComplete((result, ex) -> inFlight.remove(key))
```
- 반드시 완료 시 제거해야 함
- `whenComplete`는 성공/실패 모두 실행됨

---

### 캐시와 함께 쓸 수도 있다

```
[Request Collapsing + 캐시]

1. 캐시 확인 → HIT? → 바로 반환
2. MISS? → Request Collapsing으로 DB 조회
3. 결과 캐시에 저장
```

하지만 **일관성이 중요하면 Request Collapsing만 사용**하는 게 단순함.

---

### 결론

| 상황 | 추천 방식 |
|------|----------|
| 일관성 중요 | Request Collapsing |
| 약간의 stale 허용 | 캐시 |
| 둘 다 | Collapsing + 짧은 TTL 캐시 |

**Request Collapsing은 캐시의 일관성 문제 없이 동시 요청을 최적화하는 방법이다.**
