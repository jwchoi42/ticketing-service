---
trigger: model_decision
---

# Allocation Status Strategy

## 1. 개요
좌석 현황(Allocation Status)은 실시간성이 중요한 데이터이며, 조회 요청이 빈번하게 발생합니다.
따라서 확장성과 성능을 고려하여 초기 데이터(Snapshot) 조회와 변경 감지(Changes) 로직을 분리하는 구조를 채택합니다.

## 2. 데이터 모델 분리 전략 (Naming Convention)

### 2.1 AllocationStatusSnapShot
- **목적**: 특정 시점의 좌석 현황 전체 데이터 조회를 담당합니다.
- **구조**: 좌석 상태 목록을 포함합니다.
- **활용 시점**: API 요청 시 초기 데이터를 반환할 때.

### 2.2 AllocationStatusChanges (Stream)
- **목적**: 시간의 흐름에 따라 발생하는 좌석 상태의 변화(Delta)를 스트리밍합니다.
- **구조**: 변경된 좌석들의 목록(List)을 포함합니다. 단건이 아닌 다건 변경을 한 번의 이벤트로 전송하여 효율을 높입니다.
- **활용 시점**: SSE 연결 후 실시간 업데이트를 전송할 때.

## 3. 서비스 구조 (Single Service, Separated Interfaces)
RDB MVP 단계에서는 두 로직을 하나의 서비스 클래스(`AllocationStatusService`)에서 관리하되, 인터페이스(UseCase)를 분리하여 역할의 명확성을 유지합니다.

### 3.1 AllocationStatusService
- **통합 구현**: 아래 두 UseCase 인터페이스를 모두 구현하는 단일 서비스 빈(Bean)입니다.
- **Polling 로직**: SSE 연결 및 변경 감지(Polling) 로직을 중앙에서 관리합니다.

### 3.2 Interfaces (UseCases)
- **GetAllocationStatusSnapShotUseCase**
  - 메서드: `AllocationStatusSnapShot getAllocationStatusSnapShot(Long matchId, Long blockId)`
  - 역할: 현재 DB 상태를 조회하여 전체 스냅샷 반환.

- **SubscribeAllocationStatusChangesStreamUseCase**
  - 메서드: `SseEmitter subscribeAllocationStatusChangesStream(Long matchId, Long blockId)`
  - 역할: SSE 스트림 구독 요청 처리.

## 4. 향후 전략 (Redis 도입 시)
- Redis 도입 시에도 `AllocationStatusService` 내부 구현만 변경하거나, 필요에 따라 서비스를 분리하는 리팩토링을 수행할 수 있습니다.
- SnapShot은 Redis Key-Value 조회로, Changes는 Redis Stream Consumer로 대체될 예정입니다.
