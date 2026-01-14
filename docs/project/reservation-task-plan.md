---
trigger: model_decision
---

# 예약(Reservation) 기능 구현 계획

본 문서는 `reservation.feature`를 바탕으로 예약 기능을 구현하기 위한 작업 계획입니다.

## 1. 개요
- **목표**: 사용자가 선점(Hold)한 좌석을 확정하여 예약(Reservation) 상태로 만드는 기능 구현.
- **주요 흐름**: 좌석 선점 확인 -> 예약 생성(PENDING) -> 좌석-예약 매핑 업데이트.

## 2. 세부 작업 단계

### 1단계: Feature 분석 및 테스트 준비 (Step 1 & 2)
- **Feature**: `src/test/resources/allocations/reservation.feature` (이미 작성됨)
- **Steps**: `src/test/java/dev/ticketing/acceptance/steps/ReservationSteps.java`
  - `ReservationClient` 구현 (POST /api/reservations)
  - `점유 중인 좌석에 대해 선택을 확정하면` 구현
  - `예약이 접수되어야 한다` 검증 구현 (응답 확인)
  - `예약 상태는 "PENDING"이어야 한다` 검증
  - `배정 받은 각 좌석의 예약 번호와 생성된 예약 번호가 일치해야 한다` 검증

### 2단계: 도메인 및 포트 정의 (Step 3 - Inside)
- **Domain**:
  - `Reservation`: id, userId, matchId, status (PENDING, CONFIRMED, CANCELLED), createdAt, etc.
  - `ReservationStatus`: Enum
- **Inbound Port (UseCase)**:
  - `ReserveSeatsUseCase`: `ReserveSeatsCommand`(userId, matchId, List<seatId>) -> `Reservation`
- **Outbound Port**:
  - `RecordReservationPort`: 예약 저장
  - `LoadReservationPort`: 예약 조회
  - `UpdateAllocationPort` (Allocation 도메인의 Port 활용 혹은 협력) -> *AllocationService*를 통해 처리 권장.

### 3단계: 서비스 및 어댑터 구현 (Step 3 - Reference Implementation)
- **Service**: `ReservationService`
  - `ReserveSeatsUseCase` 구현.
  - 1. 선점된 좌석 유효성 검사 (AllocationService 협력)
  - 2. Reservation 생성 및 저장 (PENDING)
  - 3. Allocation에 reservationId 마킹 (Update)
- **Persistence Adapter**:
  - `ReservationEntity`, `ReservationRepository` (JPA)
  - `ReservationPersistenceAdapter` 구현
- **Web Adapter**:
  - `ReservationController`: POST /api/reservations

### 4단계: 통합 (Step 4)
- Cucumber 테스트 실행 및 Pass 확인.
