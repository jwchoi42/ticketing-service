---
trigger: model_decision
description: rdb-implementation-strategy
---

RDB-First Implementation Strategy
===

초기 개발 단계에서는 인프라 복잡도를 낮추기 위해 RDB(PostgreSQL)를 주 저장소 및 상태 관리 도구로 사용합니다.

1. 구현 원칙
---
- **구별된 구현체**: 모든 서비스는 인터페이스(Port)를 공유하되, RDB 기반 구현체는 `Rdb...` 접두어를 사용합니다. (예: `RdbAllocationService`)
- **기본 빈(Bean) 등록**: 고가용성 버전(Redis/Kafka)을 개발하기 전까지는 `Rdb...` 구현체가 `@Primary` 또는 유일한 구현체로 스프링 컨텍스트에 등록되어야 합니다.

2. 내부 전략
---
- **비관적 잠금 (Pessimistic Locking)**: `Allocation` 테이블에 대해 `SELECT FOR UPDATE`를 사용하여 동시 좌석 점유를 제어합니다.
- **상태 관리**: `HOLD`, `RESERVED`, `PAID` 등의 상태 전이를 RDB 트랜잭션 내에서 처리합니다.
- **만료 처리**: `hold_expires_at` 컬럼을 사용하고, 만료된 좌석은 조회 시 또는 주기적 배치를 통해 정리합니다.

3. 서비스별 드라이빙 가이드
---
- **Allocation**: `AllocationUseCase` -> `RdbAllocationService`
- **Stream**: `StreamUseCase` -> `RdbStreamService` (SSE 초기 진입 시 `getSeatStatuses` 호출)
