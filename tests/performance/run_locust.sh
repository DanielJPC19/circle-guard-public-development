#!/bin/bash
# Uso: bash tests/performance/run_locust.sh
# Variables de entorno opcionales (usan puertos de docker-compose.test.yml por defecto):
#   AUTH_URL=http://host:8180
#   GATEWAY_URL=http://host:8087
#   PROMOTION_URL=http://host:8088
#   FORM_URL=http://host:8086

set -e

AUTH_URL=${AUTH_URL:-"http://localhost:8180"}
GATEWAY_URL=${GATEWAY_URL:-"http://localhost:8087"}
PROMOTION_URL=${PROMOTION_URL:-"http://localhost:8088"}
FORM_URL=${FORM_URL:-"http://localhost:8086"}
export AUTH_URL GATEWAY_URL PROMOTION_URL FORM_URL

REPORT_DIR="tests/performance/reports"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
REPORT_FILE="${REPORT_DIR}/report-${TIMESTAMP}.html"
CSV_PREFIX="${REPORT_DIR}/locust-${TIMESTAMP}"

mkdir -p "$REPORT_DIR"

if ! command -v locust &>/dev/null; then
    echo "==> Installing locust..."
    pip install locust --quiet
fi

echo "==> Auth: $AUTH_URL | Gateway: $GATEWAY_URL | Promotion: $PROMOTION_URL | Form: $FORM_URL"
echo "==> Users: 50 | Spawn rate: 10/s | Duration: 120s"

locust \
    -f tests/performance/locustfile.py \
    --host="$AUTH_URL" \
    --users=50 \
    --spawn-rate=10 \
    --run-time=120s \
    --headless \
    --html="$REPORT_FILE" \
    --csv="$CSV_PREFIX" \
    --exit-code-on-error=1

STATS_CSV="${CSV_PREFIX}_stats.csv"
if [ -f "$STATS_CSV" ]; then
    AVG_MS=$(awk -F',' '/Aggregated/ {gsub(/"/, "", $6); print int($6)}' "$STATS_CSV")
    echo "==> Average response time: ${AVG_MS}ms (threshold: 2000ms)"
    if [ -n "$AVG_MS" ] && [ "$AVG_MS" -gt 2000 ]; then
        echo "==> FAIL: avg ${AVG_MS}ms exceeds 2000ms threshold"
        exit 1
    fi
    echo "==> PASS: Performance within acceptable limits"
fi

echo "==> Report: $REPORT_FILE"
