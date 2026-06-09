/**
 * delivery-service k6 부하 테스트
 *
 * 대상 엔드포인트:
 *   GET  /api/v1/deliveries          (목록 조회)
 *   GET  /api/v1/deliveries/:id      (단건 조회)
 *   PUT  /api/v1/deliveries/:id      (배송 수정)
 *   POST /api/v1/deliveries/:id/assign (배차)
 *
 * 실행 방법:
 *   1. k6 설치: https://k6.io/docs/getting-started/installation/
 *   2. 로컬 서비스 기동 (delivery-service:19096)
 *   3. 환경변수 설정 후 실행:
 *      k6 run --env BASE_URL=http://localhost:19096 \
 *              --env USER_ID=<uuid> \
 *              --env DELIVERY_ID=<uuid> \
 *              delivery-load-test.js
 *
 * 시나리오:
 *   - 30초 워밍업(ramp-up) → 1분 유지(steady) → 30초 종료(ramp-down)
 *   - 최대 VU: 50
 *   - 성공 기준: p(95) 응답시간 < 500ms, 에러율 < 1%
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// --- 커스텀 메트릭 ---
const errorRate = new Rate('error_rate');
const listLatency = new Trend('list_latency', true);
const detailLatency = new Trend('detail_latency', true);
const assignLatency = new Trend('assign_latency', true);

// --- 설정 ---
const BASE_URL = __ENV.BASE_URL || 'http://localhost:19096';
const USER_ID   = __ENV.USER_ID   || '00000000-0000-0000-0000-000000000001';
const DELIVERY_ID = __ENV.DELIVERY_ID || '00000000-0000-0000-0000-000000000001';

// 게이트웨이를 거치지 않고 직접 호출할 때 사용하는 모의 헤더
const HEADERS = {
    'Content-Type': 'application/json',
    'X-User-Id': USER_ID,
    'X-User-Role': 'MASTER',
};

export const options = {
    stages: [
        { duration: '30s', target: 10 },  // 워밍업
        { duration: '1m',  target: 50 },  // 부하 유지
        { duration: '30s', target: 0  },  // 종료
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500'],  // 95th percentile 500ms 이하
        'error_rate':        ['rate<0.01'],  // 에러율 1% 미만
        'list_latency':      ['p(95)<500'],
        'detail_latency':    ['p(95)<300'],
        'assign_latency':    ['p(95)<800'],
    },
};

export default function () {
    group('목록 조회', () => {
        const res = http.get(`${BASE_URL}/api/v1/deliveries?page=0&size=10`, { headers: HEADERS });
        const ok = check(res, {
            '200 OK': (r) => r.status === 200,
            'success=true': (r) => r.json('success') === true,
        });
        errorRate.add(!ok);
        listLatency.add(res.timings.duration);
    });

    sleep(0.5);

    group('단건 조회', () => {
        const res = http.get(`${BASE_URL}/api/v1/deliveries/${DELIVERY_ID}`, { headers: HEADERS });
        const ok = check(res, {
            '200 OK': (r) => r.status === 200,
        });
        errorRate.add(!ok);
        detailLatency.add(res.timings.duration);
    });

    sleep(0.5);

    group('배차 요청', () => {
        const res = http.post(`${BASE_URL}/api/v1/deliveries/${DELIVERY_ID}/assign`, null, { headers: HEADERS });
        // 배차는 이미 배정됐을 수 있으므로 409도 허용
        const ok = check(res, {
            '2xx or 409': (r) => r.status < 300 || r.status === 409,
        });
        errorRate.add(!ok);
        assignLatency.add(res.timings.duration);
    });

    sleep(1);
}
