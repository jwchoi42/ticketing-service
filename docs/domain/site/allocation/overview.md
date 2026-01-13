---
trigger: model_decision
description: allocation-overview
---

좌석 점유(Allocation) 개요
===

좌석 배정은 사용자가 특정 좌석을 선택하고 결제 전까지 임시로 선점(HOLD)하는 과정입니다. 대규모 트래픽 환경에서 동시성 제어와 실시간 상태 전파가 핵심입니다.

좌석 배정 상태 (AllocationStatus)
---

코드베이스의 AllocationStatus Enum과 일치하는 상태 정의입니다:

1.  **AVAILABLE**: 점유되지 않은 빈 좌석.
    - **핵심**: allocation 테이블에 해당 seat_id 및 match_id에 대한 레코드가 없는 경우 기본적으로 AVAILABLE 상태로 간주한다.
2.  **HOLD**: 사용자가 선택하여 임시 선점한 상태 (TTL 존재)
3.  **OCCUPIED**: 결제가 완료되어 최종 점유된 상태

### 상태 전이 규칙
- AVAILABLE -> HOLD: 좌석 점유(Hold) 요청 성공 시
- HOLD -> AVAILABLE: 점유 만료(TTL) 또는 반납(Release) 요청 시
- HOLD -> OCCUPIED: 결제(Confirm) 성공 시
- OCCUPIED -> AVAILABLE: 예약 취소 시

좌석 식별 전략 (ID vs Position)
---
시스템의 안정성과 가독성을 위해 이원화된 식별 전략을 사용합니다.

1.  **내부 시스템 (Technical-Center)**: 
    - **통신**: API 엔드포인트는 고유 `seatId`를 사용합니다. (예: `/api/matches/{matchId}/allocation/seats/{seatId}/hold`)
    - **로직**: `seatId`를 사용하여 비관적 락을 수행하고 DB 정합성을 유지합니다.
    - **장점**: 성능 최적화 및 API 단순화.

2.  **외부 노출 및 테스트 (Business-Center)**:
    - **인터페이스**: 사용자 UI 및 인수 테스트(Gherkin)는 구역 이름, 행, 열 정보를 사용합니다. (예: "내야-연고-1" 1행 1열)
    - **장점**: 비즈니스 가기독성 및 도메인 직관성 확보.

3.  **가교 역할 (Bridge Logic)**:
    - 인수 테스트의 `Step Definition` 레이어에서 인간의 언어(위치 정보)를 기계의 언어(ID)로 변환하는 매핑 로직을 수행합니다.

구현 전략
---

인프라 및 요구사항에 따라 두 가지 전략을 제공합니다.

### 1. MVP 전략 (RDB 비관적 잠금)
- **방식**: PostgreSQL `SELECT ... FOR UPDATE`를 사용하여 Hold/Release 처리
- **성공 상태**: Hold 성공 시 `200 OK`, Release 성공 시 `204 No Content` 반환.

### 2. 고가용성 전략 (Redis + Write-Behind)
- **대상**: 대규모 트래픽 및 확장성 필요 시
- **방식**: Redis Lua Script를 통한 원자적 Hold/Release 연산
