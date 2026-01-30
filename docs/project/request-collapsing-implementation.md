# Request Collapsing 구현 과정 및 트러블슈팅

## 개요

좌석 현황 조회 API에 Request Collapsing을 적용하여 동시 요청 시 DB 쿼리를 1번만 실행하도록 최적화하는 과정에서 발생한 문제들과 해결 과정을 기록합니다.

## Request Collapsing이란?

동일한 데이터를 요청하는 여러 요청이 동시에 들어올 때, 첫 번째 요청만 실제로 DB를 조회하고 나머지 요청들은 그 결과를 공유하는 패턴입니다.

```
요청 A ─┐
요청 B ─┼─→ DB 쿼리 1번 ─→ 결과 공유
요청 C ─┘
```

## 테스트 환경

- **k6 부하 테스트**: 1000 VU, 2분간 실행
- **서버**: EC2 (t3.small 추정)
- **기준선 (none 전략)**: 601 req/s, 0% 실패율

---

## 시도 1: 비동기 방식 + whenComplete (실패)

### 코드

```java
CompletableFuture<AllocationStatusSnapShot> future = inFlightSnapshots.computeIfAbsent(key, k ->
    CompletableFuture
        .supplyAsync(() -> loadAllocationStatusPort
            .loadAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId))
        .whenComplete((result, ex) -> inFlightSnapshots.remove(key))
);
return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
```

### 에러

```
Caused by: java.lang.IllegalStateException: Recursive update
    at java.util.concurrent.ConcurrentHashMap.computeIfAbsent
```

### 원인: Recursive Update

`ConcurrentHashMap.computeIfAbsent()` **내부에서** 같은 맵을 수정하려고 할 때 발생합니다.

```
1. computeIfAbsent(key, ...) 호출 → 맵 락 획득
2. 람다 실행: CompletableFuture 생성
3. supplyAsync가 "매우 빠르게" 완료됨 (같은 스레드에서 동기 실행될 수 있음)
4. whenComplete 즉시 실행 → remove(key) 호출
5. 💥 아직 computeIfAbsent가 끝나지 않았는데 같은 맵을 수정하려 함
```

**왜 "가끔" 발생하는가?**
- `supplyAsync`는 보통 **다른 스레드**에서 실행됨 → 문제 없음
- 하지만 ForkJoinPool이 **포화**되면 **같은 스레드**에서 실행될 수 있음

---

## 시도 2: 비동기 방식 + whenCompleteAsync (실패)

### 코드

```java
CompletableFuture.supplyAsync(() -> loadFromDb(...))
    .whenCompleteAsync((result, ex) -> inFlightSnapshots.remove(key))
```

### 결과

| 지표 | 값 |
|------|-----|
| 실패율 | 14.13% |
| 처리량 | 67 req/s |
| p(95) | 59.99s |

### 원인: ForkJoinPool.commonPool() 병목

- `supplyAsync()`는 기본적으로 `ForkJoinPool.commonPool()` 사용
- **스레드 수 = CPU 코어 - 1** (예: 2코어 → 1개 스레드)
- 1000 VU가 동시에 요청하면 → 대부분 대기 → 5초 타임아웃

```
문제 상황:
┌─────────────────┐
│ Tomcat Thread   │
│     ↓           │
│ supplyAsync()   │ → ForkJoinPool에 작업 위임 (1-2개 스레드)
│     ↓           │
│ future.get(5s)  │ → 대기...
│     ↓           │
│ TimeoutException│ ← ForkJoinPool이 바빠서 5초 초과
└─────────────────┘
```

---

## 시도 3: 전용 CachedThreadPool 사용 (실패)

### 코드

```java
private static final Executor COLLAPSING_EXECUTOR = Executors.newCachedThreadPool();
private static final Executor CLEANUP_EXECUTOR = Executors.newSingleThreadExecutor();

CompletableFuture.supplyAsync(() -> loadFromDb(...), COLLAPSING_EXECUTOR)
    .whenCompleteAsync((result, ex) -> remove(key), CLEANUP_EXECUTOR)
```

### 결과

| 지표 | 값 |
|------|-----|
| 실패율 | 47.83% |
| 처리량 | 21.5 req/s |
| 에러 | EOF, request timeout |

### 원인: 스레드 폭증 + 리소스 고갈

- `CachedThreadPool`은 **제한 없이** 스레드 생성
- 1000 VU × 초당 수십 요청 = **수천 개 스레드 생성**
- 결과: 메모리 부족, DB 커넥션 풀 고갈, 서버 다운

---

## 시도 4: 비동기 + future.cancel(true) (실패)

### 코드

```java
try {
    return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    future.cancel(true);  // 타임아웃 시 취소
    throw new RuntimeException("조회 타임아웃", e);
}
```

### 결과

| 지표 | 값 |
|------|-----|
| 실패율 | 98.62% |
| 에러 | CancellationException |

### 원인: 공유 Future 취소

Request Collapsing에서 **여러 요청이 같은 Future를 공유**합니다.

```
1. 요청 A, B, C가 같은 Future 공유
2. 요청 A가 타임아웃 → future.cancel(true)
3. Future가 취소됨
4. 요청 B, C도 CancellationException 발생!
```

---

## 시도 5: 동기 방식 (성공!)

### 코드

```java
private AllocationStatusSnapShot loadWithCollapsing(Long matchId, Long blockId) {
    String key = matchId + ":" + blockId;

    // 이미 진행 중인 요청이 있으면 그 결과를 기다림
    CompletableFuture<AllocationStatusSnapShot> existing = inFlightSnapshots.get(key);
    if (existing != null) {
        return waitForResult(existing, matchId, blockId);
    }

    // 새로운 Future 생성 및 등록 시도
    CompletableFuture<AllocationStatusSnapShot> newFuture = new CompletableFuture<>();
    CompletableFuture<AllocationStatusSnapShot> registered = inFlightSnapshots.putIfAbsent(key, newFuture);

    // 다른 스레드가 먼저 등록했으면 그 결과를 기다림
    if (registered != null) {
        return waitForResult(registered, matchId, blockId);
    }

    // 첫 번째 스레드: 직접 실행
    try {
        AllocationStatusSnapShot result = loadAllocationStatusPort
                .loadAllocationStatusSnapShotByMatchIdAndBlockId(matchId, blockId);
        newFuture.complete(result);
        return result;
    } catch (Exception e) {
        newFuture.completeExceptionally(e);
        throw e;
    } finally {
        inFlightSnapshots.remove(key);
    }
}
```

### 결과

| 지표 | 값 |
|------|-----|
| 실패율 | **0.03%** |
| 처리량 | **539 req/s** |
| p(95) | 1.01s |

### 왜 성공했는가?

1. **ForkJoinPool 병목 없음**: Tomcat 스레드가 직접 DB 조회
2. **Recursive update 없음**: `computeIfAbsent` 대신 `get` + `putIfAbsent` 사용
3. **future.cancel() 없음**: 공유 Future 취소 문제 없음
4. **스레드 폭증 없음**: 별도 스레드 풀 사용 안함

---

## 비동기 vs 동기 비교

| 항목 | 비동기 (supplyAsync) | 동기 |
|------|---------------------|------|
| DB 조회 스레드 | ForkJoinPool (1-2개) | Tomcat (200개) |
| 동시 처리 능력 | 낮음 | 높음 |
| 스레드 풀 병목 | 있음 | 없음 |
| 1000 VU 부하 | 실패 | 성공 |

---

## 최종 결론

### Request Collapsing 구현 시 주의사항

1. **비동기(supplyAsync) 사용 주의**
   - ForkJoinPool.commonPool()의 스레드 수는 CPU 코어 - 1개로 제한
   - 고부하 상황에서 병목 발생

2. **ConcurrentHashMap.computeIfAbsent() 내부에서 맵 수정 금지**
   - Recursive update 에러 발생
   - `get` + `putIfAbsent` 패턴 사용 권장

3. **공유 Future에 cancel() 호출 금지**
   - 다른 요청들도 함께 취소됨

4. **동기 방식 권장**
   - 첫 번째 스레드가 직접 실행
   - 나머지 스레드는 결과 대기
   - Tomcat 스레드 풀 활용으로 높은 동시성 지원

### 성능 비교 요약

| 전략 | 실패율 | 처리량 |
|------|--------|--------|
| none (기준선) | 0% | 601 req/s |
| collapsing (비동기) | 14~98% | 21~67 req/s |
| **collapsing (동기)** | **0.03%** | **539 req/s** |
