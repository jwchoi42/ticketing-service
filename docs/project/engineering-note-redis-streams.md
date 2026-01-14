# Engineering Note: Redis Streams를 활용한 실시간 좌석 상태 전파 시스템

> **참고 아티클**: [Redis Streams를 활용한 메시지 큐잉 시스템 구축 - LINE Engineering](https://techblog.lycorp.co.jp/ko/building-a-messaging-queuing-system-with-redis-streams)

---

## 1. Selection Reason (선정 이유)

### 1.1 현재 프로젝트의 아키텍처와 한계

현재 티케팅 서비스는 **"RDB 중심의 동시성 제어 + Polling 기반 실시간 전파"** 구조를 채택하고 있습니다.

**좌석 점유 흐름 (AllocationService):**
```
사용자 요청 → @Lock(PESSIMISTIC_WRITE) 비관락 획득 → 상태 검증 → DB 저장
```

```java
// AllocationRepository.java:18-21
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM AllocationEntity a WHERE a.matchId = :matchId AND a.seatId = :seatId")
Optional<AllocationEntity> findByMatchIdAndSeatIdWithLock(...)
```

**실시간 상태 전파 흐름 (AllocationStatusService):**
```
@Scheduled(fixedRate = 1000) → DB에서 updatedAt > lastCheckTime 조회 → SSE 전송
```

```java
// AllocationStatusService.java:38
private LocalDateTime lastCheckTime = LocalDateTime.now().minusMinutes(10);

// AllocationStatusService.java:96-97
@Scheduled(fixedRate = 1000)
public void checkForUpdates() { ... }
```

### 1.2 식별된 문제점

| 문제 영역 | 현재 상태 | 스케일 아웃 시 문제 |
|-----------|-----------|---------------------|
| **lastCheckTime** | 인스턴스 로컬 메모리 (`LocalDateTime.now().minusMinutes(10)`) | 인스턴스마다 다른 기준 시간 → 중복 조회 또는 누락 |
| **DB Polling** | 1초마다 `findByMatchIdAndSeatIdInAndUpdatedAtAfter` 실행 | 인스턴스 N대 → DB 쿼리 N배 증가 |
| **SSE Emitter Map** | `ConcurrentHashMap<String, List<SseEmitter>>` | 인스턴스별 독립 관리 → 클라이언트가 연결한 인스턴스만 이벤트 수신 |
| **비관락** | 단일 DB 기준 락 | DB가 병목, 분산 환경에서 확장 제한 |

### 1.3 아티클과의 연결고리

LINE VOOM 팀도 **Go 채널(인메모리 통신)** 에서 시작하여 동일한 한계에 직면했습니다:

> "Go의 채널은 경량 스레드인 고루틴 간의 통신을 위해 메모리를 사용하는데, 이는 채널에 메시지를 쓴 뒤 메시지를 처리하기 전에 서버가 종료되면 메시지가 손실되는 위험성이 있습니다."

| LINE VOOM (Go 채널) | 우리 프로젝트 (Java) |
|---------------------|----------------------|
| 메모리 기반 통신 | `ConcurrentHashMap` + `lastCheckTime` |
| 서버 종료 시 메시지 손실 | 인스턴스 재시작 시 `lastCheckTime` 초기화 → 변경사항 누락 가능 |
| 분산 확장 불가 | Polling이 인스턴스별 독립 실행 |
| 재처리 메커니즘 부재 | SSE 전송 실패 시 재시도 로직 없음 |

---

## 2. Key Takeaways (핵심 논리 및 기술적 기법)

### 2.1 기술 선택의 논리적 프레임워크

LINE 팀은 세 가지 기준으로 기술을 평가했습니다:

| 평가 기준 | 설명 | 우리 프로젝트 적용 |
|-----------|------|-------------------|
| **중복 없는 동시 처리** | 같은 메시지를 여러 소비자가 중복 처리하지 않음 | 여러 인스턴스가 동일 이벤트를 한 번만 처리 |
| **재처리 가능** | 실패한 메시지를 다시 처리할 수 있음 | SSE 전송 실패 시 재시도 |
| **사내 인프라 활용** | 운영 부담 최소화 | 이미 사용 중인 Redis 활용 |

**비교 결과:**

| 기술 | 중복 없는 처리 | 재처리 | 인프라 |
|------|---------------|--------|--------|
| Redis Streams | ✓ (Consumer Group) | ✓ (Pending 메시지) | ✓ |
| RabbitMQ | ✓ | ✓ | ✗ (별도 클러스터 필요) |
| Redis Lists (LPUSH/RPOP) | ✗ | ✗ | ✓ |
| Kafka | ✓ | ✓ | ✗ (운영 복잡도 높음) |

### 2.2 Redis Streams Consumer Group의 동작 원리

```
┌─────────────────────────────────────────────────────────────┐
│                     Redis Stream                            │
│  ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐         │
│  │ E1  │ E2  │ E3  │ E4  │ E5  │ E6  │ E7  │ E8  │ ...     │
│  └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘         │
│                          │                                  │
│              Consumer Group "allocation-status"             │
│                    last_delivered_id: E5                    │
└─────────────────────────────────────────────────────────────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │Consumer 1│    │Consumer 2│    │Consumer 3│
    │ (E1, E4) │    │ (E2, E5) │    │ (E3)     │
    │ pending  │    │ pending  │    │ pending  │
    └──────────┘    └──────────┘    └──────────┘
    서버 인스턴스1    서버 인스턴스2    서버 인스턴스3
```

**핵심 메커니즘:**

1. **XADD**: 이벤트 발행 (좌석 상태 변경 시)
   ```
   XADD allocation:stream:match:1 * seatId 100 status HOLD userId 42
   ```

2. **XREADGROUP**: 그룹 내 소비자가 메시지 수신 (중복 없이 분배)
   ```
   XREADGROUP GROUP allocation-status consumer-1 BLOCK 0 STREAMS allocation:stream:match:1 >
   ```

3. **XACK**: 처리 완료 확인
   ```
   XACK allocation:stream:match:1 allocation-status 1234567890-0
   ```

4. **XPENDING + XCLAIM**: 장애 복구 (미확인 메시지 재할당)
   ```
   # 10초 이상 pending 상태인 메시지를 다른 소비자가 가져감
   XCLAIM allocation:stream:match:1 allocation-status consumer-2 10000 1234567890-0
   ```

### 2.3 Big Key 문제와 해결 (쿠폰 수집가 문제)

**문제 상황:**
- Redis 클러스터는 16,384개의 해시 슬롯을 샤드에 분배
- 단일 Stream 키는 하나의 슬롯 → 하나의 샤드에만 저장
- 예상: 60일분 저장 가능 → 실제: 4일 만에 메모리 60% 도달

**수학적 해결 (쿠폰 수집가 공식):**
```
n개 샤드 모두 95% 확률로 배정받으려면:
필요 키 개수 ≈ n × ln(n) + n × ln(-ln(1-p))

6개 샤드, 95% 확률:
6 × ln(6) + 6 × ln(-ln(0.05)) ≈ 17.34 → 18개 Stream
```

**우리 프로젝트 적용:**
- 단일 Stream `allocation:stream` 대신
- `allocation:stream:{matchId % 18}` 또는 `allocation:stream:{matchId}:{blockId % N}`으로 분산

### 2.4 운영 안정성 전략

**1) 온/오프 스위치 (Feature Flag):**
```java
// 아티클의 Central Dogma 대신 Spring Cloud Config 또는 환경변수 활용
@Value("${feature.redis-streams.enabled:false}")
private boolean redisStreamsEnabled;

public void publishEvent(AllocationEvent event) {
    if (redisStreamsEnabled) {
        redisStreamPublisher.publish(event);
    }
    // RDB 기반 fallback은 항상 동작 (Dual Write)
}
```

**2) 점진적 마이그레이션:**
```
Phase 1: Dual Write (RDB + Redis Streams), Read from RDB
Phase 2: Dual Write, Read from Redis Streams (Shadow 비교)
Phase 3: Redis Streams Only, RDB 백업
```

---

## 3. Application (프로젝트 적용 방안)

### 3.1 아키텍처 전환 다이어그램

**As-Is (현재):**
```
┌─────────────┐         ┌─────────────┐
│   Client    │◄───SSE──│  Server 1   │──┐
└─────────────┘         │ lastCheck=T1│  │
                        └──────┬──────┘  │
┌─────────────┐         ┌──────▼──────┐  │    ┌─────────────┐
│   Client    │◄───SSE──│  Server 2   │──┼───►│     RDB     │
└─────────────┘         │ lastCheck=T2│  │    │ (SELECT ... │
                        └──────┬──────┘  │    │  FOR UPDATE)│
┌─────────────┐         ┌──────▼──────┐  │    └─────────────┘
│   Client    │◄───SSE──│  Server 3   │──┘
└─────────────┘         │ lastCheck=T3│
                        └─────────────┘
                        ▲
                        │ 문제: 각 인스턴스가 독립적으로 DB Polling
                        │      lastCheckTime 불일치
```

**To-Be (개선):**
```
┌─────────────┐         ┌─────────────┐
│   Client    │◄───SSE──│  Server 1   │◄──┐
└─────────────┘         │ Consumer-1  │   │
                        └─────────────┘   │
┌─────────────┐         ┌─────────────┐   │    ┌─────────────┐
│   Client    │◄───SSE──│  Server 2   │◄──┼────│ Redis Stream│
└─────────────┘         │ Consumer-2  │   │    │ Consumer    │
                        └─────────────┘   │    │   Group     │
┌─────────────┐         ┌─────────────┐   │    └──────▲──────┘
│   Client    │◄───SSE──│  Server 3   │◄──┘           │
└─────────────┘         │ Consumer-3  │               │
                        └─────────────┘               │
                                                      │
┌─────────────────────────────────────────────────────┘
│
▼
┌──────────────────────────────────────────────────────────────┐
│                   AllocationService                           │
│  ┌────────────┐    ┌────────────┐    ┌────────────────────┐  │
│  │Redis 분산락 │───►│ 상태 변경  │───►│ XADD to Stream     │  │
│  │ (Redisson) │    │ (SET NX EX)│    │ + RDB 동기화       │  │
│  └────────────┘    └────────────┘    └────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 코드 레벨 변환 매핑

**1) 점유 로직 (AllocationService)**

| 현재 코드 | 개선 방향 |
|-----------|-----------|
| `loadAllocationWithLock()` | Redis `SET allocation:{matchId}:{seatId} {userId} NX EX 300` |
| `@Lock(PESSIMISTIC_WRITE)` | Redisson `RLock` 또는 Redis SETNX 기반 분산락 |
| `recordAllocationPort.recordAllocation()` | Redis Hash 저장 + Stream 이벤트 발행 |

```java
// 개선된 점유 로직 (의사코드)
public void allocateSeat(AllocateSeatCommand command) {
    String lockKey = "lock:seat:" + matchId + ":" + seatId;
    String allocationKey = "allocation:" + matchId + ":" + seatId;

    // 1. 분산락 획득 (Redisson)
    RLock lock = redissonClient.getLock(lockKey);
    try {
        if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
            // 2. 현재 상태 확인
            String currentHolder = redisTemplate.opsForValue().get(allocationKey);

            // 3. 점유 가능 여부 검증
            if (currentHolder == null || isExpired(currentHolder)) {
                // 4. Redis에 점유 상태 저장 (TTL 5분)
                AllocationData data = new AllocationData(userId, HOLD, Instant.now());
                redisTemplate.opsForValue().set(allocationKey, serialize(data), 5, TimeUnit.MINUTES);

                // 5. Stream에 이벤트 발행 ★핵심★
                redisTemplate.opsForStream().add(
                    "allocation:stream:" + matchId,
                    Map.of("seatId", seatId, "status", "HOLD", "userId", userId)
                );

                // 6. RDB 동기화 (비동기 또는 Dual Write)
                allocationRepository.save(allocation);
            }
        }
    } finally {
        lock.unlock();
    }
}
```

**2) 상태 전파 로직 (AllocationStatusService)**

| 현재 코드 | 개선 방향 |
|-----------|-----------|
| `@Scheduled(fixedRate = 1000)` | `StreamMessageListenerContainer` (Spring Data Redis) |
| `lastCheckTime` 인스턴스 변수 | Consumer Group의 `last-delivered-id` (Redis 관리) |
| `loadAllocationStatusesSince()` DB 쿼리 | `XREADGROUP` 명령 |

```java
// 개선된 상태 구독 로직 (의사코드)
@Component
public class AllocationStreamConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final Map<String, List<SseEmitter>> emitters; // 기존 유지

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String streamKey = message.getStream(); // allocation:stream:1
        String matchId = extractMatchId(streamKey);

        Map<String, String> body = message.getValue();
        Long seatId = Long.parseLong(body.get("seatId"));
        String status = body.get("status");

        // 1. 해당 블록 구독자들에게 SSE 전송
        String emitterKey = matchId + ":" + getBlockIdForSeat(seatId);
        List<SseEmitter> targets = emitters.get(emitterKey);

        if (targets != null) {
            AllocationStatusChanges changes = AllocationStatusChanges.of(seatId, status);
            targets.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name("changes").data(changes));
                } catch (IOException e) {
                    targets.remove(emitter);
                }
            });
        }

        // 2. 처리 완료 확인 (XACK) - Spring이 자동 처리
        // Consumer Group의 pending list에서 제거됨
    }
}
```

**3) Consumer Group 설정**

```java
@Configuration
public class RedisStreamConfig {

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>>
            streamMessageListenerContainer(RedisConnectionFactory connectionFactory) {

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
            StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofMillis(100))
                .build();

        return StreamMessageListenerContainer.create(connectionFactory, options);
    }

    @Bean
    public Subscription subscription(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            AllocationStreamConsumer consumer) {

        // Consumer Group 생성 (없으면)
        // XGROUP CREATE allocation:stream:* allocation-status $ MKSTREAM

        return container.receive(
            Consumer.from("allocation-status", "consumer-" + instanceId),
            StreamOffset.create("allocation:stream:*", ReadOffset.lastConsumed()),
            consumer
        );
    }
}
```

### 3.3 장애 대응 시나리오

**시나리오 1: SSE 전송 실패**
```
현재: IOException 발생 시 emitter 제거 후 무시 (메시지 손실)
개선: XACK 미발행 → 메시지가 pending 상태 유지 → 다른 Consumer가 XCLAIM으로 재처리
```

**시나리오 2: 서버 인스턴스 장애**
```
현재: lastCheckTime 소실 → 복구 시 최근 10분 데이터 재조회 (비효율적)
개선: Consumer Group이 last-delivered-id 관리 → 새 인스턴스가 자연스럽게 이어받음
```

**시나리오 3: Redis 장애**
```
현재: 해당 없음 (Redis 미사용)
개선: Feature Flag로 RDB Polling 모드로 즉시 전환
      - Circuit Breaker 패턴 적용 (Resilience4j)
      - Fallback: 기존 @Scheduled Polling 활성화
```

### 3.4 점진적 전환 전략 (아티클 교훈 적용)

LINE 팀의 실패 경험 (4일 만에 롤백)을 반면교사로:

```
Week 1: 인프라 준비
  - Redis Stream 생성 및 Consumer Group 설정
  - 모니터링 대시보드 구축 (XLEN, XPENDING 지표)
  - Feature Flag 시스템 구축

Week 2: Dual Write 단계
  - AllocationService에서 RDB 저장 + Redis Stream 발행
  - 기존 Polling 로직 유지 (Read from RDB)
  - 두 데이터 소스 불일치 모니터링

Week 3: Shadow Read 단계
  - AllocationStatusService에서 Redis Stream 구독 시작
  - RDB Polling 결과와 비교 검증 (Shadow 모드)
  - 불일치 발생 시 알람

Week 4: 전환 완료
  - RDB Polling 비활성화
  - Redis Stream이 Primary
  - RDB는 영속성 백업으로 비동기 동기화
```

### 3.5 모니터링 포인트 (아티클 적용)

```java
// 아티클: "전용 모니터링 API로 메시지 수, 메모리 사용량 추적"

@RestController
@RequestMapping("/internal/stream-metrics")
public class StreamMetricsController {

    @GetMapping("/pending")
    public Map<String, Long> getPendingCounts() {
        // XPENDING allocation:stream:* allocation-status
        // 각 경기별 미처리 메시지 수
    }

    @GetMapping("/length")
    public Map<String, Long> getStreamLengths() {
        // XLEN allocation:stream:*
        // Stream 길이 (메모리 사용량 지표)
    }

    @GetMapping("/consumers")
    public List<ConsumerInfo> getConsumerStatus() {
        // XINFO CONSUMERS allocation:stream:1 allocation-status
        // 각 소비자의 pending 수, idle 시간
    }
}
```

---

## 4. 요약: 아티클 → 프로젝트 매핑 테이블

| 아티클 요소 | 우리 프로젝트 대응 |
|-------------|-------------------|
| Go 채널의 메모리 기반 한계 | `lastCheckTime` 인스턴스 로컬 변수 |
| Redis Streams Consumer Group | `AllocationStatusService`의 Polling 대체 |
| XREAD BLOCK | `@Scheduled(fixedRate=1000)` 제거 |
| XACK / Pending 재처리 | SSE 전송 실패 시 자동 재시도 |
| Big Key 분산 (18개 Stream) | `allocation:stream:{matchId}` 또는 해시 기반 분산 |
| Central Dogma 온/오프 스위치 | Spring Feature Flag + RDB Fallback |
| 4일 만에 롤백 경험 | 점진적 전환 전략 + Shadow 비교 |

---

## 5. 작성 시 강조 포인트

1. **문제 인식의 유사성**: 인메모리 기반의 한계 (Go 채널 ↔ Java ConcurrentHashMap + lastCheckTime)
2. **점진적 전환**: 아티클처럼 온/오프 스위치로 롤백 가능한 구조 설계
3. **실패 경험의 가치**: LINE 팀의 4일 만에 롤백 경험 → 우리도 RDB Fallback 유지 필요성
