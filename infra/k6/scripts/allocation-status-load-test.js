import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * [Burst] Allocation Status Load 테스트 - 티켓 오픈 시나리오
 *
 * 목적: 티켓 오픈 순간처럼 동시 요청이 폭주하는 상황 시뮬레이션
 * - sleep(0.1)로 요청 간격을 줄여 동시성 증가 → Request Collapsing 효과 측정
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MATCH_ID = __ENV.MATCH_ID || '1';
const BLOCK_ID = __ENV.BLOCK_ID || '1';
const STRATEGY = __ENV.STRATEGY || 'collapsing';  // none, collapsing, redis, caffeine

export const options = {
    scenarios: {
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 1000 }, // 목표 부하까지 30초 동안 상승
                { duration: '1m', target: 1000 },  // 1000 VU에서 1분간 부하 유지
                { duration: '30s', target: 0 },    // 30초 동안 종료
            ],
        },
    },
    thresholds: {
        // 95%의 요청이 1초 이내여야 함
        http_req_duration: ['p(95)<1000', 'p(99)<5000'],
        // 에러율 1% 미만 유지
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const url = `${BASE_URL}/api/matches/${MATCH_ID}/blocks/${BLOCK_ID}/seats?strategy=${STRATEGY}`;
    const res = http.get(url);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 1000ms': (r) => r.timings.duration < 1000,
    });

    sleep(1);  // 100ms 간격 → 동시 요청 증가 → Request Collapsing 효과 측정
}
