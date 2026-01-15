# 자리 배정 (Allocation) 구현 작업 계획

본 문서는 dev-workflow에 따라 좌석 선점(Hold) 및 해제(Release) 기능을 구현하기 위한 상세 계획을 정의합니다.

## 1. 개요
- **목적**: 사용자가 특정 경기의 좌석을 임시로 선점(Hold)하고, 필요 시 이를 해제(Release)하는 기능을 구현합니다.
- **핵심 정책**:
    - HOLD: 5분간 유효 (MVP 1단계는 RDB 비관적 잠금으로 구현).
    - AVAILABLE: allocation 테이블에 레코드가 없거나, 상태가 AVAILABLE인 경우.
    - **동시성 제어**: MVP 단계에서는 RDB 비관적 잠금(SELECT ... FOR UPDATE)을 사용하여 중복 점유를 방지합니다.
    - **기술적 단순화**: 내부 API 및 서비스 로직은 고유 `seatId`를 사용하여 복잡도를 낮춥니다.
    - **비즈니스 가독성**: 인수 테스트 시나리오(Gherkin)는 구역 이름, 행, 열 정보를 사용하여 가독성을 유지합니다.

## 2. 세부 작업 단계 (dev-workflow 준수)

### ✅ 1단계: Feature 정의 및 고도화 (Completed)
- **대상 파일**: src/test/resources/features/allocation.feature
- **성과**: 
    - "Hold/Release" 용어 통일 ("점유/반납").
    - 구역 이름("내야-연고-1") 및 위치(행/열)를 사용한 인간 중심 시나리오 작성.
    - 대규모 구역 구조(25개 구간, 100개 좌석)로 Background 업데이트.

### ✅ 2단계: 실패하는 인수 테스트 작성 (Completed)
- **대상 파일**: 
    - src/test/java/dev/ticketing/acceptance/client/AllocationClient.java
    - src/test/java/dev/ticketing/acceptance/steps/AllocationSteps.java
- **성과**:
    - `seatId` 기반의 단순화된 API 엔드포인트(`POST /hold/{seatId}`) 구축.
    - **Bridge Logic 도입**: Step Definition에서 위치 정보를 ID로 매핑하여 호출.
    - `TestContext` 내에 `heldSeatIds` 리스트와 `matchIdMap`을 도입하여 다중 경기/좌석 시나리오 지원.

### ✅ 3단계: 내부 구성 요소 구현 (Inside-Out) (Completed)
- **성과**:
    - `AllocationService`에서 `seatId` 기반 비관적 잠금 구현 완료.
    - `AllocationController` 리팩토링 (ID 기반 엔드포인트).
    - `SeatNotFoundException` 등 예외 체계 정비.
    - `AllocationSteps`에서 API 호출 및 검증 로직 구현.

### 4단계: 테스트 통과 및 최종 검증 (In Progress)
- **할 일**:
    - Cucumber 테스트 실행하여 Green 달성 확인.
    - 로그를 통해 비관적 잠금이 정상적으로 작동하는지 확인.
    - 대규모 구역 구조에서의 조회 성능 확인.

## 3. 향후 확장 계획
- **TTL 자동 해제**: Redis TTL 또는 @Scheduled 스케줄러를 이용한 만료 처리.
- **고성능 전환**: Redis Lua Script 기반의 분산 락 도입.
