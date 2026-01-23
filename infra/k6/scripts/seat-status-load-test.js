import http from 'k6/http';
import { check, sleep } from 'k6';

// 환경변수로 설정 가능
const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';
const MATCH_ID = __ENV.MATCH_ID || '1';
const BLOCK_ID = __ENV.BLOCK_ID || '1';
const SCENARIO = __ENV.SCENARIO || 'load'; // smoke, load, stress, spike

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
      { duration: '10s', target: 10000 }, // 티켓팅 오픈!
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
