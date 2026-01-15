---
trigger: model_decision
description: technical-implementation-guide
---

Technical Implementation Guide
===

1. 좌석 현황 확인 및 점유 (Site Service)
---

### 관련 컴포넌트 및 역할 (Phase 1: MVP - RDB + SSE)
- **Site Manager (SSE Manager)**: 
    - **SSE 방식**으로 좌석 현황 제공.
    - **Init**: 사용자가 특정 구간에 연결하면, DB에서 현재 전체 좌석 리스트를 조회하여 `init` 이벤트를 발송한다.
    - **Update**: 좌석 상태 변경 시 이벤트를 브로드캐스팅한다. (현재는 Mocking 또는 DB Poll로 구현 시작, 추후 Redis Stream 연동)
- **Site Allocation Service**:
    - 예매자로부터 좌석 점유 요청을 받음.
    - **RDB (Pessimistic Locking)**: `SELECT ... FOR UPDATE`를 통해 좌석 상태를 확인하고 선점(Hold) 처리.
    - **Terminology**: 서비스 레벨에서 `seatId`를 사용하여 데이터베이스 락 효율을 높임.

### 상세 흐름 (Progressive Site Selection)
1. **진입**: 사용자가 특정 구간(Block)에 진입하면 SSE 연결을 요청한다.
2. **Snapshot 전송**: `Site Manager`는 연결이 완료되는 즉시 DB/Cache의 최신 상태를 `init` 이벤트로 보낸다.
3. **Delta 수신**: 사용자가 좌석을 보고 있는 동안, 다른 사용자의 점유로 인해 상태가 변하면 `update` 이벤트를 통해 화면의 특정 좌석만 갱신한다.

---

2. 좌석 선택 확정 및 예약 (Reservation Service)
---
사용자가 임시 점유(HOLD)한 좌석들에 대해 최종 예약을 요청하는 단계입니다.
- **Validation**: 요청된 모든 좌석이 해당 사용자에 의해 정상적으로 HOLD 상태인지 검증합니다.
- **Confirmation**: `AllocationStatus`를 `OCCUPIED`로 변경하고 `Reservation` 엔티티를 생성합니다.

---

3. 인수 테스트 및 가독성 전략 (Test Strategy)
---
- **External Representation**: Gherkin 및 UI에서는 "구역 이름, 행, 열" 위치 정보를 사용합니다.
- **Internal Integration**: Step Definition에서 위 위치 정보를 `seatId`로 변환하여 API를 호출합니다 (Bridge Logic).
- **State Management**: `TestContext` 내의 `matchIdMap` 및 `heldSeatIds`를 통해 복잡한 시나리오 흐름을 추적합니다.
