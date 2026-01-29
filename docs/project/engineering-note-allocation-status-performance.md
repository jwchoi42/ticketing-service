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

---

## 추가 고도화 방안: CDN 패턴 적용

> 참고: [Cloudflare Revalidation](https://developers.cloudflare.com/cache/concepts/revalidation/), [Vercel CDN Request Collapsing](https://vercel.com/blog/cdn-request-collapsing)

### 현재 구현 vs CDN 패턴 비교

| 기능 | 현재 구현 | CDN 패턴 |
|------|----------|----------|
| Request Collapsing | ✅ 적용됨 | ✅ |
| 타임아웃 | ✅ 5초 | ✅ 3초 |
| stale-while-revalidate | ❌ 없음 | ✅ |
| CDN 캐싱 | ❌ 없음 | ✅ |
| ETag 조건부 요청 | ❌ 없음 | ✅ |

---

### 1. Stale-While-Revalidate 패턴

#### 개념

```
[일반 캐시]
캐시 만료 → 새 데이터 조회 (대기) → 응답

[stale-while-revalidate]
캐시 만료 → 이전 데이터 즉시 반환 + 백그라운드 갱신
```

#### Cloudflare 동작 방식

```
1,000개 동시 요청 + TTL 만료 상태:

- 1개 요청: Origin 서버로 재검증 요청
- 999개 요청: "UPDATING" 상태로 만료된 캐시에서 즉시 응답

→ Origin 부하: 1개만 / 사용자 대기: 0ms
```

#### 구현 예시

```java
public class AllocationStatusService {

    // 마지막 조회 결과 저장 (stale data)
    private final Map<String, AllocationStatusSnapShot> lastKnown = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastUpdated = new ConcurrentHashMap<>();

    private static final Duration STALE_THRESHOLD = Duration.ofSeconds(1);

    public AllocationStatusSnapShot getSnapshot(Long matchId, Long blockId) {
        String key = matchId + ":" + blockId;

        AllocationStatusSnapShot stale = lastKnown.get(key);
        LocalDateTime updated = lastUpdated.get(key);

        // 1. stale 데이터가 있고, 아직 유효 기간 내면 즉시 반환 + 백그라운드 갱신
        if (stale != null && updated != null) {
            if (Duration.between(updated, LocalDateTime.now()).compareTo(STALE_THRESHOLD) < 0) {
                return stale;  // 아직 fresh
            }
            // stale이지만 즉시 반환, 백그라운드에서 갱신
            refreshInBackground(key, matchId, blockId);
            return stale;
        }

        // 2. 없으면 동기 조회 (첫 요청)
        return fetchAndCache(key, matchId, blockId);
    }

    private void refreshInBackground(String key, Long matchId, Long blockId) {
        CompletableFuture.runAsync(() -> fetchAndCache(key, matchId, blockId));
    }

    private AllocationStatusSnapShot fetchAndCache(String key, Long matchId, Long blockId) {
        AllocationStatusSnapShot snapshot = loadAllocationStatusPort
            .loadAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId);
        lastKnown.put(key, snapshot);
        lastUpdated.put(key, LocalDateTime.now());
        return snapshot;
    }
}
```

#### 트레이드오프

| 장점 | 단점 |
|------|------|
| 응답 즉시 (0ms 대기) | 1초 이내 stale data 가능 |
| Origin 부하 대폭 감소 | 메모리 사용 증가 |

---

### 2. HTTP Cache-Control 헤더

#### 개념

CDN(nginx, Cloudflare)이 API 응답을 캐싱하도록 헤더 설정.

```
Cache-Control: public, max-age=1, stale-while-revalidate=5
```

- `max-age=1`: 1초간 fresh (CDN에서 바로 응답)
- `stale-while-revalidate=5`: 만료 후 5초간 stale 응답 허용 + 백그라운드 갱신

#### 구현 예시

```java
@GetMapping("/seats")
public ResponseEntity<SuccessResponse<AllocationStatusSnapShot>> getSnapshot(
        @PathVariable Long matchId, @PathVariable Long blockId) {

    AllocationStatusSnapShot snapshot = service.getSnapshot(matchId, blockId);

    return ResponseEntity.ok()
        .cacheControl(CacheControl
            .maxAge(1, TimeUnit.SECONDS)
            .staleWhileRevalidate(5, TimeUnit.SECONDS)
            .cachePublic())
        .body(SuccessResponse.of(snapshot));
}
```

#### 효과

```
CDN에서 1000개 요청 처리:

[max-age 이내]
→ CDN 캐시에서 즉시 응답 (Origin 요청 0개)

[max-age 만료, stale-while-revalidate 이내]
→ 999개: CDN 캐시에서 stale 응답
→ 1개: Origin으로 재검증 요청
```

---

### 3. ETag 조건부 요청

#### 개념

데이터가 변경되지 않았으면 304 Not Modified 반환 (본문 없음).

```
Client: If-None-Match: "abc123"
Server: 304 Not Modified (변경 없음) 또는 200 OK + 새 데이터
```

#### 구현 예시

```java
@GetMapping("/seats")
public ResponseEntity<SuccessResponse<AllocationStatusSnapShot>> getSnapshot(
        @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
        @PathVariable Long matchId, @PathVariable Long blockId) {

    AllocationStatusSnapShot snapshot = service.getSnapshot(matchId, blockId);
    String etag = "\"" + snapshot.hashCode() + "\"";  // 또는 더 정교한 해시

    if (etag.equals(ifNoneMatch)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
            .eTag(etag)
            .build();
    }

    return ResponseEntity.ok()
        .eTag(etag)
        .body(SuccessResponse.of(snapshot));
}
```

#### 효과

| 상황 | 응답 |
|------|------|
| 데이터 변경됨 | 200 OK + 전체 데이터 |
| 데이터 변경 없음 | 304 Not Modified (본문 없음) |

→ 네트워크 대역폭 절약, 클라이언트 파싱 비용 감소

---

### 4. Vercel의 Double-Checked Locking

#### 개념

```
1. 캐시 확인 → HIT면 반환
2. 잠금 획득
3. 캐시 재확인 (대기 중 다른 요청이 채웠을 수 있음)
4. 필요시에만 실행
```

#### 현재 구현과 비교

```java
// 현재 구현 (유사하게 적용됨)
CompletableFuture<AllocationStatusSnapShot> existing = inFlightSnapshots.get(key);
if (existing != null) {
    return waitForResult(existing);  // 다른 요청 기다림
}

CompletableFuture<AllocationStatusSnapShot> newFuture = new CompletableFuture<>();
CompletableFuture<AllocationStatusSnapShot> registered = inFlightSnapshots.putIfAbsent(key, newFuture);

if (registered != null) {
    return waitForResult(registered);  // Double-check
}

// 실행
```

→ **이미 유사하게 적용되어 있음**

---

### 적용 권장 조합

| 실시간성 요구 | 권장 조합 |
|--------------|----------|
| 매우 높음 (0~1초) | Request Collapsing (현재) |
| 높음 (1~2초) | + HTTP Cache-Control (max-age=1) |
| 보통 (2~5초) | + stale-while-revalidate |
| 낮음 | + ETag + 긴 TTL |

#### 좌석 현황 특성

- 티켓팅 오픈 시: 실시간성 매우 중요 → 현재 구현 유지
- 일반 조회 시: 1~2초 stale 허용 가능 → Cache-Control 추가 고려

---

### 결론

현재 Request Collapsing 구현은 Vercel/Cloudflare의 핵심 패턴과 동일한 방식.
추가로 HTTP 캐시 헤더를 적용하면 CDN 레벨에서도 최적화 가능.

**다만 좌석 현황의 실시간성 요구사항에 따라 선택적으로 적용해야 함.**

---

## 현재 구현의 한계와 개선 필요성

### 1. Stale-While-Revalidate 없음

#### 현재 동작

```
1000명 동시 요청 (첫 요청)

요청 1 ──▶ DB 조회 시작 (20ms)
요청 2~1000 ──▶ 요청 1 기다림 (20ms)
                    ↓
              20ms 후 모두 응답
```

#### 문제가 되는 시나리오

Request Collapsing은 **"동시" 요청만 합침**. 연속적으로 들어오는 요청은 각각 DB를 때림.

```
[시간 T+0] 요청 A 완료 → 응답 반환 → inFlight에서 제거됨
[시간 T+1ms] 요청 B 도착 → inFlight 비어있음 → 새로 DB 조회
[시간 T+2ms] 요청 C 도착 → inFlight 비어있음 → 새로 DB 조회
```

#### 숫자로 비교

```
초당 1000 요청이 균등하게 분산되면:

[현재]
- 1ms마다 1개 요청 도착
- 각 요청마다 DB 조회
- 초당 ~1000 쿼리

[stale-while-revalidate 적용 시]
- 1초간 stale 데이터 반환
- 백그라운드에서 1초마다 1번 갱신
- 초당 1 쿼리
```

#### 그림으로 비교

```
[현재 - Request Collapsing만]

시간 →  0ms    20ms    40ms    60ms
        │       │       │       │
요청 A ─┼───DB──┼───────┼───────┼
요청 B ─┼───────┼───DB──┼───────┼  (A 끝난 후 도착)
요청 C ─┼───────┼───────┼───DB──┼  (B 끝난 후 도착)

DB 쿼리: 3번


[stale-while-revalidate 적용]

시간 →  0ms    20ms    40ms    60ms
        │       │       │       │
요청 A ─┼───DB──┼───────┼───────┼  (캐시 저장)
요청 B ─┼───────┼─stale─┼───────┼  (즉시 반환)
요청 C ─┼───────┼───────┼─stale─┼  (즉시 반환)
        │       │       │       │
        └───────────────────────┘
              백그라운드 갱신 (1초마다)

DB 쿼리: 1번 + 백그라운드 1번/초
```

---

### 2. HTTP Cache-Control 없음

#### 현재 동작

```
클라이언트 1000명 → nginx → Spring 서버 → DB

모든 요청이 Spring 서버까지 도달
```

#### 문제점

```
[현재]
nginx: 그냥 프록시 역할만
        ↓
모든 요청이 Spring까지 감
        ↓
Request Collapsing이 Spring에서 처리
        ↓
Spring 스레드 1000개 점유 (대기 중이어도)
```

#### Cache-Control 있으면

```
[개선]
클라이언트 → nginx (Cache-Control 확인)
                ↓
        캐시 HIT? → 바로 응답 (Spring 안 감)
        캐시 MISS? → Spring으로 전달
                        ↓
                    1개만 Origin 요청
```

#### 효과 비교

| 항목 | 현재 | Cache-Control 적용 |
|------|------|-------------------|
| nginx → Spring 요청 | 1000개 | 1개 |
| Spring 스레드 사용 | 1000개 | 1개 |
| 응답 속도 | 20ms | <1ms (캐시 HIT) |

---

### 3. ETag 없음

#### 현재 동작

```
클라이언트가 1초마다 폴링:

GET /seats → 200 OK + 전체 데이터 (10KB)
GET /seats → 200 OK + 전체 데이터 (10KB)  ← 변경 없어도
GET /seats → 200 OK + 전체 데이터 (10KB)  ← 매번 전체 전송
```

#### 문제점

- 데이터 변경 없어도 매번 전체 전송
- 네트워크 대역폭 낭비
- 클라이언트 파싱 비용

#### ETag 있으면

```
GET /seats
→ 200 OK + ETag: "abc123" + 전체 데이터 (10KB)

GET /seats (If-None-Match: "abc123")
→ 304 Not Modified (본문 없음, 0KB)  ← 변경 없으면

GET /seats (If-None-Match: "abc123")
→ 200 OK + ETag: "def456" + 전체 데이터 (10KB)  ← 변경 있으면
```

#### 효과 비교

| 상황 | 현재 | ETag 적용 |
|------|------|----------|
| 변경 없음 | 10KB 전송 | 0KB (304) |
| 변경 있음 | 10KB 전송 | 10KB 전송 |

→ 좌석 100개 블록에서 1개만 변경되어도 전체 10KB를 보내야 하는데,
**변경 없으면 아예 안 보내도 됨.**

---

### 4. 종합 비교

#### 시나리오: 초당 1000 요청, 1초에 1번 좌석 상태 변경

| 구현 | DB 쿼리/초 | 네트워크 | Spring 스레드 |
|------|-----------|----------|--------------|
| 현재 (Collapsing만) | ~50 (동시성에 따라) | 10MB/s | 1000개 점유 |
| + stale-while-revalidate | 1 | 10MB/s | 1000개 점유 |
| + Cache-Control | 1 | 10MB/s | 1개 |
| + ETag | 1 | ~100KB/s | 1개 |
| 전체 적용 | 1 | ~100KB/s | 1개 |

---

### 5. 실시간성 vs 효율성 트레이드오프

| 방안 | 실시간성 손실 | 효율 개선 | 복잡도 |
|------|-------------|----------|--------|
| stale-while-revalidate | 1~2초 | DB 쿼리 대폭 감소 | 중 |
| Cache-Control | 1~2초 | Spring 부하 감소 | 낮음 |
| ETag | 없음 | 네트워크 절약 | 중 |

#### 좌석 현황 특성

```
티켓팅 오픈 순간:
- 실시간성 매우 중요
- 1~2초 stale은 문제될 수 있음
- 현재 구현 유지가 안전

일반 시간대:
- 실시간성 덜 중요
- stale-while-revalidate 적용 가능
- 서버 부하 대폭 감소
```

---

### 6. 향후 적용 우선순위

| 순위 | 방안 | 이유 |
|------|------|------|
| 1 | ETag | 실시간성 손실 없이 네트워크 절약 |
| 2 | Cache-Control (짧은 TTL) | nginx에서 처리, Spring 부하 감소 |
| 3 | stale-while-revalidate | 실시간성 요구사항 확인 후 적용 |

---

## 캐시 + Evict 방식: 일관성 문제 해결

### 기존 캐시의 문제점 (TTL 기반)

```
09:00:00.000  A 요청 → DB 조회 → 캐시 저장 (TTL 10초)
09:00:00.001  B 요청 → 캐시 HIT
09:00:03.000  좌석 상태 변경됨 (DB에는 반영)
09:00:05.000  C 요청 → 캐시 HIT (3초 전 데이터, stale!)
09:00:10.000  캐시 만료 → 다시 DB 조회

문제: TTL 동안 데이터가 바뀌어도 옛날 데이터 반환
```

### 캐시 + Evict 방식

```
09:00:00.000  A 요청 → DB 조회 → 캐시 저장
09:00:00.001  B 요청 → 캐시 HIT
09:00:03.000  좌석 상태 변경 → @CacheEvict 실행 → 캐시 삭제
09:00:05.000  C 요청 → 캐시 MISS → DB 조회 → 캐시 저장 (최신 데이터!)
```

**변경 시점에 캐시를 지우니까 항상 최신 데이터.**

### 구현 예시

```java
// 조회 시 캐시 사용
@Service
public class AllocationStatusService {

    @Cacheable(value = "snapshot", key = "#matchId + ':' + #blockId")
    public AllocationStatusSnapShot getSnapshot(Long matchId, Long blockId) {
        return loadAllocationStatusPort
            .loadAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId);
    }
}

// 변경 시 캐시 무효화
@Service
public class AllocationService {

    private final ApplicationEventPublisher eventPublisher;

    public void hold(Long matchId, Long blockId, Long seatId) {
        // 좌석 배정 로직
        allocationRepository.save(...);

        // 캐시 무효화 이벤트 발행
        eventPublisher.publishEvent(new AllocationChangedEvent(matchId, blockId));
    }
}

// 이벤트 리스너에서 캐시 evict
@Component
public class AllocationCacheEvictor {

    private final CacheManager cacheManager;

    @TransactionalEventListener
    public void onAllocationChanged(AllocationChangedEvent event) {
        Cache cache = cacheManager.getCache("snapshot");
        if (cache != null) {
            cache.evict(event.matchId() + ":" + event.blockId());
        }
    }
}
```

### 비교

| 방식 | 일관성 | DB 부하 | Redis 부하 | 복잡도 |
|------|--------|---------|-----------|--------|
| Request Collapsing (현재) | ✅ 최신 | 동시요청만 합침 | 없음 | 중 |
| 캐시 + TTL | ❌ stale | 낮음 | 있음 | 낮음 |
| 캐시 + Evict | ✅ 최신 | 낮음 | 있음 | 낮음 |
| Collapsing + 캐시 + Evict | ✅ 최신 | 매우 낮음 | 있음 | 중 |

### Redis 병목 우려

```
1000명 동시 요청 → Redis 조회 1000번
```

하지만 Redis는 **초당 10만+ 요청** 처리 가능해서 DB보다 훨씬 여유롭다.

| 저장소 | 처리량 | 지연 |
|--------|--------|------|
| MySQL | 수천 qps | 1~10ms |
| Redis | 10만+ qps | <1ms |

### Request Collapsing + 캐시 + Evict 조합

가장 효율적인 조합:

```
요청 → 캐시 확인
        ↓
    HIT? → 즉시 반환 (Redis)
    MISS? → Request Collapsing으로 DB 조회 → 캐시 저장
        ↓
변경 발생 → Evict로 캐시 무효화
```

효과:
- 캐시 HIT: Redis에서 즉시 반환 (<1ms)
- 캐시 MISS + 동시 요청: DB 쿼리 1번만
- 변경 시: Evict로 일관성 보장

---

## 왜 캐시 없이 먼저 해결해야 하는가?

> 멘토: "캐시를 쓰지 말고 해결해봐"

### 1. 캐시는 "쉬운 해결책"이다

```
문제: DB 부하가 높다
쉬운 답: 캐시 쓰자!
```

캐시는 누구나 생각할 수 있는 해결책. 면접에서도:

```
면접관: "DB 부하가 높으면 어떻게 하시겠어요?"
지원자 A: "캐시 쓰겠습니다"  ← 모든 지원자가 이렇게 답함
지원자 B: "Request Collapsing 패턴으로 동시 요청을 합치고,
          인덱스 최적화를 하고, 그래도 부족하면 캐시를 고려합니다"
```

**캐시 없이 해결한 경험은 차별화 포인트가 됨.**

### 2. 캐시는 문제를 "숨긴다"

```
[캐시 적용 전]
쿼리 1000번 → 느리다 → 문제 인식

[캐시 적용 후]
캐시 HIT → 빠르다 → 문제 해결된 것 같음
                     ↓
              근본 원인은 그대로
              (느린 쿼리, 잘못된 인덱스, N+1 등)
```

캐시 없이 해결하면:
- 쿼리 최적화
- 인덱스 튜닝
- 아키텍처 개선

**근본적인 해결 능력을 기르게 됨.**

### 3. 캐시는 새로운 문제를 가져온다

| 캐시 없을 때 | 캐시 있을 때 새로 생기는 문제 |
|-------------|------------------------------|
| DB 부하 | 캐시 일관성 문제 |
| | 캐시 무효화 전략 |
| | 캐시 서버 장애 시 대응 |
| | 분산 환경 동기화 |
| | 메모리 관리 |
| | 캐시 웜업 (Cold Start) |

```
"캐시 서버 죽으면 서비스 전체가 죽는다"
→ 실무에서 흔히 겪는 장애
```

### 4. 인프라 복잡도 증가

```
[캐시 없이]
Client → Spring → DB

[캐시 있으면]
Client → Spring → Redis → DB
                    ↓
              관리 포인트 증가
              - Redis 클러스터 운영
              - 모니터링
              - 장애 대응
              - 비용
```

**작은 서비스에서는 오버엔지니어링일 수 있음.**

### 5. 다양한 패턴을 배우라는 의도

```
캐시: 같은 데이터를 "시간" 기준으로 재사용
Request Collapsing: 같은 데이터를 "동시성" 기준으로 재사용
```

- 캐시만 알면 → 모든 문제에 캐시를 적용하려 함
- 다양한 패턴을 알면 → 상황에 맞는 해결책 선택 가능

### 6. 캐시 없이 해결하면서 배운 것들

| 주제 | 배운 것 |
|------|--------|
| 동시성 | `ConcurrentHashMap`, `CompletableFuture`, `putIfAbsent` |
| 패턴 | Request Collapsing, Double-Checked Locking |
| DB | 복합 인덱스 최적화, 쿼리 분석 |
| 트랜잭션 | `@Transactional`과 멀티스레드 문제, `TransactionTemplate` |
| CDN 패턴 | stale-while-revalidate, ETag, Cache-Control |
| 아키텍처 | 트레이드오프 분석, 실시간성 vs 효율성 |

**캐시 한 줄 추가했으면 이런 걸 배울 기회가 없었음.**

### 결론

> "캐시는 나중에 언제든 쓸 수 있다.
> 먼저 캐시 없이 해결해보면서 근본적인 문제 해결 능력을 기르고,
> 다양한 패턴을 배워라."

그리고 이제 캐시를 쓸 때도 **왜 캐시가 필요한지, 어떤 문제가 있는지** 이해하고 쓸 수 있게 됨.

캐시는 "마지막 수단"이 아니라 "상황에 맞는 선택지 중 하나"로 바라볼 수 있게 됨.

---

## 캐시의 숨겨진 복잡성

> 일관성은 Evict로, 분산 환경은 Redis로 해결된다.
> 하지만 그 외에도 고려해야 할 것들이 있다.

### 1. 캐시 무효화 전략

#### 문제: "언제 캐시를 지울 것인가?"

```java
// 간단해 보이지만...
@CacheEvict(value = "snapshot", key = "#matchId + ':' + #blockId")
public void hold(Long matchId, Long blockId, Long seatId) { }
```

#### 복잡해지는 경우들

**Case 1: 여러 곳에서 데이터 변경**

```java
// 좌석 배정
public void hold(...) { /* evict 해야 함 */ }

// 좌석 해제
public void release(...) { /* evict 해야 함 */ }

// 예약 확정
public void confirm(...) { /* evict 해야 함 */ }

// 만료 처리 (스케줄러)
@Scheduled
public void expireHolds() { /* evict 해야 함 */ }

// 관리자 강제 해제
public void adminRelease(...) { /* evict 해야 함 */ }
```

**하나라도 빠뜨리면 stale 발생.**

**Case 2: 연관 데이터 변경**

```
Block 정보 변경 → Snapshot 캐시도 무효화해야 함?
Seat 정보 변경 → Snapshot 캐시도 무효화해야 함?
Match 상태 변경 → 모든 관련 Block 캐시 무효화?
```

**Case 3: 벌크 연산**

```java
// 1000개 좌석 일괄 해제
public void releaseAll(Long matchId) {
    // 모든 블록의 캐시를 다 지워야 함
    // 블록이 100개면 100번 evict?
}
```

#### 무효화 전략 종류

| 전략 | 설명 | 장단점 |
|------|------|--------|
| Write-Through | 쓰기 시 캐시도 같이 업데이트 | 일관성 좋음, 쓰기 느림 |
| Write-Behind | 쓰기는 캐시만, 나중에 DB 반영 | 빠름, 데이터 유실 위험 |
| Cache-Aside + Evict | 현재 방식 | 구현 쉬움, 누락 위험 |
| Event-Driven Evict | 이벤트로 무효화 | 느슨한 결합, 복잡함 |

---

### 2. 메모리 관리

#### 문제: "캐시가 메모리를 다 먹으면?"

```
Match 100개 × Block 50개 = 5,000개 캐시 엔트리
각 엔트리 100KB = 500MB

티켓팅 시즌에 Match 1,000개면?
→ 5GB 메모리 필요
```

#### 해결 전략: Eviction Policy (LRU, LFU, TTL)

```bash
# Redis 설정
maxmemory 1gb
maxmemory-policy allkeys-lru  # 메모리 꽉 차면 가장 오래된 것 삭제
```

#### 문제점

```
티켓팅 오픈 직전:
- 인기 경기 캐시가 LRU로 삭제됨
- 오픈 순간 캐시 MISS → DB 폭주

"필요한 캐시가 삭제되는" 상황 발생 가능
```

#### 해결책

```java
// 중요한 캐시는 TTL 없이 유지
// 덜 중요한 캐시만 eviction 대상
@Cacheable(value = "snapshot", key = "...", unless = "#result.isHotMatch()")
```

---

### 3. 캐시 웜업 (Cold Start)

#### 문제: "서버 재시작하면 캐시가 비어있다"

```
[정상 상태]
1000 요청 → 캐시 HIT → 빠름

[서버 재시작 직후]
1000 요청 → 캐시 MISS → DB 1000번 조회 → 장애
```

#### 실제 시나리오

```
새벽 3시: 서버 배포 (재시작)
아침 9시: 티켓팅 오픈
         → 캐시 비어있음
         → 첫 요청들이 모두 DB로
         → DB 과부하
         → 서비스 장애
```

#### 해결책: 웜업 스크립트

```java
@Component
public class CacheWarmer {

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        // 서버 시작 시 주요 데이터 미리 캐시
        List<Match> hotMatches = matchRepository.findUpcomingMatches();
        for (Match match : hotMatches) {
            for (Block block : match.getBlocks()) {
                snapshotService.getSnapshot(match.getId(), block.getId());
            }
        }
    }
}
```

#### 문제점

```
웜업에 5분 걸리면?
→ 5분 동안 서버가 준비 안 됨
→ 배포 시간 증가
→ 롤링 배포 시 복잡해짐
```

---

### 4. 캐시 서버 장애 시 대응

#### 문제: "Redis가 죽으면?"

```
[정상]
요청 → Redis(캐시) → 응답

[Redis 장애]
요청 → Redis 연결 실패 → ???
```

#### 장애 시나리오들

**Case 1: Redis 연결 실패 시 예외 발생**

```java
@Cacheable("snapshot")
public AllocationStatusSnapShot getSnapshot(...) {
    // Redis 연결 실패 → RedisConnectionFailureException
    // → 서비스 전체 장애
}
```

**Case 2: Redis 느려짐 (네트워크 지연)**

```
평소: Redis 응답 1ms
장애: Redis 응답 5초

→ 모든 요청이 5초씩 대기
→ 스레드 풀 고갈
→ 서비스 장애
```

#### 해결책: Circuit Breaker 패턴

```java
@Cacheable("snapshot")
@CircuitBreaker(name = "redis", fallbackMethod = "getSnapshotFallback")
public AllocationStatusSnapShot getSnapshot(Long matchId, Long blockId) {
    return cacheManager.get(...);
}

// Redis 장애 시 DB에서 직접 조회
public AllocationStatusSnapShot getSnapshotFallback(Long matchId, Long blockId, Exception e) {
    return loadFromDb(matchId, blockId);
}
```

#### 해결책: 타임아웃 설정

```yaml
spring:
  redis:
    timeout: 500ms  # 500ms 넘으면 포기하고 DB로
```

#### 그래도 남는 문제

```
Redis 장애 → Fallback으로 DB 직접 조회
          → DB에 갑자기 트래픽 폭주
          → DB도 장애
          → 전체 시스템 장애
```

**캐시가 "있다가 없어지는" 게 "처음부터 없는 것"보다 위험할 수 있음.**

---

### 5. 캐시 복잡성 정리

| 문제 | 해결책 | 복잡도 |
|------|--------|--------|
| 일관성 | Evict | 낮음 |
| 분산 환경 | Redis | 낮음 |
| 무효화 전략 | 모든 변경 지점에 Evict 적용 | **중간** |
| 메모리 관리 | Eviction Policy + 용량 계획 | **중간** |
| 캐시 웜업 | 웜업 스크립트 | **중간** |
| 서버 장애 | Circuit Breaker + Fallback | **높음** |

### 결론

캐시를 **"그냥 쓰면"** 간단하지만, **"제대로 쓰려면"** 고려할 게 많다.

```
캐시 도입 = 단순히 @Cacheable 붙이는 것이 아니라,
           무효화 전략 + 메모리 계획 + 웜업 + 장애 대응까지 설계하는 것
```
