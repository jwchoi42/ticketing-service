#!/bin/bash

# Allocation Status 전략별 부하 테스트 실행 스크립트
# 사용법: ./run-all-strategies.sh [BASE_URL]

BASE_URL=${1:-"http://3.38.125.37"}
OUTPUT_DIR="docs/load-test"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
STRATEGIES=("none" "collapsing" "redis" "caffeine")
REST_SECONDS=15

echo "========================================"
echo "Allocation Status Load Test"
echo "========================================"
echo "BASE_URL: $BASE_URL"
echo "OUTPUT_DIR: $OUTPUT_DIR"
echo "TIMESTAMP: $TIMESTAMP"
echo "========================================"

# 결과 디렉토리 생성
mkdir -p "$OUTPUT_DIR"

for strategy in "${STRATEGIES[@]}"; do
    echo ""
    echo "----------------------------------------"
    echo "Testing strategy: $strategy"
    echo "----------------------------------------"

    OUTPUT_FILE="$OUTPUT_DIR/${TIMESTAMP}-${strategy}.txt"

    # 파일에는 전체 결과 저장, 터미널에는 필터링된 결과 출력
    k6 run \
        -e BASE_URL="$BASE_URL" \
        -e STRATEGY="$strategy" \
        infra/k6/scripts/allocation-status-load-test.js \
        2>&1 | tee "$OUTPUT_FILE" | grep -v -E "(^running|^load_test|level=warning)"

    echo ""
    echo "Result saved to: $OUTPUT_FILE"

    # 마지막 전략이 아니면 휴식
    if [ "$strategy" != "caffeine" ]; then
        echo "Resting for $REST_SECONDS seconds..."
        sleep $REST_SECONDS
    fi
done

echo ""
echo "========================================"
echo "All tests completed!"
echo "Results saved in: $OUTPUT_DIR"
echo "========================================"

# 요약 파일 생성
SUMMARY_FILE="$OUTPUT_DIR/${TIMESTAMP}-summary.md"
echo "# Load Test Summary - $TIMESTAMP" > "$SUMMARY_FILE"
echo "" >> "$SUMMARY_FILE"
echo "| Strategy | p(95) | p(99) | Avg | Failed |" >> "$SUMMARY_FILE"
echo "|----------|-------|-------|-----|--------|" >> "$SUMMARY_FILE"

for strategy in "${STRATEGIES[@]}"; do
    FILE="$OUTPUT_DIR/${TIMESTAMP}-${strategy}.txt"
    if [ -f "$FILE" ]; then
        P95=$(grep "p(95)=" "$FILE" | head -1 | sed "s/.*p(95)=//" | cut -d' ' -f1)
        P99=$(grep "p(99)=" "$FILE" | head -1 | sed "s/.*p(99)=//" | cut -d' ' -f1)
        AVG=$(grep "http_req_duration.............:" "$FILE" | head -1 | awk '{print $2}')
        FAILED=$(grep "http_req_failed" "$FILE" | head -1 | awk '{print $2}')
        echo "| $strategy | $P95 | $P99 | $AVG | $FAILED |" >> "$SUMMARY_FILE"
    fi
done

echo ""
echo "Summary saved to: $SUMMARY_FILE"
