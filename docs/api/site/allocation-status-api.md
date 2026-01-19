---
trigger: model_decision
---

Status Stream API (SSE)
===

구간을 선택한 사용자에게 초기 좌석 배정 현황(AllocationStatus)과 실시간 변경 사항을 전송한다.

- **연결 시점**: 사용자가 구간(Block)을 클릭하는 시점에 연결.
- **초기 데이터 (init)**: 연결 직후 해당 구간의 모든 좌석 배정 상태(`AllocationStatus`) 리스트를 전송.
- **갱신 데이터 (update)**: 좌석 배정 상태 변경 시 해당 좌석의 정보만 전송.

실시간 좌석 배정 현황 스트림
---
- **Method**: GET
- **Path**: `/api/matches/{matchId}/blocks/{blockId}/seats/events`
- **Content-Type**: `text/event-stream`
- **Controller**: `AllocationStatusController`
- **Response**: 200 OK (Event Stream)

### [event: init] - 초기 전체 현황
`AllocationStatusStreamInitResponse` 구조를 따른다.

```json
{
    "status": 200,
    "data": {
        "seats": [
            { "id": 1, "status": "AVAILABLE" },
            { "id": 2, "status": "OCCUPIED" }
        ]
    }
}
```

### [event: update] - 실시간 변경 사항
`AllocationStatusStreamUpdateResponse` 구조를 따른다.

```json
{
    "status": 200,
    "data": {
        "seatId": 1,
        "status": "HOLD"
    }
}
```

좌석 배정 현황 조회 (HTTP Fallback)
---
SSE 연결 전이나 연결 실패 시 단순 조회를 위한 엔드포인트.

- **Method**: GET
- **Path**: `/api/matches/{matchId}/statusstream/blocks/{blockId}/seats`
- **Response**: 200 OK (`SuccessResponse<AllocationStatusStreamInitResponse>`)