# 프로젝트 진행 상황 보고서 (Project Progress Report)

## 1. 프로젝트 개요
**Ticketing Service**는 대규모 트래픽을 처리하기 위한 티켓 예매 시스템입니다. 헥사고날 아키텍처(Hexagonal Architecture)를 기반으로 하며, Spring Boot, Java, Redis, Kafka 등의 기술 스택을 사용합니다. 현재 MVP 단계의 핵심 기능들이 구현되어 있습니다.

## 2. 도메인별 진행 상황

### 2.1. User (사용자)
- **상태**: ✅ **구현 완료 (Completed)**
- **완료된 작업**:
  - **회원가입/로그인 API**: `/sign-up`, `/log-in` 엔드포인트 구현 완료.
  - **예외 처리**: 도메인별 예외(`LoginFailureException` 등) 및 전역 핸들러(`GlobalExceptionHandler`) 체계 정립.
  - **테스트**: Cucumber 인수 테스트(`user.feature`) 통과.

### 2.2. Site (공연장/좌석)
- **상태**: ✅ **핵심 구현 완료 (Core Implemented)**
- **Allocation (좌석 배정)**:
  - **점유(Hold)/해제(Release)**: RDB 비관적 락(Pessimistic Lock)을 이용한 동시성 제어 구현 완료.
  - **테스트**: 다중 사용자 동시 요청 시나리오 검증.
- **Status (좌석 현황)**:
  - **실시간 조회**: SSE(Server-Sent Events) + RDB Polling(1초 주기) 방식의 MVP 구현 완료.
  - **이벤트**: 초기 스냅샷(`snapshot`) 및 변경분(`changes`) 스트리밍 전송 구현.
- **Hierarchy (구조)**:
  - 경기장 구역(Block, Section) 조회 로직 구현 완료.

### 2.3. Reservation (예약)
- **상태**: ✅ **구현 완료 (Implemented)**
- **완료된 작업**:
  - **예약 생성**: 선점된 좌석을 바탕으로 예약(PENDING) 생성 로직 구현.
  - **연동**: 좌석 상태를 선점(HOLD)에서 예약 중(PENDING) 상태로 연계하는 흐름 존재.

### 2.4. Payment (결제)
- **상태**: ✅ **구현 완료 (Implemented with Mock)**
- **완료된 작업**:
  - **결제 요청/완료**: 예약 건에 대한 결제 승인 프로세스 구현.
  - **PG 연동**: `TossPaymentGatewayAdapter`를 통한 외부 PG사 Mocking 구현.
  - **상태 동기화**: 결제 완료 시 예약(CONFIRMED) 및 좌석(OCCUPIED) 상태 변경 로직 구현.

### 2.5. Match (경기)
- **상태**: ✅ **기본 구현 완료 (Basic Implemented)**
- **완료된 작업**:
  - 경기 목록 조회 API 및 기본 엔티티 구조 구현.

## 3. 인프라 및 아키텍처
- **문서화 (Documentation)**:
  - 프로젝트 문서를 `docs/` 폴더로 통합 및 재편. (`api/`, `convention/`, `domain/`, `project/`)
  - `.agent`, `.claude` 설정을 통한 AI 에이전트 컨텍스트 공유 체계 구축.
- **테스트 환경**:
  - Cucumber 인수 테스트 환경 구축 및 도메인별 시나리오 작성.
  - TestContext를 활용한 상태 공유 및 Bridge Logic 패턴 적용.
- **공통 모듈**:
  - 표준 예외 처리 및 응답(`SuccessResponse`) 포맷 통일.

## 4. 향후 계획 (Next Steps)
1.  **통합 테스트 (E2E Integration)**:
    - User -> Match -> Site(Allocation) -> Reservation -> Payment 로 이어지는 전체 예매 시나리오를 통합 검증. (현재 각 단계별 유닛/인수 테스트 위주)
2.  **성능 최적화 (Optimization)**:
    - **Allocation Status**: 현재의 RDB Polling 방식을 Redis Streams 또는 Pub/Sub 기반으로 고도화하여 지연 시간 및 DB 부하 감소.
    - **Caching**: 경기 목록, 좌석 구조 등 정적 데이터에 대한 캐싱 적용.
3.  **대기열 시스템 (Queue Integration)**:
    - 대량 트래픽 유입 시 진입 제어를 위한 대기열 시스템(Redis/Kafka) 본격 도입 및 연동.
