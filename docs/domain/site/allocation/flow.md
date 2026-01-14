---
trigger: model_decision
description: seat-allocation-flow
---

좌석 배정(Allocation) 흐름
===

개요
---
사용자는 원하는 좌석을 선점(Hold)하거나, 마음이 변했을 때 반환(Release)할 수 있습니다.

RDB 기반 MVP 흐름
---

### 1. 좌석 선점 (Hold)
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as Ticketing App
    participant DB as Database

    User->>App: 좌석 선점 요청 (userId, seatId)
    App->>DB: SELECT FOR UPDATE (상태 확인/잠금)
    alt 선점 가능 (AVAILABLE 또는 만료)
        App->>DB: 상태 'HOLD', userId 기록
        App-->>User: 선점 성공 (201)
    else 선점 불가 (타인 점유 중)
        App-->>User: 선점 실패 (409)
    end
```

### 2. 좌석 반환 (Release)
```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as Ticketing App
    participant DB as Database

    User->>App: 좌석 반환 요청 (userId, seatId)
    App->>DB: SELECT FOR UPDATE (상태 및 소유자 확인)
    alt 본인 점유 확인
        App->>DB: 상태 'AVAILABLE', userId 제거
        App-->>User: 반환 성공 (200)
    else 본인 점유 아님
        App-->>User: 반환 실패 (403/409)
    end
```

핵심 컴포넌트
---

### 1. Allocation Service
- `allocateSeat(command)`: 좌석 선점(Hold) 로직 수행
- `releaseSeat(command)`: 좌석 반환(Release) 로직 수행

### 2. API 엔드포인트

- **Hold**: `POST /api/matches/{matchId}/allocation/seats/{seatId}/hold`
- **Release**: `POST /api/matches/{matchId}/allocation/seats/{seatId}/release`
- **Complete**: `POST /api/matches/{matchId}/allocation/seats/complete`

데이터베이스 스키마
---
`allocations` 테이블은 `user_id`를 통해 현재 누가 좌석을 점유하고 있는지 추적합니다.

상태 전이
---
```
AVAILABLE <--(Hold)--> HOLD <--(Release)--> AVAILABLE
HOLD --(Complete)--> RESERVED --(Pay)--> OCCUPIED
```
