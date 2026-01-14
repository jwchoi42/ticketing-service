---
trigger: model_decision
---

# 좌석 현황(Allocation Status) MVP 작업 계획

본 문서는 dev-workflow의 작업 순서(BDD 기반 Outside-In)에 따라 좌석 현황 실시간 조회 기능의 RDB MVP (RDB Polling + SSE) 단계를 완성하기 위한 계획을 정의합니다.

## 1. 개요
- **방식**: RDB 기반 MVP (Server-side Polling + SSE)
- **핵심 로직**:
  - 클라이언트가 SSE 엔드포인트에 연결하면 초기 데이터(init 이벤트) 전송.
  - 서버는 1초마다 DB를 폴링하여 변경된 좌석 상태를 감지.
  - 변경 사항 발생 시 연결된 모든 클라이언트에게 update 이벤트 전송.

## 2. 세부 작업 단계 (dev-workflow 준수)

### 1단계: Feature 정의 및 고도화 (Step 1)
- **대상 파일**: src/test/resources/features/allocation_status.feature
- **할 일**: 
  - 단순 HTTP 조회가 아닌, 실시간 스트림 이벤트를 검증하는 시나리오 명확화.
  - init 이벤트(초기 전체 목록)와 update 이벤트(변경 시 알림) 수신 시나리오 추가.

### 2단계: 실패하는 인수 테스트 작성 (Step 2)
- **대상 파일**: 
  - src/test/java/dev/ticketing/acceptance/client/StatusStreamClient.java
  - src/test/java/dev/ticketing/acceptance/steps/AllocationStatusSteps.java
- **할 일**:
  - WebTestClient를 사용하여 text/event-stream을 비동기적으로 구독하고 이벤트를 캡처하는 로직 구현.
  - "실시간 이벤트를 수신한다"는 검증 단계(Step) 구현.
  - 테스트 실행 시 Red(실패) 확인.

### 3단계: 내부 구성 요소 구현 (Inside-Out) (Step 3)
의존성 방향은 외부에서 내부로 흐르지만(Dependencies Flow In), 구현 순서는 도메인 독립성을 위해 내부에서 외부로(Inside-Out) 진행합니다.

1.  **Domain Model & Entity**:
    - AllocationStatus (Enum): AVAILABLE, HOLD, OCCUPIED 상태 정의 확인.
    - Allocation (도메인): 좌석 ID 및 상태를 포함하는 핵심 비즈니스 객체.
2.  **Inbound Port (UseCase)**:
    - GetAllocationStatusesUseCase: 특정 구간의 전체 좌석 현황 조회 인터페이스.
    - GetAllocationStatusStreamUseCase: SSE 스트림 요청 처리 인터페이스.
3.  **UseCase Implementation (Service)**:
    - AllocationStatusService: 
        - SseEmitter 저장소 관리 (ConcurrentHashMap).
        - @Scheduled를 이용한 DB 폴링 로직 구현.
        - 변경 사항 발생 시 SuccessResponse 규격에 맞춰 SSE 이벤트 발행.
4.  **Outbound Port**:
    - LoadAllocationStatusPort: DB에서 구간별 조회(loadAllocationStatusesByBlockId) 및 시간차 조회(loadAllocationStatusesSince) 인터페이스 정의.
5.  **Outbound Adapter**:
    - AllocationPersistenceAdapter: JPA Repository를 사용하여 실제 DB 쿼리 구현 및 도메인 모델 변환.
6.  **Inbound Adapter (Controller)**:
    - AllocationStatusController: SSE 엔드포인트 정의 및 UseCase 연결.

### 4단계: 테스트 통과 및 검증 (Step 4)
- **할 일**:
  - 2단계에서 작성한 Cucumber 테스트 실행.
  - 실제 좌석 선점(Hold) 시 연결된 SSE 스트림으로 상태 변경 이벤트가 오는지 확인하여 **Green** 달성.

## 3. 향후 확장 계획
- MVP 완성 후, 트래픽 부하 분산을 위해 Redis Status Stream (XADD/XREADGROUP) 기반의 고가용성 전략으로 전환 예정.
