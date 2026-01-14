---
trigger: model_decision
description: allocation-strategy-rdb
---

RDB 기반 좌석 점유 (MVP)
===

RDB를 소스 오브 트루스(Source of Truth)로 사용하는 초기 단계의 좌석 점유/반환 로직입니다.

1. 좌석 점유 (Hold) 프로세스
---

1.  **사용자 요청**: 특정 `seatId`에 대해 `Hold`를 요청합니다.
2.  **비관적 잠금 (Pessimistic Lock)**:
    - `AllocationRepository`에서 `SELECT ... FOR UPDATE`를 통해 해당 레코드를 잠금합니다.
3.  **상태 검증**:
    - 현재 상태가 `AVAILABLE`이거나, 기존 `HOLD`가 만료되었는지 확인합니다.
4.  **점유 처리**:
    - `HOLD` 상태로 변경, `userId` 기록, `hold_expires_at` 업데이트.
5.  **트랜잭션 커밋**: 잠금 해제 및 상태 반영.

2. 좌석 반환 (Release) 프로세스
---

1.  **사용자 요청**: 본인이 선점한 `seatId`에 대해 `Release`를 요청합니다.
2.  **비관적 잠금**: `SELECT ... FOR UPDATE`로 레코드 잠금.
3.  **권한 검증**:
    - 해당 좌석이 현재 `HOLD` 상태이고, 기록된 `userId`가 요청자와 일치하는지 확인합니다.
4.  **반환 처리**:
    - 상태를 `AVAILABLE`로 변경하고 `userId`, `hold_expires_at`을 초기화합니다.
5.  **트랜잭션 커밋**: 잠금 해제 및 상태 반영.

만료 좌석 정리
---
- 별도의 배치(`@Scheduled`) 작업을 통해 주기적으로 `hold_expires_at`이 지난 `HOLD` 레코드를 `AVAILABLE`로 전환합니다.
