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

BASE_URL=${BASE_URL:-"http://localhost:8080"}
MATCH_ID=${MATCH_ID:-"1"}
BLOCK_ID=${BLOCK_ID:-"1"}
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
echo "TIMESTAMP: $TIMESTAMP"
echo ""

# MD 파일 헤더 작성
cat > "$MD_FILE" << 'EOF'
# 부하 테스트 결과

EOF

echo "테스트 시작: $(date)" >> "$MD_FILE"
echo "" >> "$MD_FILE"

for scenario in "${SCENARIOS[@]}"; do
    IFS=':' read -r schema strategy <<< "$scenario"
    JSON_FILE="${RESULT_DIR}/${TIMESTAMP}-${schema}-${strategy}.json"

    echo "------------------------------------------"
    echo "테스트: schema=$schema, strategy=$strategy"
    echo "------------------------------------------"

    k6 run \
        --env BASE_URL="$BASE_URL" \
        --env MATCH_ID="$MATCH_ID" \
        --env BLOCK_ID="$BLOCK_ID" \
        --env SCHEMA="$schema" \
        --env STRATEGY="$strategy" \
        --summary-export="$JSON_FILE" \
        "${SCRIPT_DIR}/allocation-status-strategy-test.js" \
        2>/dev/null

    echo "결과 저장: $JSON_FILE"

    # JSON에서 값 추출
    if [ -f "$JSON_FILE" ]; then
        VU=$(jq -r '.metrics.vus_max.max' "$JSON_FILE")
        TPS=$(jq -r '.metrics.http_reqs.rate | floor' "$JSON_FILE")
        LATENCY_AVG=$(jq -r '.metrics.http_req_duration.avg | . * 100 | floor | . / 100' "$JSON_FILE")
        LATENCY_P95=$(jq -r '.metrics.http_req_duration."p(95)" | . * 100 | floor | . / 100' "$JSON_FILE")
        ERROR_RATE=$(jq -r '.metrics.http_req_failed.value | . * 100 | . * 100 | floor | . / 100' "$JSON_FILE")

        # MD에 결과 추가
        cat >> "$MD_FILE" << EOF

## ${schema} + ${strategy}

|  | 측정치 |
| --- | --- |
| 동시 접속자 수 (VU) | ${VU} |
| 처리량 (TPS) | ${TPS} |
| 평균 응답 시간 (ms) | ${LATENCY_AVG} |
| p(95) 응답 시간 (ms) | ${LATENCY_P95} |
| 실패 비율 (%) | ${ERROR_RATE} |

EOF
    fi

    echo ""
    echo "쿨다운 대기 (30초)..."
    sleep 30
done

echo "" >> "$MD_FILE"
echo "테스트 종료: $(date)" >> "$MD_FILE"

echo "=========================================="
echo "모든 테스트 완료"
echo "결과 위치: $RESULT_DIR"
echo "요약 파일: $MD_FILE"
echo "=========================================="
