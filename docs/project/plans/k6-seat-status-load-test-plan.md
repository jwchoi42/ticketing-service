# Plan: k6 좌석 상태 조회 API 부하테스트

## 목표
프로메테우스/그라파나 모니터링 환경에서 k6를 사용하여 좌석 상태 조회 API의 성능을 측정한다.

## 테스트 대상 API
```
GET /api/matches/{matchId}/blocks/{blockId}/seats
```
- 가장 트래픽이 몰릴 것으로 예상되는 API
- 현재 캐싱 없이 매번 DB 조회

## 파일 구조
```
infra/
├── k6/
│   ├── scripts/
│   │   └── seat-status-load-test.js    # 부하테스트 스크립트
│   └── README.md                        # 실행 방법 문서
├── grafana/
├── nginx/
└── prometheus/
```

## k6 테스트 스크립트 (`seat-status-load-test.js`)

### 테스트 시나리오
| 시나리오 | VUs | Duration | 목적 |
|---------|-----|----------|------|
| Smoke | 10 | 30s | 기본 동작 확인 |
| Load | 1,000 | 2m | 일반적인 부하 |
| Stress | 2,000→4,000→6,000 | 3m | 한계점 탐색 |
| Spike | 100→10,000→100 | 1m | 티켓팅 오픈 시뮬레이션 |

### 스크립트 내용
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

// 환경변수로 설정 가능
const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';
const MATCH_ID = __ENV.MATCH_ID || '1';
const BLOCK_ID = __ENV.BLOCK_ID || '1';
const SCENARIO = __ENV.SCENARIO || 'load';  // smoke, load, stress, spike

// 시나리오별 설정
const scenarios = {
  smoke: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '10s', target: 10 },
      { duration: '20s', target: 10 },
    ],
  },
  load: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '30s', target: 1000 },
      { duration: '1m', target: 1000 },
      { duration: '30s', target: 0 },
    ],
  },
  stress: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '30s', target: 2000 },
      { duration: '1m', target: 4000 },
      { duration: '1m', target: 6000 },
      { duration: '30s', target: 0 },
    ],
  },
  spike: {
    executor: 'ramping-vus',
    startVUs: 100,
    stages: [
      { duration: '10s', target: 100 },
      { duration: '10s', target: 10000 },  // 티켓팅 오픈!
      { duration: '30s', target: 10000 },
      { duration: '10s', target: 100 },
    ],
  },
};

export const options = {
  scenarios: {
    [SCENARIO]: scenarios[SCENARIO],
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const url = `${BASE_URL}/api/matches/${MATCH_ID}/blocks/${BLOCK_ID}/seats`;
  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
```

## 실행 방법

### 1. k6 설치
```bash
# macOS
brew install k6

# Windows (chocolatey)
choco install k6

# Docker
docker pull grafana/k6
```

### 2. 테스트 실행
```bash
# 순차 실행 (권장: smoke → load → stress → spike)
k6 run -e SCENARIO=smoke infra/k6/scripts/seat-status-load-test.js
k6 run -e SCENARIO=load infra/k6/scripts/seat-status-load-test.js
k6 run -e SCENARIO=stress infra/k6/scripts/seat-status-load-test.js
k6 run -e SCENARIO=spike infra/k6/scripts/seat-status-load-test.js

# 환경변수 지정 (서버 테스트 시)
k6 run -e BASE_URL=http://your-server -e MATCH_ID=1 -e BLOCK_ID=1 -e SCENARIO=spike \
  infra/k6/scripts/seat-status-load-test.js

# Docker로 실행 (docker-compose network 연결)
docker run --rm -i --network ticketing-service_ticketing-network \
  -e BASE_URL=http://nginx -e SCENARIO=load \
  grafana/k6 run - < infra/k6/scripts/seat-status-load-test.js
```

### 3. 결과 확인
- **k6 콘솔**: 실시간 메트릭 출력
- **Grafana**: `/grafana/` 에서 Spring Boot 대시보드 확인
  - HTTP 요청 수
  - 응답 시간 분포
  - DB 커넥션 풀 상태
  - JVM 메모리/GC

## 사전 준비사항
1. Match가 OPEN 상태여야 함 (allocation이 생성되어 있어야 조회 가능)
2. 테스트할 matchId, blockId 확인

## 검증
1. k6 실행하여 부하 발생
2. Grafana에서 메트릭 모니터링
3. 병목 지점 확인 (DB 쿼리, 커넥션 풀 등)
