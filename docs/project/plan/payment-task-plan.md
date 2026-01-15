---
trigger: model_decision
---

# 결제(Payment) 기능 구현 계획

본 문서는 `payment.feature`를 바탕으로 결제 기능을 구현하기 위한 작업 계획입니다.

## 1. 개요
- **목표**: 생성된 예약에 대해 결제를 요청하고, 완료를 처리하여 예약을 확정하는 기능 구현.
- **주요 흐름**: 결제 요청(PENDING) -> 결제 완료(PAID) -> 예약 확정(CONFIRMED) -> 좌석 점유(OCCUPIED).

## 2. 세부 작업 단계

### 1단계: Feature 분석 및 테스트 준비 (Step 1 & 2)
- **Feature**: `src/test/resources/allocations/payment.feature` (이미 작성됨)
- **Steps**: `src/test/java/dev/ticketing/acceptance/steps/PaymentSteps.java`
  - `PaymentClient` 구현 (POST /api/payments, POST /api/payments/{id}/complete)
  - `예약에 대해 결제를 요청하면` 구현
  - `결제 정보가 생성되어야 한다` 등 검증
  - `결제를 완료하면` 구현

### 2단계: 도메인 및 포트 정의 (Step 3 - Inside)
- **Domain**:
  - `Payment`: id, userId, reservationId, amount, status (PENDING, PAID, FAILED, CANCELLED), createdAt
  - `PaymentStatus`: Enum
- **Inbound Port (UseCase)**:
  - `RequestPaymentUseCase`: `RequestPaymentCommand` -> `Payment`
  - `CompletePaymentUseCase`: `CompletePaymentCommand` -> `Payment`
- **Outbound Port**:
  - `RecordPaymentPort`: 결제 저장
  - `LoadPaymentPort`: 결제 조회
  - `LoadReservationPort`, `RecordReservationPort` (예약 상태 변경용)

### 3단계: 서비스 및 어댑터 구현 (Step 3 - Reference Implementation)
- **Service**: `PaymentService`
  - `RequestPaymentUseCase`: 결제 정보 생성 (PENDING)
  - `CompletePaymentUseCase`:
    - 1. Payment 상태 PAID 변경
    - 2. Reservation 상태 CONFIRMED 변경
    - 3. AllocationService를 통해 좌석 상태 OCCUPIED 변경 (Event Driven 가능하지만 MVP는 Direct Call)
- **Persistence Adapter**:
  - `PaymentEntity`, `PaymentRepository` (JPA)
  - `PaymentPersistenceAdapter` 구현
- **Gateway Adapter**:
  - `TossPaymentGatewayAdapter` (Mock 구현): 토스페이먼츠 승인 API를 모방. `paymentKey`, `orderId`, `amount` 검증 및 승인 처리 시뮬레이션.
- **Web Adapter**:
  - `PaymentController`: 결제 요청, 결제 완료 API

### 4단계: 통합 (Step 4)
- Cucumber 테스트 실행 및 Pass 확인.
