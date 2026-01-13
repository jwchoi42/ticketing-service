---
trigger: model_decision
---

Site Allocation API
===

사용자가 특정 좌석을 점유(Hold)하거나 반환(Release)하고, 최종 선택을 완료하는 API이다.

1. 좌석 점유 (Hold)
---
예매자가 특정 좌석을 임시로 선점한다.

- **Method**: POST
- **Path**: `/api/matches/{matchId}/allocation/seats/{seatId}/hold`
- **Request Body**:
```json
{
    "userId": 1
}
```
- **Response**: 201 Created

### 선점(Hold) 규칙
1. 좌석이 `AVAILABLE` 상태이거나 기존 점유가 만료된 경우에만 성공한다.
2. 성공 시 좌석 상태는 `HOLD`가 되며, 일정 시간(기본 5분) 동안 유지된다.

2. 좌석 반환 (Release)
---
점유한 좌석을 사용자가 직접 해제하여 다른 사용자가 선택할 수 있게 한다.

- **Method**: POST
- **Path**: `/api/matches/{matchId}/allocation/seats/{seatId}/release`
- **Request Body**:
```json
{
    "userId": 1
}
```
- **Response**: 200 OK

### 반환(Release) 규칙
1. 좌석이 `HOLD` 상태이고, 요청한 `userId`가 점유한 주체와 일치해야 한다.
2. 성공 시 좌석 상태는 `AVAILABLE`로 변경된다.

3. 좌석 선택 완료 (Complete)
---
점유한 좌석들을 최종 선택 완료하여 예약 단계로 넘어간다.

- **Method**: POST
- **Path**: `/api/matches/{matchId}/allocation/seats/complete`
- **Request Body**:
```json
{
    "userId": 1,
    "seatIds": [1, 2]
}
```
- **Response**: 200 OK
```json
{
    "status": 200,
    "data": {
        "completedSeats": [
            { "seatId": 1, "status": "HOLD" },
            { "seatId": 2, "status": "HOLD" }
        ]
    }
}
```