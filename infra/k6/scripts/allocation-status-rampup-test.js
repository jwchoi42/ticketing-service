import http from 'k6/http';
import { check, sleep } from 'k6';

// 환경변수로 설정 가능
const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';
const MATCH_ID = __ENV.MATCH_ID || '1';
const BLOCK_ID = __ENV.BLOCK_ID || '1';

/**
 * Ramp-Up 부하 테스트
 *
 * 목적: 시스템의 한계치(Breaking Point) 도출
 * - 점진적으로 VU를 증가시키며 응답 시간, TPS, 에러율 관찰
 * - 10 → 20 → 50 → 100 → 200 → 500 → 1000
 *
 * 측정 항목:
 * - 동시 접속자 수 (VU)
 * - 처리 성능 (TPS)
 * - 응답 시간 (Response Time)
 * - 실패 비율 (Error Rate)
 */
export const options = {
  scenarios: {
    rampup: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        // 10 VU
        { duration: '30s', target: 10 },
        { duration: '1m', target: 10 },

        // 20 VU
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },

        // 50 VU
        { duration: '30s', target: 50 },
        { duration: '1m', target: 50 },

        // 100 VU
        { duration: '30s', target: 100 },
        { duration: '1m', target: 100 },

        // 200 VU
        { duration: '30s', target: 200 },
        { duration: '1m', target: 200 },

        // 500 VU
        { duration: '30s', target: 500 },
        { duration: '1m', target: 500 },

        // 1000 VU
        { duration: '30s', target: 1000 },
        { duration: '1m', target: 1000 },

        // Ramp-down
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 95% 요청이 1초 이내
    http_req_failed: ['rate<0.01'],    // 에러율 1% 미만
  },
};

export default function () {
  const url = `${BASE_URL}/api/matches/${MATCH_ID}/blocks/${BLOCK_ID}/seats`;
  const res = http.get(url);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 1000ms': (r) => r.timings.duration < 1000,
  });

  sleep(1);
}
