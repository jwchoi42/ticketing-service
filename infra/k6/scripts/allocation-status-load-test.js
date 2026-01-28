import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * [Common] Allocation Status Load 테스트
 * 
 * 목적: 예상되는 최대 트래픽(1000 VU) 상황에서 시스템의 안정성 및 응답 속도 확인
 * - Breakpoint 테스트처럼 부하를 계속 높이는 것이 아니라, 목표 부하에서 일정 시간 유지하여 안정성 확인
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const MATCH_ID = __ENV.MATCH_ID || '1';
const BLOCK_ID = __ENV.BLOCK_ID || '1';

export const options = {
    scenarios: {
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 }, // 목표 부하까지 30초 동안 상승
                { duration: '1m', target: 10 },  // 1000 VU에서 1분간 부하 유지
                { duration: '30s', target: 0 },    // 30초 동안 종료
            ],
        },
    },
    thresholds: {
        // 95%의 요청이 1초 이내여야 함
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
        'response time < 1000ms': (r) => r.timings.duration < 1000,
    });

    sleep(1);
}
