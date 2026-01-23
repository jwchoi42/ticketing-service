# k6 부하테스트

좌석 상태 조회 API의 성능을 측정하기 위한 k6 부하테스트 스크립트입니다.

## 테스트 대상 API

```
GET /api/matches/{matchId}/blocks/{blockId}/seats
```

- 가장 트래픽이 몰릴 것으로 예상되는 API
- 현재 캐싱 없이 매번 DB 조회

## 스크립트 목록

| 스크립트 | 용도 |
|---------|------|
| `seat-status-rampup-test.js` | Ramp-Up 테스트 (한계점 도출) |
| `seat-status-load-test.js` | 시나리오별 테스트 (smoke, load, stress, spike) |

## Ramp-Up 테스트 (권장)

시스템의 한계치(Breaking Point)를 도출하기 위한 점진적 부하 테스트입니다.

### 테스트 단계

| 단계 | VUs | Duration | 누적 시간 |
|-----|-----|----------|----------|
| 1 | 10 | 1m 30s | 1m 30s |
| 2 | 20 | 1m 30s | 3m |
| 3 | 50 | 1m 30s | 4m 30s |
| 4 | 100 | 1m 30s | 6m |
| 5 | 200 | 1m 30s | 7m 30s |
| 6 | 500 | 1m 30s | 9m |
| 7 | 1000 | 1m 30s | 10m 30s |
| Ramp-down | 0 | 30s | 11m |

### 실행 방법

```bash
k6 run -e BASE_URL=http://your-server -e MATCH_ID=1 -e BLOCK_ID=1 \
  infra/k6/scripts/seat-status-rampup-test.js
```

### 측정 항목

- **동시 접속자 수 (VU)**: 시스템이 수용 가능한 최대 사용자 수
- **처리 성능 (TPS)**: 초당 처리 요청 수
- **응답 시간 (Response Time)**: p95 < 1000ms
- **실패 비율 (Error Rate)**: < 1%

## 시나리오별 테스트

| 시나리오 | VUs | Duration | 목적 |
|---------|-----|----------|------|
| Smoke | 10 | 30s | 기본 동작 확인 |
| Load | 1,000 | 2m | 일반적인 부하 |
| Stress | 2,000→4,000→6,000 | 3m | 한계점 탐색 |
| Spike | 100→10,000→100 | 1m | 티켓팅 오픈 시뮬레이션 |

### 실행 방법

```bash
k6 run -e SCENARIO=smoke infra/k6/scripts/seat-status-load-test.js
k6 run -e SCENARIO=load infra/k6/scripts/seat-status-load-test.js
k6 run -e SCENARIO=stress infra/k6/scripts/seat-status-load-test.js
k6 run -e SCENARIO=spike infra/k6/scripts/seat-status-load-test.js
```

## k6 설치

```bash
# macOS
brew install k6

# Windows (chocolatey)
choco install k6

# Docker
docker pull grafana/k6
```

## 환경변수

| 변수 | 기본값 | 설명 |
|-----|-------|------|
| BASE_URL | http://localhost:80 | API 서버 주소 |
| MATCH_ID | 1 | 테스트할 경기 ID |
| BLOCK_ID | 1 | 테스트할 블록 ID |
| SCENARIO | load | 시나리오 (seat-status-load-test.js 전용) |

## 결과 확인

- **k6 콘솔**: 실시간 메트릭 출력
- **Grafana**: `/grafana/` 에서 Spring Boot 대시보드 확인
  - HTTP 요청 수
  - 응답 시간 분포
  - DB 커넥션 풀 상태
  - JVM 메모리/GC

## 사전 준비사항

1. Match가 OPEN 상태여야 함 (allocation이 생성되어 있어야 조회 가능)
2. 테스트할 matchId, blockId 확인
3. docker-compose로 인프라 실행 중이어야 함
