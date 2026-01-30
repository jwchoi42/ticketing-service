import http from 'k6/http';
import { check } from 'k6';

/**
 * [Burst] Allocation Status Burst 테스트 - sleep 없이 최대 부하
 *
 * 목적: sleep 없이 요청을 최대한 빠르게 전송하여 Request Collapsing 효과 측정
 * - 요청이 서버에 동시에 도착할 확률 증가
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MATCH_ID = __ENV.MATCH_ID || '1';
const BLOCK_ID = __ENV.BLOCK_ID || '1';
const STRATEGY = __ENV.STRATEGY || 'collapsing';

export const options = {
    scenarios: {
        burst_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 500 },  // 빠르게 500 VU까지
                { duration: '20s', target: 500 },  // 20초간 유지
                { duration: '10s', target: 0 },    // 종료
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000', 'p(99)<5000'],
        http_req_failed: ['rate<0.05'],
    },
};

export default function () {
    const url = `${BASE_URL}/api/matches/${MATCH_ID}/blocks/${BLOCK_ID}/seats?strategy=${STRATEGY}`;
    const res = http.get(url);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });
    // sleep 없음 - 최대한 빠르게 요청 전송
}
