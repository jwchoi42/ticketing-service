import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * [Local] Breakpoint 테스트
 * 
 * 목적: 시스템의 한계점(Breakpoint) 도출
 * - 특정 부하(100 VU)에서 시작하여 단계적으로 부하를 높임
 * - 어느 지점에서 응답 시간이 무너지거나 에러가 발생하는지 확인
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MATCH_ID = __ENV.MATCH_ID || '1';
const BLOCK_ID = __ENV.BLOCK_ID || '1';

export const options = {
    scenarios: {
        breakpoint_test: {
            executor: 'ramping-vus',
            startVUs: 500, // 100 VU에서 안정적으로 시작
            stages: [

                // 4단계: 1000 VU (최대 한계 테스트)
                { duration: '30s', target: 1000 },
                { duration: '1m', target: 1000 },

                // 4단계: 2000 VU (최대 한계 테스트)
                { duration: '30s', target: 2000 },
                { duration: '1m', target: 2000 },

                // 4단계: 5000 VU (최대 한계 테스트)
                { duration: '30s', target: 5000 },
                { duration: '1m', target: 5000 },

                // Ramp-down
                { duration: '30s', target: 0 },
            ],
        },
    },
    thresholds: {
        // 95%의 요청이 1초 이내여야 함 (실패 시 이 지점이 Breakpoint)
        http_req_duration: ['p(95)<1000'],
        // 에러율 1% 미만 유지
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const url = `${BASE_URL}/api/matches/${MATCH_ID}/blocks/${BLOCK_ID}/seats`;
    const res = http.get(url);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'latency < 1000ms': (r) => r.timings.duration < 1000,
    });

    sleep(1);
}
