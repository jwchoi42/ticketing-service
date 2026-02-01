import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Allocation Status 전략별 부하 테스트
 *
 * 테스트 시나리오:
 * - schema: normalized (JOIN 쿼리), denormalized (비정규화 쿼리)
 * - strategy: none, collapsing, redis, caffeine
 *
 * 사용법:
 *   k6 run --env SCHEMA=normalized --env STRATEGY=none allocation-status-strategy-test.js
 *   k6 run --env SCHEMA=denormalized --env STRATEGY=collapsing allocation-status-strategy-test.js
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MATCH_ID = __ENV.MATCH_ID || '1';
const BLOCK_ID = __ENV.BLOCK_ID || '1';
const SCHEMA = __ENV.SCHEMA || 'denormalized';
const STRATEGY = __ENV.STRATEGY || 'collapsing';

export const options = {
    scenarios: {
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 1000 },
                { duration: '1m', target: 1000 },
                { duration: '30s', target: 0 },
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<1000', 'p(99)<3000'],
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const url = `${BASE_URL}/api/matches/${MATCH_ID}/blocks/${BLOCK_ID}/seats?strategy=${STRATEGY}&schema=${SCHEMA}`;
    const res = http.get(url);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 1000ms': (r) => r.timings.duration < 1000,
    });

    sleep(1);
}
