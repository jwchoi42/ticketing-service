---
trigger: model_decision
description: allocation-strategy-redis
---

Redis 기반 좌석 점유 (Write-Behind)
===

대규모 트래픽 환경에서 성능을 최적화하고 DB 부하를 줄이기 위해 Redis를 주력으로 사용하는 Write-Behind 전략을 사용합니다.

핵심 컨셉
---

1.  **좌석 점유 및 토글 (Toggle)**: Redis Lua Script를 통해 원자적으로 좌석을 점유하거나, 본인이 이미 점유한 경우 해제합니다.
2.  **좌석 선택 완료 (확정)**: DB에 최종 상태를 저장하고 Redis 캐시를 동기화합니다.

2단계 프로세스
---

### 1단계: 좌석 점유 및 해제 (Toggle)
- **Redis Lua Script**: `AVAILABLE` 상태일 때만 `HOLD`로 변경하거나, 기존 `HOLD` 상태의 점유자가 본인(`userId`)일 경우 `AVAILABLE`로 원자적으로 되돌립니다.
- **TTL 설정**: Redis Key/Field에 만료 시간을 설정하여 자동 해제를 유도합니다.
- **이벤트 발행**: Redis Status Stream으로 변경 사항을 발행하여 SSE로 실시간 전파합니다.

### 2단계: 좌석 선택 완료 (RESERVED)
- **상태 확인**: Redis에서 `HOLD` 상태가 유효한지 확인합니다.
- **DB 기록**: `allocations` 테이블에 `RESERVED` 상태로 기록합니다.
- **캐시 동기화**: Redis 상태를 업데이트하고 TTL을 관리합니다.

Redis 데이터 구조
---

### 1. Hash: 좌석 상태 저장
- **Key**: `seat:status:{matchId}`
- **Field**: `{seatId}`
- **Value**: `AllocationStatus`

### 2. Hash: 점유자 정보 저장 (Toggle용)
- **Key**: `seat:owner:{matchId}`
- **Field**: `{seatId}`
- **Value**: `{userId}`

### 3. Stream: 실시간 이벤트 (Status Stream)
- **Key**: `seat:events:{matchId}`
- **Data**: `blockId`, `seatId`, `status`

장점
---
- **원자성 보장**: 대규모 트래픽에서도 원자적인 점유/해제 연산 보장
- **성능**: DB I/O 감소 및 응답 시간 단축
- **확장성**: Redis 분산을 통한 수평 확장 가능
- **정합성**: Lua Script를 통한 원자성 보장
