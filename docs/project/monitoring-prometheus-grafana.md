# Prometheus + Grafana 모니터링 설정

> 티케팅 서비스의 메트릭 수집 및 시각화를 위한 모니터링 구성

---

## 1. 아키텍처

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Application   │────►│   Prometheus    │────►│    Grafana      │
│   (Spring Boot) │     │   (수집/저장)    │     │   (시각화)       │
│   :8080         │     │   :9090         │     │   :3000         │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │
        ▼
  /actuator/prometheus
  (메트릭 노출)
```

---

## 2. 파일 구조

```
ticketing-service/
├── src/main/resources/
│   └── application.yaml                    # Actuator 메트릭 설정
├── infra/
│   ├── prometheus/
│   │   └── prometheus.yml                  # Prometheus 스크래핑 설정
│   └── grafana/
│       └── provisioning/
│           ├── datasources/
│           │   └── datasource.yml          # Prometheus 데이터소스 연결
│           └── dashboards/
│               ├── dashboard.yml           # 대시보드 프로비저닝 설정
│               └── ticketing-service-dashboard.json  # 기본 대시보드
└── docker-compose.yaml                     # 컨테이너 구성
```

---

## 3. 설정 상세

### 3.1 Spring Boot Actuator 설정

**application.yaml:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
```

**주요 설정:**
- `endpoints.web.exposure.include`: 노출할 엔드포인트 (health, info, metrics, prometheus)
- `metrics.tags.application`: 모든 메트릭에 애플리케이션 이름 태그 추가
- `distribution.percentiles`: HTTP 요청의 p50, p95, p99 백분위수 계산

### 3.2 Prometheus 설정

**infra/prometheus/prometheus.yml:**
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'ticketing-service'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
    static_configs:
      - targets: ['application:8080']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'ticketing-service'
```

**주요 설정:**
- `scrape_interval: 5s`: 5초마다 메트릭 수집
- `metrics_path`: Spring Boot Actuator의 Prometheus 엔드포인트

### 3.3 Grafana 프로비저닝

**infra/grafana/provisioning/datasources/datasource.yml:**
```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
```

### 3.4 Docker Compose

```yaml
prometheus:
  container_name: "prometheus"
  image: prom/prometheus:v2.47.0
  ports:
    - "9090:9090"
  volumes:
    - ./infra/prometheus/prometheus.yaml:/etc/prometheus/prometheus.yaml
    - prometheus_data:/prometheus
  command:
    - '--config.file=/etc/prometheus/prometheus.yaml'
    - '--storage.tsdb.path=/prometheus'
    - '--web.enable-lifecycle'

grafana:
  container_name: "grafana"
  image: grafana/grafana:10.1.0
  ports:
    - "3000:3000"
  environment:
    - GF_SECURITY_ADMIN_USER=admin
    - GF_SECURITY_ADMIN_PASSWORD=admin
    - GF_USERS_ALLOW_SIGN_UP=false
  volumes:
    - ./infra/grafana/provisioning:/etc/grafana/provisioning
    - grafana_data:/var/lib/grafana

volumes:
  prometheus_data:
  grafana_data:
```

---

## 4. 접속 정보

| 서비스 | URL | 인증 |
|--------|-----|------|
| **Application** | http://localhost:8080 | - |
| **Prometheus** | http://localhost:9090 | - |
| **Grafana** | http://localhost:3000 | admin / admin |
| **Metrics Endpoint** | http://localhost:8080/actuator/prometheus | - |

---

## 5. 실행 방법

### 5.1 전체 스택 실행

```bash
docker-compose up -d
```

### 5.2 개별 서비스 실행

```bash
# 데이터베이스만
docker-compose up -d database

# 모니터링만 (애플리케이션은 로컬 실행)
docker-compose up -d prometheus grafana
```

### 5.3 로그 확인

```bash
# Prometheus 로그
docker logs -f prometheus

# Grafana 로그
docker logs -f grafana
```

### 5.4 서비스 중지

```bash
docker-compose down

# 볼륨까지 삭제 (데이터 초기화)
docker-compose down -v
```

---

## 6. 기본 대시보드

Grafana 접속 후 **Dashboards** → **Ticketing Service Dashboard**

### 6.1 패널 구성

| 패널 | 메트릭 | 설명 |
|------|--------|------|
| **HTTP Request Rate** | `http_server_requests_seconds_count` | 초당 요청 수 |
| **HTTP Response Time** | `http_server_requests_seconds_bucket` | p50, p95, p99 응답 시간 |
| **JVM Heap Memory** | `jvm_memory_used_bytes` | 힙 메모리 사용량 |
| **HikariCP Connection Pool** | `hikaricp_connections_*` | DB 커넥션 풀 상태 |
| **JVM Threads** | `jvm_threads_*` | 스레드 수 |
| **JVM GC Pause Time** | `jvm_gc_pause_seconds_sum` | GC 일시정지 시간 |
| **HTTP 5xx Error Rate** | `http_server_requests_seconds_count{status=~"5.."}` | 서버 에러율 |
| **Active DB Connections** | `hikaricp_connections_active` | 활성 DB 커넥션 |
| **Application Uptime** | `process_uptime_seconds` | 가동 시간 |

### 6.2 대시보드 레이아웃

```
┌─────────────────────────┬─────────────────────────┐
│ HTTP Request Rate       │ HTTP Response Time      │
│ (stat)                  │ (timeseries)            │
├─────────────────────────┼─────────────────────────┤
│ JVM Heap Memory         │ HikariCP Connection Pool│
│ (timeseries)            │ (timeseries)            │
├─────────────────────────┼─────────────────────────┤
│ JVM Threads             │ JVM GC Pause Time       │
│ (timeseries)            │ (timeseries)            │
├────────────┬────────────┼─────────────────────────┤
│ Error Rate │ Active DB  │ Uptime                  │
│ (stat)     │ (stat)     │ (stat)                  │
└────────────┴────────────┴─────────────────────────┘
```

---

## 7. 주요 메트릭

### 7.1 HTTP 메트릭

```promql
# 초당 요청 수
rate(http_server_requests_seconds_count{application="ticketing-service"}[1m])

# 응답 시간 p95
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="ticketing-service"}[5m])) by (le, uri))

# 에러율
sum(rate(http_server_requests_seconds_count{application="ticketing-service", status=~"5.."}[5m]))
/ sum(rate(http_server_requests_seconds_count{application="ticketing-service"}[5m]))
```

### 7.2 JVM 메트릭

```promql
# 힙 메모리 사용량
jvm_memory_used_bytes{application="ticketing-service", area="heap"}

# GC 일시정지 시간
rate(jvm_gc_pause_seconds_sum{application="ticketing-service"}[1m])

# 스레드 수
jvm_threads_live_threads{application="ticketing-service"}
```

### 7.3 HikariCP 메트릭

```promql
# 활성 커넥션
hikaricp_connections_active{application="ticketing-service"}

# 대기 중인 요청
hikaricp_connections_pending{application="ticketing-service"}

# 커넥션 획득 시간
hikaricp_connections_acquire_seconds{application="ticketing-service"}
```

---

## 8. 알림 설정 (선택사항)

Grafana에서 알림을 설정하려면:

1. **Alerting** → **Alert rules** → **New alert rule**
2. 조건 설정 예시:

```yaml
# 에러율 5% 초과 시 알림
- alert: HighErrorRate
  expr: |
    sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
    / sum(rate(http_server_requests_seconds_count[5m])) > 0.05
  for: 1m
  labels:
    severity: critical
  annotations:
    summary: "High error rate detected"

# DB 커넥션 80% 이상 사용 시 알림
- alert: HighDBConnectionUsage
  expr: |
    hikaricp_connections_active
    / hikaricp_connections_max > 0.8
  for: 2m
  labels:
    severity: warning
  annotations:
    summary: "High DB connection usage"
```

---

## 9. 트러블슈팅

### 9.1 Prometheus가 메트릭을 수집하지 못할 때

```bash
# 1. 애플리케이션 메트릭 엔드포인트 확인
curl http://localhost:8080/actuator/prometheus

# 2. Prometheus 타겟 상태 확인
# http://localhost:9090/targets 접속

# 3. 네트워크 연결 확인 (컨테이너 내부에서)
docker exec prometheus wget -qO- http://application:8080/actuator/prometheus
```

### 9.2 Grafana 대시보드가 보이지 않을 때

```bash
# 1. 프로비저닝 로그 확인
docker logs grafana | grep -i provision

# 2. 파일 권한 확인
ls -la infra/grafana/provisioning/

# 3. 수동으로 데이터소스 추가
# Grafana UI → Configuration → Data Sources → Add data source
```

### 9.3 메트릭이 없을 때

```bash
# 1. Actuator 엔드포인트 목록 확인
curl http://localhost:8080/actuator

# 2. 특정 메트릭 확인
curl http://localhost:8080/actuator/metrics/http.server.requests

# 3. Prometheus 형식 메트릭 확인
curl http://localhost:8080/actuator/prometheus | grep http_server
```

---

## 10. 커스텀 메트릭 추가

### 10.1 Counter 예시 (좌석 점유 횟수)

```java
@Component
@RequiredArgsConstructor
public class AllocationMetrics {

    private final MeterRegistry meterRegistry;
    private Counter holdSuccessCounter;
    private Counter holdFailedCounter;

    @PostConstruct
    public void init() {
        holdSuccessCounter = Counter.builder("seat.hold.success")
            .description("Number of successful seat holds")
            .tag("application", "ticketing-service")
            .register(meterRegistry);

        holdFailedCounter = Counter.builder("seat.hold.failed")
            .description("Number of failed seat holds")
            .tag("application", "ticketing-service")
            .register(meterRegistry);
    }

    public void incrementHoldSuccess() {
        holdSuccessCounter.increment();
    }

    public void incrementHoldFailed() {
        holdFailedCounter.increment();
    }
}
```

### 10.2 Gauge 예시 (SSE 연결 수)

```java
@Component
public class SseMetrics {

    public SseMetrics(MeterRegistry meterRegistry,
                      AllocationStatusService statusService) {
        Gauge.builder("sse.connections.active", statusService,
                      service -> service.getActiveConnectionCount())
            .description("Number of active SSE connections")
            .tag("application", "ticketing-service")
            .register(meterRegistry);
    }
}
```

### 10.3 Timer 예시 (좌석 점유 처리 시간)

```java
@Service
@RequiredArgsConstructor
public class AllocationService {

    private final MeterRegistry meterRegistry;

    public void allocateSeat(AllocateSeatCommand command) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // 점유 로직
        } finally {
            sample.stop(Timer.builder("seat.hold.duration")
                .description("Time taken to hold a seat")
                .tag("application", "ticketing-service")
                .register(meterRegistry));
        }
    }
}
```

---

## 11. 참고 자료

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Prometheus](https://micrometer.io/docs/registry/prometheus)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
