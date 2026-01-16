# Engineering Note: SSE 브로드캐스팅에서의 메시지 보장 전략

> **관련 문서**: [Redis Streams를 활용한 실시간 좌석 상태 전파 시스템](./engineering-note-redis-streams.md)

---

## 1. 문제 정의

### 1.1 SSE 브로드캐스팅의 요구사항

좌석 상태 변경 시 **모든 클라이언트**가 해당 이벤트를 받아야 합니다:

```
좌석 E1 상태 변경 발생
    │
    ├──► Client A (Server 1에 연결) → 수신 필요
    ├──► Client B (Server 2에 연결) → 수신 필요
    └──► Client C (Server 3에 연결) → 수신 필요
```

### 1.2 기술 선택의 딜레마

| 기술 | 브로드캐스트 | 메시지 저장 | 재처리 |
|------|-------------|------------|--------|
| **Redis Pub/Sub** | O | X | X |
| **Redis Streams + Consumer Group** | X (분배됨) | O | O |

**Pub/Sub**: 모든 인스턴스가 받지만, 전송 실패 시 메시지 손실
**Consumer Group**: 재처리 가능하지만, 메시지가 분배되어 일부 인스턴스만 수신

---

## 2. 해결 방안: 인스턴스별 Consumer Group

### 2.1 핵심 아이디어

각 서버 인스턴스가 **자신만의 Consumer Group**을 생성하면:
- 모든 인스턴스가 모든 메시지를 독립적으로 수신 (브로드캐스트 효과)
- 각 인스턴스가 자체 offset과 pending 목록 관리 (재처리 가능)

### 2.2 아키텍처

```
                    Redis Stream: "seat-updates:{matchId}"
                    ┌─────┬─────┬─────┬─────┬─────┐
                    │ E1  │ E2  │ E3  │ E4  │ E5  │
                    └─────┴─────┴─────┴─────┴─────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│ Consumer Group│     │ Consumer Group│     │ Consumer Group│
│ "server-1"    │     │ "server-2"    │     │ "server-3"    │
│ last_id: E5   │     │ last_id: E5   │     │ last_id: E3   │
│ pending: []   │     │ pending: []   │     │ pending: E4,E5│
└───────┬───────┘     └───────┬───────┘     └───────┬───────┘
        │                     │                     │
        ▼                     ▼                     ▼
   Server 1              Server 2              Server 3
   (E1~E5 처리 완료)      (E1~E5 처리 완료)      (E4,E5 재처리 필요)
        │                     │                     │
        ▼                     ▼                     ▼
   SSE → Clients         SSE → Clients         SSE → Clients
```

### 2.3 메시지 흐름

```
1. AllocationService: 좌석 상태 변경
       │
       ▼
2. XADD seat-updates:{matchId} * seatId 100 status HOLD
       │
       ▼
3. 각 인스턴스가 자신의 Consumer Group으로 XREADGROUP
       │
       ├── Server 1: XREADGROUP GROUP server-1 consumer-1 ...
       ├── Server 2: XREADGROUP GROUP server-2 consumer-1 ...
       └── Server 3: XREADGROUP GROUP server-3 consumer-1 ...
       │
       ▼
4. 각 인스턴스가 자신에게 연결된 클라이언트에게 SSE 전송
       │
       ▼
5. 성공 시 XACK, 실패 시 pending 상태 유지 → 재처리
```

---

## 3. 구현 상세

### 3.1 인스턴스 식별 및 Consumer Group 생성

```java
@Component
@RequiredArgsConstructor
public class StreamConsumerInitializer {

    private final StringRedisTemplate redisTemplate;

    // 인스턴스 고유 ID (환경변수 또는 랜덤 생성)
    @Value("${server.instance-id:${random.uuid}}")
    private String instanceId;

    @PostConstruct
    public void initialize() {
        String groupName = getGroupName();
        String streamKey = "seat-updates:*";

        try {
            // 인스턴스별 Consumer Group 생성
            // 0: 스트림의 처음부터 읽기, $: 새 메시지부터 읽기
            redisTemplate.opsForStream()
                .createGroup(streamKey, ReadOffset.from("0"), groupName);
            log.info("Consumer Group 생성 완료: {}", groupName);
        } catch (RedisSystemException e) {
            // BUSYGROUP: 이미 존재하는 경우 무시
            log.info("Consumer Group 이미 존재: {}", groupName);
        }
    }

    public String getGroupName() {
        return "sse-broadcaster-" + instanceId;
    }

    public String getConsumerName() {
        return "consumer-" + instanceId;
    }
}
```

### 3.2 메시지 수신 및 SSE 브로드캐스트

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatUpdateStreamConsumer
        implements StreamListener<String, MapRecord<String, String, String>> {

    private final StringRedisTemplate redisTemplate;
    private final StreamConsumerInitializer consumerInitializer;
    private final Map<String, List<SseEmitter>> emitters; // 기존 SSE 관리 Map

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String streamKey = message.getStream();
        RecordId messageId = message.getId();
        Map<String, String> body = message.getValue();

        log.debug("메시지 수신: stream={}, id={}, body={}", streamKey, messageId, body);

        try {
            // 1. 메시지 파싱
            Long matchId = Long.parseLong(body.get("matchId"));
            Long blockId = Long.parseLong(body.get("blockId"));
            Long seatId = Long.parseLong(body.get("seatId"));
            String status = body.get("status");

            // 2. 해당 블록을 구독 중인 클라이언트들에게 SSE 전송
            String emitterKey = matchId + ":" + blockId;
            List<SseEmitter> targetEmitters = emitters.get(emitterKey);

            if (targetEmitters != null && !targetEmitters.isEmpty()) {
                AllocationStatusChanges changes = AllocationStatusChanges.of(seatId, status);
                String json = objectMapper.writeValueAsString(SuccessResponse.of(changes));

                List<SseEmitter> failedEmitters = new ArrayList<>();

                for (SseEmitter emitter : targetEmitters) {
                    try {
                        emitter.send(SseEmitter.event().name("changes").data(json));
                    } catch (IOException e) {
                        log.warn("SSE 전송 실패, emitter 제거 예정", e);
                        failedEmitters.add(emitter);
                    }
                }

                // 실패한 emitter 정리
                targetEmitters.removeAll(failedEmitters);
            }

            // 3. 처리 성공 - ACK 전송
            redisTemplate.opsForStream().acknowledge(
                consumerInitializer.getGroupName(),
                message
            );
            log.debug("메시지 ACK 완료: {}", messageId);

        } catch (Exception e) {
            // ACK 하지 않음 → pending 상태 유지 → 재처리 대상
            log.error("메시지 처리 실패, 재처리 예정: id={}", messageId, e);
        }
    }
}
```

### 3.3 Pending 메시지 재처리

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingMessageReprocessor {

    private final StringRedisTemplate redisTemplate;
    private final StreamConsumerInitializer consumerInitializer;
    private final SeatUpdateStreamConsumer consumer;

    private static final Duration PENDING_TIMEOUT = Duration.ofSeconds(10);

    @Scheduled(fixedRate = 5000) // 5초마다 실행
    public void reprocessPendingMessages() {
        String groupName = consumerInitializer.getGroupName();
        String consumerName = consumerInitializer.getConsumerName();

        try {
            // 1. Pending 메시지 목록 조회
            PendingMessagesSummary summary = redisTemplate.opsForStream()
                .pending("seat-updates:*", groupName);

            if (summary.getTotalPendingMessages() == 0) {
                return;
            }

            log.info("Pending 메시지 발견: count={}", summary.getTotalPendingMessages());

            // 2. 상세 pending 메시지 조회
            PendingMessages pendingMessages = redisTemplate.opsForStream()
                .pending("seat-updates:*", groupName, Range.unbounded(), 100);

            for (PendingMessage pending : pendingMessages) {
                // 3. 타임아웃 초과한 메시지만 재처리
                if (pending.getElapsedTimeSinceLastDelivery().compareTo(PENDING_TIMEOUT) > 0) {

                    log.info("Pending 메시지 재처리: id={}, elapsed={}",
                        pending.getId(), pending.getElapsedTimeSinceLastDelivery());

                    // 4. XCLAIM으로 메시지 소유권 가져오기
                    List<MapRecord<String, String, String>> claimed = redisTemplate.opsForStream()
                        .claim("seat-updates:*", groupName, consumerName,
                            PENDING_TIMEOUT, pending.getId());

                    // 5. 재처리
                    for (MapRecord<String, String, String> message : claimed) {
                        consumer.onMessage(message);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Pending 메시지 재처리 실패", e);
        }
    }
}
```

### 3.4 좀비 Consumer Group 정리

인스턴스가 종료되면 Consumer Group이 남아있게 됩니다. 주기적으로 정리가 필요합니다.

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ZombieGroupCleaner {

    private final StringRedisTemplate redisTemplate;

    private static final Duration ZOMBIE_THRESHOLD = Duration.ofMinutes(30);

    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void cleanupZombieGroups() {
        try {
            // 1. 모든 Consumer Group 정보 조회
            // XINFO GROUPS seat-updates:*
            StreamInfo.XInfoGroups groups = redisTemplate.opsForStream()
                .groups("seat-updates:*");

            for (StreamInfo.XInfoGroup group : groups) {
                String groupName = group.groupName();

                // sse-broadcaster-* 패턴의 그룹만 대상
                if (!groupName.startsWith("sse-broadcaster-")) {
                    continue;
                }

                // 2. 그룹 내 Consumer 정보 조회
                StreamInfo.XInfoConsumers consumers = redisTemplate.opsForStream()
                    .consumers("seat-updates:*", groupName);

                boolean isZombie = true;
                for (StreamInfo.XInfoConsumer consumer : consumers) {
                    // idle 시간이 임계값 미만이면 활성 상태
                    if (consumer.idleTime().compareTo(ZOMBIE_THRESHOLD) < 0) {
                        isZombie = false;
                        break;
                    }
                }

                // 3. 좀비 그룹 삭제
                if (isZombie && group.pendingCount() == 0) {
                    log.info("좀비 Consumer Group 삭제: {}", groupName);
                    redisTemplate.opsForStream().destroyGroup("seat-updates:*", groupName);
                }
            }

        } catch (Exception e) {
            log.error("좀비 그룹 정리 실패", e);
        }
    }
}
```

### 3.5 StreamMessageListenerContainer 설정

```java
@Configuration
@RequiredArgsConstructor
public class RedisStreamConfig {

    private final StreamConsumerInitializer consumerInitializer;

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>>
            streamMessageListenerContainer(RedisConnectionFactory connectionFactory) {

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
            StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofMillis(100))
                .batchSize(10)
                .targetType(MapRecord.class)
                .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
            StreamMessageListenerContainer.create(connectionFactory, options);

        container.start();
        return container;
    }

    @Bean
    public Subscription subscription(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            SeatUpdateStreamConsumer consumer) {

        return container.receive(
            Consumer.from(
                consumerInitializer.getGroupName(),
                consumerInitializer.getConsumerName()
            ),
            StreamOffset.create("seat-updates:*", ReadOffset.lastConsumed()),
            consumer
        );
    }
}
```

---

## 4. 이벤트 발행 (AllocationService)

```java
@Service
@RequiredArgsConstructor
public class AllocationService {

    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void allocateSeat(AllocateSeatCommand command) {
        // ... 기존 점유 로직 ...

        // 상태 변경 후 이벤트 발행
        publishSeatUpdateEvent(command.getMatchId(), command.getBlockId(),
                               command.getSeatId(), AllocationStatus.HOLD);
    }

    private void publishSeatUpdateEvent(Long matchId, Long blockId, Long seatId, AllocationStatus status) {
        String streamKey = "seat-updates:" + matchId;

        Map<String, String> event = Map.of(
            "matchId", matchId.toString(),
            "blockId", blockId.toString(),
            "seatId", seatId.toString(),
            "status", status.name(),
            "timestamp", Instant.now().toString()
        );

        RecordId recordId = redisTemplate.opsForStream()
            .add(streamKey, event);

        log.debug("좌석 상태 변경 이벤트 발행: stream={}, id={}", streamKey, recordId);
    }
}
```

---

## 5. 트레이드오프

### 5.1 장점

| 항목 | 설명 |
|------|------|
| **브로드캐스트** | 모든 인스턴스가 모든 메시지 수신 |
| **메시지 보장** | ACK 기반 At-least-once delivery |
| **재처리** | Pending 메시지 자동 재처리 |
| **장애 복구** | 인스턴스 재시작 시 마지막 처리 위치부터 재개 |

### 5.2 단점 및 주의사항

| 항목 | 설명 | 대응 방안 |
|------|------|----------|
| **저장 공간** | 인스턴스 수 × 메시지 수만큼 offset 관리 | Stream MAXLEN 설정으로 오래된 메시지 자동 삭제 |
| **좀비 그룹** | 종료된 인스턴스의 Consumer Group 잔존 | 주기적 정리 스케줄러 |
| **중복 전송 가능** | 재처리 시 클라이언트가 중복 수신 가능 | 클라이언트 측 메시지 ID 기반 중복 제거 |
| **복잡도** | Pub/Sub 대비 구현 복잡 | 추상화 계층으로 관리 |

### 5.3 Stream 크기 관리

```java
// 발행 시 MAXLEN으로 자동 트리밍
RecordId recordId = redisTemplate.opsForStream()
    .add(StreamRecords.newRecord()
        .in(streamKey)
        .ofMap(event),
        StreamRecords.mapBackedRecord(event).maxlen(10000)); // 최대 10,000개 유지
```

---

## 6. 방식 비교 정리

| 방식 | 브로드캐스트 | 메시지 저장 | 재처리 | 복잡도 | 적합한 경우 |
|------|-------------|------------|--------|--------|------------|
| **Pub/Sub** | O | X | X | 낮음 | 손실 허용, 단순 알림 |
| **Streams + 단일 Consumer Group** | X | O | O | 중간 | 작업 분배 (워커) |
| **Streams + 인스턴스별 Consumer Group** | O | O | O | 높음 | SSE 브로드캐스트 + 메시지 보장 |

---

## 7. 마이그레이션 체크리스트

- [ ] 인스턴스 ID 환경변수 설정 (`SERVER_INSTANCE_ID`)
- [ ] Redis Stream 키 네이밍 규칙 정의
- [ ] Consumer Group 생성 로직 구현
- [ ] StreamMessageListenerContainer 설정
- [ ] Pending 메시지 재처리 스케줄러 구현
- [ ] 좀비 Consumer Group 정리 스케줄러 구현
- [ ] Stream MAXLEN 정책 설정
- [ ] 모니터링 메트릭 추가 (pending count, lag 등)
