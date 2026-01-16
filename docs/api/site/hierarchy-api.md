---
trigger: model_decision
---

Site 계층 구조 API
===

사용자는 권역(Area) -> 진영(Section) -> 구역(Block) -> 좌석(Seat) 순서로 계층적인 선택을 진행한다.

1. 권역(Area) 목록 조회
---
- **Method**: GET
- **Path**: `/api/matches/{matchId}/areas`
- **Response**: 200 OK
```json
{
    "status": 200,
    "data": {
        "areas": [
            { "id": 1, "name": "1루측" },
            { "id": 2, "name": "3루측" }
        ]
    }
}
```

2. 권역 내 진영(Section) 목록 조회
---
- **Method**: GET
- **Path**: `/api/matches/{matchId}/areas/{areaId}/sections`
- **Response**: 200 OK
```json
{
    "status": 200,
    "data": {
        "sections": [
            { "id": 1, "name": "내야석" },
            { "id": 2, "name": "외야석" }
        ]
    }
}
```

3. 진영 내 구역(Block) 목록 조회
---
- **Method**: GET
- **Path**: `/api/matches/{matchId}/sections/{sectionId}/blocks`
- **Response**: 200 OK
```json
{
    "status": 200,
    "data": {
        "blocks": [
            { "id": 1, "name": "101블록" },
            { "id": 2, "name": "102블록" }
        ]
    }
}
```

4. 구역 내 좌석 목록 조회
---
- **Method**: GET
- **Path**: `/api/matches/{matchId}/blocks/{blockId}/seats`
- **Response**: 200 OK
```json
{
    "status": 200,
    "data": {
        "seats": [
            { "id": 1, "seatNumber": "A-1" },
            { "id": 2, "seatNumber": "A-2" }
        ]
    }
}
```