# 부하 테스트 계획

> 티케팅 서비스의 성능 및 안정성 검증을 위한 부하 테스트 계획

---

## 1. 테스트 대상 개요

### 1.1 시스템 현황

| 항목 | 설정 |
|------|------|
| **Database** | PostgreSQL 17 |
| **Connection Pool** | HikariCP (max: 20, min-idle: 5) |
| **동시성 제어** | Pessimistic Lock (`SELECT FOR UPDATE`) |
| **실시간 기능** | SSE + 1초 주기 DB Polling |
| **좌석 점유 TTL** | 10분 |

### 1.2 핵심 API 엔드포인트

| API | 메서드 | 특성 |
|-----|--------|------|
| `/api/matches/{matchId}/allocation/seats/{seatId}/hold` | POST | 락 경합, 동시성 핵심 |
| `/api/matches/{matchId}/allocation/seats/{seatId}/release` | POST | 락 사용 |
| `/api/matches/{matchId}/blocks/{blockId}/seats/events` | GET | SSE 연결 |
| `/api/matches/{matchId}/blocks/{blockId}/seats` | GET | 읽기 전용 |
| `/api/reservations` | POST | 트랜잭션 + 락 |
| `/api/payments/confirm` | POST | 복합 트랜잭션 (3개 도메인) |

---

## 2. 테스트 영역 및 시나리오

### 2.1 동시 좌석 점유 (최우선)

티케팅 서비스의 핵심 기능으로, 비관적 락의 성능과 정합성을 검증합니다.

#### 시나리오 A: 같은 좌석 경합 (Hot Spot)

```
목적: 동일 좌석에 대한 동시 점유 시도 시 정합성 검증
조건: 100명이 동시에 같은 좌석(seatId=1) 점유 시도
기대 결과:
  - 1명만 성공 (HTTP 200)
  - 99명은 실패 (HTTP 400, SeatAlreadyHeldException)
  - 데이터 정합성 유지
```

#### 시나리오 B: 분산 좌석 점유

```
목적: 대량 동시 요청 처리 성능 측정
조건: 1,000명이 동시에 서로 다른 좌석 점유
기대 결과:
  - 대부분 성공
  - 응답 시간 p99 < 500ms
  - DB Lock 대기 시간 최소화
```

#### 시나리오 C: 점유-해제 반복

```
목적: 상태 전이 정합성 검증
조건: 500명이 [점유 → 해제 → 재점유] 사이클 반복
기대 결과:
  - 상태 일관성 유지
  - 메모리 누수 없음
```

#### 측정 지표

| 지표 | 목표 |
|------|------|
| 응답 시간 (p50) | < 100ms |
| 응답 시간 (p95) | < 300ms |
| 응답 시간 (p99) | < 500ms |
| 에러율 | < 1% (정상 실패 제외) |
| TPS | > 500 |

---

### 2.2 SSE 대량 연결

실시간 좌석 현황 브로드캐스팅의 한계를 검증합니다.

#### 시나리오 A: 대량 연결 유지

```
목적: 동시 SSE 연결 한계 측정
조건: 1,000개 SSE 연결 동시 생성 후 10분 유지
기대 결과:
  - 모든 연결 유지
  - 메모리 사용량 선형 증가
  - CPU 사용률 안정적
```

#### 시나리오 B: 연결 상태에서 이벤트 브로드캐스트

```
목적: 이벤트 전파 성능 측정
조건:
  - 500개 SSE 연결 유지
  - 좌석 상태 변경 발생
기대 결과:
  - 모든 클라이언트가 1초 내 변경사항 수신
  - 이벤트 누락 없음
```

#### 시나리오 C: 연결 끊김 + 재연결

```
목적: 연결 정리 로직 검증
조건: 1,000개 연결 중 50%가 갑자기 끊김
기대 결과:
  - emitter 정상 제거
  - 메모리 누수 없음
  - 나머지 연결 정상 동작
```

#### 측정 지표

| 지표 | 목표 |
|------|------|
| 동시 연결 한계 | > 1,000 |
| 이벤트 전파 지연 | < 1.5초 |
| 메모리 사용량 (1,000 연결) | < 500MB 증가 |
| emitter 누수 | 0 |

---

### 2.3 DB Connection Pool

HikariCP 설정의 적정성을 검증합니다.

#### 현재 설정

```yaml
hikari:
  maximum-pool-size: 20
  minimum-idle: 5
  connection-timeout: 30000  # 30초
  idle-timeout: 600000       # 10분
  max-lifetime: 1800000      # 30분
```

#### 시나리오 A: Connection 고갈

```
목적: Pool 크기 초과 시 동작 검증
조건: 50개 동시 요청 (Pool 20개 초과)
기대 결과:
  - connection-timeout(30초) 내 처리
  - Timeout 발생 시 적절한 에러 응답
```

#### 시나리오 B: 긴 트랜잭션 + 동시 요청

```
목적: 느린 트랜잭션이 다른 요청에 미치는 영향 측정
조건:
  - 10개의 느린 트랜잭션 (5초씩) 실행 중
  - 추가 20개 요청 발생
기대 결과:
  - 추가 요청의 대기 시간 측정
  - Deadlock 발생하지 않음
```

#### 시나리오 C: Pool 크기별 성능 비교

```
목적: 최적 Pool 크기 도출
조건: pool-size를 10, 20, 30, 50으로 변경하며 동일 부하 테스트
측정: TPS, 응답 시간, Connection 대기 시간
```

#### 측정 지표

| 지표 | 목표 |
|------|------|
| Connection 획득 대기 시간 | < 1초 |
| Connection Timeout 발생률 | < 0.1% |
| Active Connection | Pool 크기의 80% 이하 |

---

### 2.4 전체 예매 플로우 (E2E)

실제 사용자 시나리오를 시뮬레이션합니다.

#### 전체 흐름

```
1. POST /api/users/log-in (로그인)
2. GET /api/matches (경기 목록)
3. GET /api/matches/{matchId}/blocks/{blockId}/seats/events (SSE 연결)
4. POST /api/matches/{matchId}/allocation/seats/{seatId}/hold (좌석 점유)
5. POST /api/reservations (예약 생성)
6. POST /api/payments/request (결제 요청)
7. POST /api/payments/confirm (결제 확인)
```

#### 시나리오 A: 일반 사용자 시뮬레이션

```
목적: 정상 플로우 성능 측정
조건: 100명이 전체 플로우 동시 실행
기대 결과:
  - 전체 플로우 완료 시간 < 10초
  - 각 단계별 응답 시간 측정
```

#### 시나리오 B: 피크 타임 시뮬레이션

```
목적: 대규모 동시 접속 안정성 검증
조건: 30초 내 1,000명 동시 접속 (Ramp-up)
기대 결과:
  - 시스템 안정성 유지
  - 에러율 < 5%
  - 응답 시간 급격한 증가 없음
```

#### 시나리오 C: 좌석 경합 + 전체 플로우

```
목적: 실제 티케팅 오픈 상황 시뮬레이션
조건:
  - 100명이 10개 좌석을 두고 경쟁
  - 실패한 사용자는 다른 좌석으로 재시도
기대 결과:
  - 10명 예매 성공
  - 나머지 90명 적절한 에러 처리
  - 재시도 로직 정상 동작
```

---

## 3. 부하 테스트 도구

### 3.1 k6 (권장)

JavaScript 기반의 현대적인 부하 테스트 도구입니다.

#### 설치

```bash
# macOS
brew install k6

# Windows
choco install k6

# Docker
docker run -i grafana/k6 run - <script.js
```

#### 좌석 점유 테스트 스크립트

```javascript
// tests/load/seat-hold.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// 커스텀 메트릭
const holdSuccess = new Counter('hold_success');
const holdFailed = new Counter('hold_failed');
const holdDuration = new Trend('hold_duration');

export const options = {
  scenarios: {
    // 시나리오 A: 같은 좌석 경합
    same_seat_competition: {
      executor: 'shared-iterations',
      vus: 100,
      iterations: 100,
      maxDuration: '30s',
      env: { SCENARIO: 'same_seat' },
    },
    // 시나리오 B: 분산 좌석 점유
    distributed_seats: {
      executor: 'constant-vus',
      vus: 100,
      duration: '1m',
      startTime: '35s',
      env: { SCENARIO: 'distributed' },
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  const scenario = __ENV.SCENARIO;
  const matchId = 1;

  let seatId;
  if (scenario === 'same_seat') {
    seatId = 1; // 모두 같은 좌석
  } else {
    seatId = Math.floor(Math.random() * 1000) + 1; // 랜덤 좌석
  }

  const startTime = Date.now();

  const res = http.post(
    `${BASE_URL}/api/matches/${matchId}/allocation/seats/${seatId}/hold`,
    null,
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'hold_seat' },
    }
  );

  holdDuration.add(Date.now() - startTime);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'status is 200 or 400': (r) => r.status === 200 || r.status === 400,
  });

  if (res.status === 200) {
    holdSuccess.add(1);
  } else {
    holdFailed.add(1);
  }

  sleep(0.1);
}
```

#### SSE 연결 테스트 스크립트

```javascript
// tests/load/sse-connection.js
import { check } from 'k6';
import sse from 'k6/x/sse';

export const options = {
  scenarios: {
    sse_connections: {
      executor: 'constant-vus',
      vus: 100,
      duration: '5m',
    },
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  const matchId = 1;
  const blockId = 1;

  const url = `${BASE_URL}/api/matches/${matchId}/blocks/${blockId}/seats/events`;

  const response = sse.open(url, function (client) {
    client.on('open', function () {
      console.log('SSE connection opened');
    });

    client.on('event', function (event) {
      check(event, {
        'received event': (e) => e.data !== undefined,
      });
    });

    client.on('error', function (e) {
      console.error('SSE error:', e.message);
    });
  });

  check(response, {
    'connection successful': (r) => r.status === 200,
  });
}
```

#### 전체 플로우 테스트 스크립트

```javascript
// tests/load/full-flow.js
import http from 'k6/http';
import { check, sleep, group } from 'k6';

export const options = {
  scenarios: {
    full_booking_flow: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },   // Ramp-up
        { duration: '1m', target: 50 },    // Steady
        { duration: '30s', target: 100 },  // Peak
        { duration: '30s', target: 0 },    // Ramp-down
      ],
    },
  },
  thresholds: {
    'group_duration{group:::01_login}': ['p(95)<500'],
    'group_duration{group:::02_get_matches}': ['p(95)<300'],
    'group_duration{group:::03_hold_seat}': ['p(95)<1000'],
    'group_duration{group:::04_create_reservation}': ['p(95)<1000'],
    'group_duration{group:::05_payment}': ['p(95)<2000'],
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  const userId = `user_${__VU}_${__ITER}`;
  let matchId, seatId, reservationId, paymentId;

  // 1. 로그인
  group('01_login', function () {
    const res = http.post(`${BASE_URL}/api/users/log-in`, JSON.stringify({
      email: 'test@example.com',
      password: 'password123',
    }), {
      headers: { 'Content-Type': 'application/json' },
    });
    check(res, { 'login success': (r) => r.status === 200 });
  });

  sleep(0.5);

  // 2. 경기 목록 조회
  group('02_get_matches', function () {
    const res = http.get(`${BASE_URL}/api/matches`);
    check(res, { 'get matches success': (r) => r.status === 200 });

    const body = JSON.parse(res.body);
    matchId = body.data.matches[0].id;
  });

  sleep(0.5);

  // 3. 좌석 점유
  group('03_hold_seat', function () {
    seatId = Math.floor(Math.random() * 1000) + 1;

    const res = http.post(
      `${BASE_URL}/api/matches/${matchId}/allocation/seats/${seatId}/hold`,
      null,
      { headers: { 'Content-Type': 'application/json' } }
    );
    check(res, { 'hold seat success': (r) => r.status === 200 });
  });

  sleep(1);

  // 4. 예약 생성
  group('04_create_reservation', function () {
    const res = http.post(`${BASE_URL}/api/reservations`, JSON.stringify({
      matchId: matchId,
      seatIds: [seatId],
    }), {
      headers: { 'Content-Type': 'application/json' },
    });

    if (res.status === 200 || res.status === 201) {
      const body = JSON.parse(res.body);
      reservationId = body.data.id;
    }
    check(res, { 'create reservation success': (r) => r.status === 200 || r.status === 201 });
  });

  sleep(1);

  // 5. 결제
  group('05_payment', function () {
    // 결제 요청
    let res = http.post(`${BASE_URL}/api/payments/request`, JSON.stringify({
      reservationId: reservationId,
      amount: 50000,
    }), {
      headers: { 'Content-Type': 'application/json' },
    });

    if (res.status === 200 || res.status === 201) {
      const body = JSON.parse(res.body);
      paymentId = body.data.id;

      // 결제 확인
      res = http.post(`${BASE_URL}/api/payments/confirm`, JSON.stringify({
        paymentId: paymentId,
      }), {
        headers: { 'Content-Type': 'application/json' },
      });
    }

    check(res, { 'payment success': (r) => r.status === 200 });
  });

  sleep(2);
}
```

#### 실행 방법

```bash
# 기본 실행
k6 run tests/load/seat-hold.js

# 결과를 JSON으로 출력
k6 run --out json=results.json tests/load/seat-hold.js

# InfluxDB로 결과 전송 (Grafana 대시보드용)
k6 run --out influxdb=http://localhost:8086/k6 tests/load/seat-hold.js
```

---

### 3.2 Gatling (대안)

Java/Scala 친화적인 부하 테스트 도구입니다.

#### build.gradle 설정

```gradle
plugins {
    id 'io.gatling.gradle' version '3.9.5'
}

dependencies {
    gatling 'io.gatling.highcharts:gatling-charts-highcharts:3.9.5'
}
```

#### 시뮬레이션 클래스

```java
// src/gatling/java/simulations/SeatAllocationSimulation.java
package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class SeatAllocationSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json");

    // 시나리오 A: 같은 좌석 경합
    ScenarioBuilder sameSeatCompetition = scenario("Same Seat Competition")
        .exec(http("hold_same_seat")
            .post("/api/matches/1/allocation/seats/1/hold")
            .check(status().in(200, 400)));

    // 시나리오 B: 분산 좌석 점유
    ScenarioBuilder distributedSeats = scenario("Distributed Seats")
        .exec(session -> session.set("seatId", (int)(Math.random() * 1000) + 1))
        .exec(http("hold_random_seat")
            .post("/api/matches/1/allocation/seats/#{seatId}/hold")
            .check(status().is(200)));

    {
        setUp(
            sameSeatCompetition.injectOpen(atOnceUsers(100)),
            distributedSeats.injectOpen(
                rampUsers(100).during(30),
                constantUsersPerSec(10).during(60)
            )
        ).protocols(httpProtocol)
         .assertions(
             global().responseTime().percentile3().lt(500),
             global().failedRequests().percent().lt(1.0)
         );
    }
}
```

#### 실행 방법

```bash
./gradlew gatlingRun
```

---

## 4. 모니터링 및 분석

### 4.1 애플리케이션 메트릭

```yaml
# application.yaml에 추가
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 4.2 주요 모니터링 지표

| 카테고리 | 지표 | 설명 |
|----------|------|------|
| **JVM** | `jvm.memory.used` | 메모리 사용량 |
| | `jvm.gc.pause` | GC 일시정지 시간 |
| **HikariCP** | `hikaricp.connections.active` | 활성 커넥션 수 |
| | `hikaricp.connections.pending` | 대기 중인 요청 수 |
| | `hikaricp.connections.timeout` | 타임아웃 발생 수 |
| **HTTP** | `http.server.requests` | 요청 수, 응답 시간 |
| **Custom** | `seat.hold.success` | 좌석 점유 성공 수 |
| | `seat.hold.failed` | 좌석 점유 실패 수 |
| | `sse.connections.active` | 활성 SSE 연결 수 |

### 4.3 Grafana 대시보드 구성

```
┌─────────────────────────────────────────────────────────────┐
│                    부하 테스트 대시보드                       │
├─────────────────────┬───────────────────────────────────────┤
│ TPS (Transactions   │ Response Time (p50, p95, p99)        │
│ Per Second)         │                                       │
├─────────────────────┼───────────────────────────────────────┤
│ Error Rate          │ Active DB Connections                 │
├─────────────────────┼───────────────────────────────────────┤
│ SSE Connections     │ JVM Memory Usage                      │
└─────────────────────┴───────────────────────────────────────┘
```

---

## 5. 테스트 실행 체크리스트

### 5.1 사전 준비

- [ ] 테스트 환경 분리 (운영 환경과 분리)
- [ ] 테스트 데이터 준비 (경기, 좌석 데이터)
- [ ] DB Connection Pool 설정 확인
- [ ] 모니터링 도구 설정 (Prometheus, Grafana)
- [ ] 로그 레벨 조정 (INFO 이상)

### 5.2 테스트 실행

- [ ] Warm-up 실행 (시스템 예열)
- [ ] 시나리오별 순차 실행
- [ ] 결과 기록 및 스크린샷

### 5.3 결과 분석

- [ ] 목표 대비 달성률 확인
- [ ] 병목 지점 식별
- [ ] 개선 방안 도출

---

## 6. 테스트 우선순위 요약

| 순위 | 테스트 영역 | 목적 | 예상 소요 |
|------|------------|------|----------|
| **1** | 동시 좌석 점유 | 핵심 기능, 락 성능 | 2시간 |
| **2** | SSE 대량 연결 | 실시간 기능 한계 | 2시간 |
| **3** | DB Connection Pool | 인프라 한계 | 1시간 |
| **4** | 전체 예매 플로우 | E2E 안정성 | 3시간 |

---

## 7. 예상 병목 지점

| 영역 | 예상 병목 | 개선 방향 |
|------|----------|----------|
| **DB Lock** | 비관적 락 경합 | Redis 분산 락 도입 |
| **Connection Pool** | 20개 제한 | Pool 크기 증가 또는 읽기 전용 복제본 |
| **SSE Polling** | 1초마다 DB 쿼리 | Redis Pub/Sub 또는 Streams 도입 |
| **단일 인스턴스** | 수직적 한계 | 수평 확장 (로드밸런서) |
