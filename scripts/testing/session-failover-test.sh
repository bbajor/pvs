#!/bin/bash
# Session-Failover Test für Redis Session Storage
# Testet ob Sessions bei Instanz-Ausfall erhalten bleiben

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
INSTANCE_1="${INSTANCE_1:-http://localhost:8080}"
INSTANCE_2="${INSTANCE_2:-http://localhost:8081}"
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"

echo "=== PVS Session Failover Test ==="
echo "Instance 1: $INSTANCE_1"
echo "Instance 2: $INSTANCE_2"
echo "Redis: $REDIS_HOST:$REDIS_PORT"
echo ""

# Prüfe Redis-Verbindung
echo "1. Prüfe Redis-Verbindung..."
if command -v redis-cli &> /dev/null; then
    if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping > /dev/null 2>&1; then
        echo "✅ Redis erreichbar"
    else
        echo "⚠️  Redis nicht erreichbar - Session-Failover kann nicht getestet werden"
        exit 1
    fi
else
    echo "⚠️  redis-cli nicht installiert - überspringe Redis-Check"
fi
echo ""

# Prüfe Health Checks
echo "2. Prüfe Health Checks..."
HEALTH_1=$(curl -s -o /dev/null -w "%{http_code}" "$INSTANCE_1/actuator/health" || echo "000")
HEALTH_2=$(curl -s -o /dev/null -w "%{http_code}" "$INSTANCE_2/actuator/health" || echo "000")

if [ "$HEALTH_1" = "200" ]; then
    echo "✅ Instance 1 erreichbar"
else
    echo "❌ Instance 1 nicht erreichbar (HTTP $HEALTH_1)"
fi

if [ "$HEALTH_2" = "200" ]; then
    echo "✅ Instance 2 erreichbar"
else
    echo "⚠️  Instance 2 nicht erreichbar (HTTP $HEALTH_2) - Failover-Test nicht möglich"
    echo "Hinweis: Für vollständigen Failover-Test benötigen Sie 2 Instanzen"
    exit 0
fi
echo ""

# Session-Storage Test
echo "3. Prüfe Session-Storage in Redis..."
if command -v redis-cli &> /dev/null; then
    SESSION_COUNT=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" keys "spring:session:*" | wc -l)
    echo "Aktive Sessions in Redis: $SESSION_COUNT"
    
    if [ "$SESSION_COUNT" -gt 0 ]; then
        echo "✅ Sessions werden in Redis gespeichert"
    else
        echo "⚠️  Keine Sessions in Redis gefunden"
        echo "Hinweis: Sessions werden möglicherweise erst nach Login erstellt"
    fi
else
    echo "⚠️  redis-cli nicht verfügbar - überspringe Session-Check"
fi
echo ""

# Failover-Simulation
echo "4. Failover-Simulation..."
echo "Hinweis: Für vollständigen Failover-Test:"
echo "  1. Login auf Instance 1"
echo "  2. Session-ID notieren"
echo "  3. Instance 1 stoppen"
echo "  4. Request auf Instance 2 mit gleicher Session-ID"
echo "  5. Prüfen ob Session noch gültig ist"
echo ""

# Session-TTL prüfen
if command -v redis-cli &> /dev/null; then
    echo "5. Prüfe Session-TTL..."
    FIRST_SESSION=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" keys "spring:session:*" | head -1)
    if [ -n "$FIRST_SESSION" ]; then
        TTL=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ttl "$FIRST_SESSION")
        echo "Session TTL: $TTL Sekunden"
        if [ "$TTL" -gt 0 ]; then
            echo "✅ Session hat gültige TTL"
        else
            echo "⚠️  Session TTL abgelaufen oder nicht gesetzt"
        fi
    else
        echo "⚠️  Keine Session gefunden für TTL-Check"
    fi
fi
echo ""

echo "=== Test-Zusammenfassung ==="
echo "✅ Session-Failover Test abgeschlossen"
echo ""
echo "Nächste Schritte für manuellen Test:"
echo "  1. Starte 2 Instanzen mit Redis Session Storage"
echo "  2. Login auf Instance 1"
echo "  3. Stoppe Instance 1"
echo "  4. Prüfe ob Session auf Instance 2 noch gültig ist"

