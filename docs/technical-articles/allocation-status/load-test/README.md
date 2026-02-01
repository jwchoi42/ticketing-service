# Allocation Status 전략별 부하 테스트

좌석 현황 조회 API의 다양한 전략(schema + strategy) 조합을 테스트합니다.

## 사전 요구사항

```bash
# k6 설치
brew install k6          # Mac
choco install k6         # Windows

# jq 설치 (결과 파싱용)
brew install jq          # Mac
apt install jq           # Ubuntu/Debian
choco install jq         # Windows
```

## 테스트 시나리오

| Schema | Strategy | 설명 |
|--------|----------|------|
| normalized | none | JOIN 쿼리 + 캐시 없음 (기준선) |
| normalized | collapsing | JOIN 쿼리 + 요청 병합 |
| denormalized | none | 비정규화 쿼리 + 캐시 없음 |
| denormalized | collapsing | 비정규화 쿼리 + 요청 병합 |

## 단일 시나리오 실행

```bash
k6 run \
  --env SCHEMA=normalized \
  --env STRATEGY=none \
  allocation-status-strategy-test.js
```

## 전체 시나리오 실행

```bash
chmod +x run-all-scenarios.sh
./run-all-scenarios.sh
```

## 환경변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| BASE_URL | http://localhost:8080 | API 서버 주소 |
| MATCH_ID | 1 | 경기 ID |
| BLOCK_ID | 1 | 블록 ID |
| SCHEMA | denormalized | normalized / denormalized |
| STRATEGY | collapsing | none / collapsing / redis / caffeine |

## 부하 설정

- **Ramp-up**: 30초 동안 0 → 500 VU
- **Sustain**: 1분 동안 500 VU 유지
- **Ramp-down**: 30초 동안 500 → 0 VU
- **요청 간격**: 0.1초 (동시성 높임)

## 성공 기준 (Thresholds)

- p(95) < 1000ms
- p(99) < 3000ms
- 에러율 < 1%

## 결과 파일

전체 시나리오 실행 시 `result-summarys/` 디렉토리에 결과가 저장됩니다:

```
result-summarys/
├── 20260201-143052-normalized-none.json
├── 20260201-143052-normalized-collapsing.json
├── 20260201-143052-denormalized-none.json
├── 20260201-143052-denormalized-collapsing.json
└── 20260201-143052-summary.md
```

## 결과 해석

JSON 파일의 주요 지표:

| 지표 | JSON 경로 | 설명 |
|------|-----------|------|
| VU | `metrics.vus_max.max` | 동시 접속자 수 |
| TPS | `metrics.http_reqs.rate` | 초당 처리량 |
| Latency avg | `metrics.http_req_duration.avg` | 평균 응답시간 (ms) |
| Latency p(95) | `metrics.http_req_duration.p(95)` | 95% 응답시간 (ms) |
| Error Rate | `metrics.http_req_failed.value` | 실패율 (0 = 0%) |
