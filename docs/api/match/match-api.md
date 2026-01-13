---
trigger: model_decision
---

경기(Match) API
===

The following endpoints are prefixed with `/api/matches`, extending the Base URL and resource path.

경기(Match) 목록 조회
---
- 예매 가능한 경기 목록을 조회한다. (`ticketing-flow` 1단계)

- Method: GET
- Path: /

- Response: 200 OK
```json
{
    "status": 200,
    "data": {
        "matches": [
            {
                "id": 1,
                "stadium": "stadium name",
                "homeTeam": "home team name",
                "awayTeam": "away team name",
                "dateTime": "yyyy-MM-dd HH:mm:ss"
            }
        ]
    }
}
```