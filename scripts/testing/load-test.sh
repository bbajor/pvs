#!/bin/bash
# Load-Testing Script für PVS Cloud-Deployment
# Testet die Anwendung unter Last

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
CONCURRENT_USERS="${CONCURRENT_USERS:-10}"
REQUESTS_PER_USER="${REQUESTS_PER_USER:-100}"
TEST_DURATION="${TEST_DURATION:-60s}"

echo "=== PVS Load Test ==="
echo "Base URL: $BASE_URL"
echo "Concurrent Users: $CONCURRENT_USERS"
echo "Requests per User: $REQUESTS_PER_USER"
echo "Test Duration: $TEST_DURATION"
echo ""

# Prüfe ob Apache Bench (ab) installiert ist
if ! command -v ab &> /dev/null; then
    echo "Apache Bench (ab) nicht gefunden. Installiere..."
    if command -v apt-get &> /dev/null; then
        sudo apt-get update && sudo apt-get install -y apache2-utils
    elif command -v yum &> /dev/null; then
        sudo yum install -y httpd-tools
    else
        echo "Bitte installiere Apache Bench manuell"
        exit 1
    fi
fi

# Health Check
echo "1. Health Check..."
HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health")
if [ "$HEALTH_RESPONSE" != "200" ]; then
    echo "❌ Health Check fehlgeschlagen: HTTP $HEALTH_RESPONSE"
    exit 1
fi
echo "✅ Health Check erfolgreich"
echo ""

# Load Test - Homepage
echo "2. Load Test - Homepage..."
ab -n $((CONCURRENT_USERS * REQUESTS_PER_USER)) -c $CONCURRENT_USERS \
   -H "Accept: text/html" \
   "$BASE_URL/" > /tmp/load-test-homepage.txt 2>&1 || true
cat /tmp/load-test-homepage.txt | grep -E "(Requests per second|Time per request|Failed requests)"
echo ""

# Load Test - API Endpoints
echo "3. Load Test - Actuator Endpoints..."
ab -n $((CONCURRENT_USERS * 50)) -c $CONCURRENT_USERS \
   "$BASE_URL/actuator/health" > /tmp/load-test-health.txt 2>&1 || true
cat /tmp/load-test-health.txt | grep -E "(Requests per second|Time per request|Failed requests)"
echo ""

# Session Test (wenn Login möglich)
echo "4. Session Test..."
echo "Hinweis: Für vollständigen Session-Test benötigen Sie gültige Credentials"
echo ""

# Zusammenfassung
echo "=== Test-Zusammenfassung ==="
echo "Homepage Test:"
grep "Requests per second" /tmp/load-test-homepage.txt || echo "Keine Daten"
echo ""
echo "Health Endpoint Test:"
grep "Requests per second" /tmp/load-test-health.txt || echo "Keine Daten"
echo ""
echo "✅ Load Test abgeschlossen"
echo ""
echo "Detaillierte Ergebnisse:"
echo "  - Homepage: /tmp/load-test-homepage.txt"
echo "  - Health: /tmp/load-test-health.txt"

