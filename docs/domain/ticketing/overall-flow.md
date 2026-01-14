---
trigger: model_decision
description: ticketing-flow
---

입장권 예매 (Ticketing) 과정
===

![Flow Diagram](../flow.png)

1. RDB 기반 MVP 흐름 (현재 타겟)
---
초기 개발 단계에서는 인프라 복잡도를 낮추기 위해 RDB(PostgreSQL) 만을 사용하여 정합성을 보장합니다.

**핵심 특징**:
- ✅ **좌석 현황**: SSE 사용 + 서버 내부 1초 주기 DB 폴링
- ✅ **좌석 점유**: RDB 비관적 잠금 (SELECT FOR UPDATE)
- ✅ **단순 구조**: Redis, Kafka 불필요

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant StreamService as Stream Service (RDB)
    participant Scheduler as Spring Scheduler
    participant AllocationService as Allocation Service (RDB)
    participant DB as Database (PostgreSQL)

    Note over User, DB: [Phase 0: 좌석 현황 조회 - SSE 스트림]
    User->>StreamService: SSE 연결 (/seats/stream)
    StreamService->>DB: 초기 좌석 현황 조회
    StreamService-->>User: event: init (전체 좌석 리스트)

    loop 서버 내부 폴링 (1초마다)
        Scheduler->>StreamService: @Scheduled 트리거
        StreamService->>DB: SELECT WHERE updated_at > last_check_time
        alt 변경 사항 있음
            StreamService-->>User: event: update (변경된 좌석만)
        end
    end

    Note over User, DB: [Phase 1: 좌석 선택 및 임시 점유]
    User->>AllocationService: 좌석 점유 요청
    AllocationService->>DB: SELECT FOR UPDATE (좌석 상태 확인/잠금)
    alt 좌석 사용 가능
        AllocationService->>DB: 좌석 상태 "HOLD" 및 만료시간, updated_at 업데이트
        AllocationService-->>User: 점유 성공 응답
    else 좌석 점유중
        AllocationService-->>User: 점유 실패 (이미 선택된 좌석)
    end

    Note over User, DB: [Phase 2: 예약 확정]
    User->>App: 예약 확정 요청
    App->>DB: 좌석 상태 확인 (EXPIRED 여부 체크)
    App->>DB: 좌석 상태 "RESERVED" 변경 및 예약 레코드 생성
    App-->>User: 예약 성공 (결제 대기)

    Note over User, DB: [Phase 3: 결제]
    User->>App: 결제 요청
    App->>DB: 결제 정보 처리 및 예약 상태 "PAID" 변경
    App-->>User: 최종 예매 완료
```

---

2. 고가용성 설계 (추후 개선 방향)
---
트래픽 규모에 따라 Redis와 Kafka를 도입하여 가용성과 확장성을 높이는 상세 설계입니다.

**핵심 특징**:
- ✅ **좌석 현황**: SSE를 통한 실시간 서버 푸시 (폴링 불필요)
- ✅ **좌석 점유**: Redis Lua Script를 통한 원자적 연산
- ✅ **이벤트 브로드캐스팅**: Redis Stream으로 모든 클라이언트에 실시간 전달
- ⚠️ **인프라 복잡도**: Redis, Kafka, SSE 관리 필요

### [Phase 0: 좌석 현황 조회 - SSE 스트림]
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant StreamService as Stream Service (Redis)
    participant Redis as Redis Cache
    participant RedisStream as Redis Stream

    User->>StreamService: SSE 연결 (/seats/stream)
    StreamService->>Redis: HGETALL seat:status:{matchId}
    StreamService-->>User: event: init (전체 좌석 리스트)

    Note over StreamService, RedisStream: Redis Stream 구독
    StreamService->>RedisStream: XREADGROUP (Consumer Group)
    loop 실시간 이벤트 수신
        RedisStream->>StreamService: 좌석 상태 변경 이벤트
        StreamService-->>User: event: update (변경된 좌석만)
    end
```

### [Phase 1: 좌석 점유 - Redis 원자적 연산]
```mermaid
sequenceDiagram
    autonumber
    actor User
    actor OtherUsers as Other Users
    participant AllocationService as Allocation Service<br/>(site)
    participant Redis as Redis Cache
    participant Stream as Redis Stream
    participant StreamService as Stream Service<br/>(site)

    User->>AllocationService: 좌석 점유 처리 요청
    AllocationService->>Redis: 좌석 상태 확인
    AllocationService->>Redis: 점유자 좌석 정보 반영
    AllocationService-->>User: 좌석 점유 결과 응답
    AllocationService->>Stream: 좌석 상태 변경 사항 전달

    par 실시간 좌석 현황 브로드캐스트
        Stream->>StreamService: 좌석 현황 정보 확인
        StreamService-->>OtherUsers: 좌석 현황 정보 제공
        User->>AllocationService: 좌석 점유 현황 정보 전달
    end
```

### [Phase 2: 좌석 선택 확정]
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant AllocationService as Allocation Service (site)
    participant Redis as Redis Cache
    participant Kafka as Kafka
    participant MySQL as MySQL

    User->>AllocationService: 좌석 선택 확정
    AllocationService->>Redis: 점유된 좌석 확인
    AllocationService-->>User: 예약용 좌석 반환
    AllocationService->>Redis: 좌석의 상태를 예약중으로 변경
    AllocationService->>Kafka: 좌석의 상태가 예약중임을<br/>데이터베이스에 반영
    Kafka->>MySQL: 예약 상태 반영
```

### [Phase 3: 예약 결제]
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Reservation as Reservation Service
    participant PaymentService as Payment Service
    participant PaymentGateway as Payment Gateway

    User->>Reservation: 최종 결과 전달
    Reservation-->>User: 결제 정보 전달
    Reservation->>PaymentService: 결제 결과 처리 요청
    User->>PaymentGateway: 결제 요청 및 결제
    PaymentGateway-->>User: 결제 승인 결과
    PaymentService->>PaymentGateway: 결제 결과 확인 및 결제 결과 반영
    PaymentService-->>Reservation: 결제 내역 확인 및 결제 결과 반영
```

---

주요 컴포넌트 매핑
---

| 다이어그램 명칭 | 실제 패키지/서비스 |
|---|---|
| Seat Allocation Service | `site.allocation` (Allocation Service) |
| Seat Manager | `site.stream` (Stream Service) |
| Reservation | `reservation` (Reservation Service) |
| Payment Service | `payment` (Payment Service) |
