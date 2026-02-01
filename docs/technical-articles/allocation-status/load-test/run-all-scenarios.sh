#!/bin/bash

# Allocation Status 전략별 부하 테스트 실행 스크립트
#
# 사용법:
#   chmod +x run-all-scenarios.sh
#   ./run-all-scenarios.sh
#
# 환경변수:
#   BASE_URL: API 서버 주소 (기본값: http://localhost:8080)
#   MATCH_ID: 경기 ID (기본값: 1)
#   BLOCK_ID: 블록 ID (기본값: 1)
#   WARMUP_ROUNDS: 웜업 라운드 수 (기본값: 1)
#   MEASURE_ROUNDS: 측정 라운드 수 (기본값: 5)
#   COOLDOWN: 쿨다운 시간(초) (기본값: 30)

BASE_URL=${BASE_URL:-"http://localhost:8080"}
MATCH_ID=${MATCH_ID:-"1"}
BLOCK_ID=${BLOCK_ID:-"1"}
WARMUP_ROUNDS=${WARMUP_ROUNDS:-1}
MEASURE_ROUNDS=${MEASURE_ROUNDS:-5}
COOLDOWN=${COOLDOWN:-30}
SCRIPT_DIR=$(dirname "$0")
RESULT_DIR="${SCRIPT_DIR}/result-summarys"
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
MD_FILE="${RESULT_DIR}/${TIMESTAMP}-summary.md"

mkdir -p "$RESULT_DIR"

SCENARIOS=(
    "normalized:none"
    "normalized:collapsing"
    "denormalized:none"
    "denormalized:collapsing"
)

echo "=========================================="
echo "Allocation Status 전략별 부하 테스트"
echo "=========================================="
echo "BASE_URL: $BASE_URL"
echo "MATCH_ID: $MATCH_ID"
echo "BLOCK_ID: $BLOCK_ID"
echo "WARMUP_ROUNDS: $WARMUP_ROUNDS"
echo "MEASURE_ROUNDS: $MEASURE_ROUNDS"
echo "COOLDOWN: ${COOLDOWN}s"
echo "TIMESTAMP: $TIMESTAMP"
echo ""

# 통계 계산 함수 (awk 사용)
# 입력: 공백으로 구분된 숫자들
# 출력: median avg min max
calc_stats() {
    echo "$@" | awk '{
        n = NF
        for (i = 1; i <= n; i++) a[i] = $i
        # 정렬 (버블 소트)
        for (i = 1; i <= n; i++) {
            for (j = i + 1; j <= n; j++) {
                if (a[i] > a[j]) {
                    t = a[i]; a[i] = a[j]; a[j] = t
                }
            }
        }
        # 중앙값 (홀수개면 가운데, 짝수개면 가운데 두 개 평균)
        if (n % 2 == 1) {
            median = a[int(n/2) + 1]
        } else {
            median = (a[n/2] + a[n/2 + 1]) / 2
        }
        # 평균
        sum = 0
        for (i = 1; i <= n; i++) sum += a[i]
        avg = sum / n
        # 최솟값, 최댓값
        min = a[1]
        max = a[n]
        printf "%.2f %.2f %.2f %.2f", median, avg, min, max
    }'
}

# MD 파일 헤더 작성
cat > "$MD_FILE" << EOF
# Allocation Status 전략별 부하 테스트 리포트

## 테스트 정보

- **테스트 ID**: ${TIMESTAMP}
- **테스트 시작**: $(date)

## 실행 조건

| 항목 | 값 |
|------|-----|
| BASE_URL | ${BASE_URL} |
| MATCH_ID | ${MATCH_ID} |
| BLOCK_ID | ${BLOCK_ID} |
| 웜업 라운드 | ${WARMUP_ROUNDS} |
| 측정 라운드 | ${MEASURE_ROUNDS} |
| 쿨다운 | ${COOLDOWN}s |

### 부하 시나리오

| 단계 | 지속 시간 | VU 수 |
|------|----------|-------|
| Ramp-up | 30s | 0 → 1000 |
| Steady | 1m | 1000 |
| Ramp-down | 30s | 1000 → 0 |

### 임계값 (Thresholds)

- \`http_req_duration\`: p(95) < 1000ms, p(99) < 3000ms
- \`http_req_failed\`: rate < 0.01 (1%)

## 테스트 결과
EOF

# 각 시나리오별 테스트 실행
for scenario in "${SCENARIOS[@]}"; do
    IFS=':' read -r schema strategy <<< "$scenario"

    echo ""
    echo "=========================================="
    echo "시나리오: ${schema} + ${strategy}"
    echo "=========================================="

    # 결과 저장용 변수
    TPS_VALUES=""
    AVG_VALUES=""
    P95_VALUES=""
    ERR_VALUES=""

    # 웜업 라운드
    echo ""
    echo "--- 웜업 라운드 ---"
    for ((r=1; r<=WARMUP_ROUNDS; r++)); do
        json_file="${RESULT_DIR}/${TIMESTAMP}-${schema}-${strategy}-rw${r}.json"
        echo "  [웜업 $r] schema=$schema, strategy=$strategy"

        k6 run \
            --env BASE_URL="$BASE_URL" \
            --env MATCH_ID="$MATCH_ID" \
            --env BLOCK_ID="$BLOCK_ID" \
            --env SCHEMA="$schema" \
            --env STRATEGY="$strategy" \
            --summary-export="$json_file" \
            "${SCRIPT_DIR}/allocation-status-strategy-test.js" \
            2>/dev/null

        echo "  쿨다운 대기 (${COOLDOWN}초)..."
        sleep $COOLDOWN
        # 웜업 결과 파일 삭제
        rm -f "$json_file"
    done

    # 측정 라운드
    echo ""
    echo "--- 측정 라운드 ---"
    for ((r=1; r<=MEASURE_ROUNDS; r++)); do
        json_file="${RESULT_DIR}/${TIMESTAMP}-${schema}-${strategy}-r${r}.json"
        echo "  [측정 $r] schema=$schema, strategy=$strategy"

        k6 run \
            --env BASE_URL="$BASE_URL" \
            --env MATCH_ID="$MATCH_ID" \
            --env BLOCK_ID="$BLOCK_ID" \
            --env SCHEMA="$schema" \
            --env STRATEGY="$strategy" \
            --summary-export="$json_file" \
            "${SCRIPT_DIR}/allocation-status-strategy-test.js" \
            2>/dev/null

        if [ -f "$json_file" ]; then
            tps=$(jq -r '.metrics.http_reqs.rate' "$json_file")
            avg=$(jq -r '.metrics.http_req_duration.avg' "$json_file")
            p95=$(jq -r '.metrics.http_req_duration."p(95)"' "$json_file")
            err=$(jq -r '.metrics.http_req_failed.value * 100' "$json_file")

            TPS_VALUES="$TPS_VALUES $tps"
            AVG_VALUES="$AVG_VALUES $avg"
            P95_VALUES="$P95_VALUES $p95"
            ERR_VALUES="$ERR_VALUES $err"

            printf "    TPS: %.0f, Avg: %.2fms, p95: %.2fms, Err: %.3f%%\n" "$tps" "$avg" "$p95" "$err"
        fi

        if [ $r -lt $MEASURE_ROUNDS ]; then
            echo "  쿨다운 대기 (${COOLDOWN}초)..."
            sleep $COOLDOWN
        fi
    done

    # 집계 계산
    read tps_med tps_avg tps_min tps_max <<< $(calc_stats $TPS_VALUES)
    read avg_med avg_avg avg_min avg_max <<< $(calc_stats $AVG_VALUES)
    read p95_med p95_avg p95_min p95_max <<< $(calc_stats $P95_VALUES)
    read err_med err_avg err_min err_max <<< $(calc_stats $ERR_VALUES)

    echo ""
    echo "--- 집계 결과 ---"
    printf "  TPS: median=%.0f, avg=%.0f, min=%.0f, max=%.0f\n" "$tps_med" "$tps_avg" "$tps_min" "$tps_max"
    printf "  Avg Latency: median=%.2fms, avg=%.2fms\n" "$avg_med" "$avg_avg"
    printf "  p95 Latency: median=%.2fms, avg=%.2fms\n" "$p95_med" "$p95_avg"

    # MD에 결과 추가
    cat >> "$MD_FILE" << EOF

### ${schema} + ${strategy}

| 지표 | 중앙값 | 평균 | 최솟값 | 최댓값 |
|------|--------|------|--------|--------|
| 처리량 (TPS) | $(printf '%.0f' $tps_med) | $(printf '%.0f' $tps_avg) | $(printf '%.0f' $tps_min) | $(printf '%.0f' $tps_max) |
| 평균 응답 시간 (ms) | $(printf '%.2f' $avg_med) | $(printf '%.2f' $avg_avg) | $(printf '%.2f' $avg_min) | $(printf '%.2f' $avg_max) |
| p95 응답 시간 (ms) | $(printf '%.2f' $p95_med) | $(printf '%.2f' $p95_avg) | $(printf '%.2f' $p95_min) | $(printf '%.2f' $p95_max) |
| 실패율 (%) | $(printf '%.3f' $err_med) | $(printf '%.3f' $err_avg) | $(printf '%.3f' $err_min) | $(printf '%.3f' $err_max) |

EOF

    # 다음 시나리오 전 쿨다운
    if [ "$scenario" != "${SCENARIOS[-1]}" ]; then
        echo ""
        echo "다음 시나리오 전 쿨다운 대기 (${COOLDOWN}초)..."
        sleep $COOLDOWN
    fi
done

echo "" >> "$MD_FILE"
echo "---" >> "$MD_FILE"
echo "" >> "$MD_FILE"
echo "**테스트 종료**: $(date)" >> "$MD_FILE"

echo ""
echo "=========================================="
echo "모든 테스트 완료"
echo "결과 위치: $RESULT_DIR"
echo "요약 파일: $MD_FILE"
echo "=========================================="
