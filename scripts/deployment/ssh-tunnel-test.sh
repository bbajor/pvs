#!/bin/bash
# SSH-Tunnel für PVS Test-Instanz
# Usage: ./ssh-tunnel-test.sh [user@server] [ssh-key-path]

set -euo pipefail

SERVER="${1:-${HETZNER_TEST_SERVER:-user@hetzner-server.example.com}}"
SSH_KEY="${2:-${HETZNER_SSH_KEY:-}}"
LOCAL_PORT="${LOCAL_PORT:-8081}"
REMOTE_PORT="${REMOTE_PORT:-8081}"
LOG_FILE="${LOG_FILE:-/tmp/ssh-tunnel-test.log}"

echo "🔗 PVS Test SSH-Tunnel Setup"
echo "============================="
echo "Server: $SERVER"
echo "Local Port: $LOCAL_PORT"
echo "Remote Port: $REMOTE_PORT"
echo ""

# Prüfe ob Port bereits belegt ist
if lsof -Pi :$LOCAL_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "⚠️  Port $LOCAL_PORT ist bereits belegt"
    echo "Bestehender Prozess:"
    lsof -Pi :$LOCAL_PORT -sTCP:LISTEN
    read -p "Prozess beenden? (j/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Jj]$ ]]; then
        PID=$(lsof -ti :$LOCAL_PORT)
        kill $PID
        sleep 2
        echo "✅ Prozess beendet"
    else
        echo "❌ Abgebrochen"
        exit 1
    fi
fi

# SSH-Key Parameter
SSH_OPTS="-o ServerAliveInterval=60 -o ServerAliveCountMax=3 -o ExitOnForwardFailure=yes -o StrictHostKeyChecking=accept-new"
if [ -n "$SSH_KEY" ]; then
    SSH_OPTS="$SSH_OPTS -i $SSH_KEY"
fi

# Prüfe SSH-Verbindung
echo "🔍 Prüfe SSH-Verbindung..."
if ! ssh $SSH_OPTS -o ConnectTimeout=5 "$SERVER" "echo 'SSH-Verbindung OK'" >/dev/null 2>&1; then
    echo "❌ SSH-Verbindung fehlgeschlagen"
    echo "Bitte prüfe:"
    echo "  - SSH-Key ist auf Server hinterlegt"
    echo "  - Server ist erreichbar"
    echo "  - Firewall erlaubt Port 22"
    exit 1
fi
echo "✅ SSH-Verbindung OK"

# Prüfe ob Test-Instanz läuft
echo "🔍 Prüfe Test-Instanz auf Server..."
if ! ssh $SSH_OPTS "$SERVER" "curl -f http://localhost:$REMOTE_PORT/actuator/health >/dev/null 2>&1"; then
    echo "⚠️  Test-Instanz scheint nicht zu laufen"
    echo "Bitte starte die Test-Instanz auf dem Server:"
    echo "  podman-compose -f podman-compose.production.yml --profile test up -d"
    read -p "Trotzdem fortfahren? (j/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Jj]$ ]]; then
        exit 1
    fi
else
    echo "✅ Test-Instanz läuft"
fi

# SSH-Tunnel starten
echo ""
echo "🚀 Starte SSH-Tunnel..."
echo "Log-Datei: $LOG_FILE"
echo ""
echo "Tunnel läuft im Hintergrund."
echo "Zum Beenden: pkill -f 'ssh.*$LOCAL_PORT'"
echo ""

# Tunnel im Hintergrund starten
ssh -L $LOCAL_PORT:localhost:$REMOTE_PORT -N -f $SSH_OPTS "$SERVER" \
    > "$LOG_FILE" 2>&1

# Warten bis Tunnel etabliert ist
sleep 2

# Tunnel-Verifikation
if lsof -Pi :$LOCAL_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "✅ SSH-Tunnel erfolgreich erstellt"
    echo ""
    echo "📊 Test-Instanz erreichbar unter:"
    echo "   http://localhost:$LOCAL_PORT"
    echo ""
    echo "🔍 Health Check:"
    if curl -f http://localhost:$LOCAL_PORT/actuator/health >/dev/null 2>&1; then
        echo "✅ Health Check erfolgreich"
    else
        echo "⚠️  Health Check fehlgeschlagen (Instanz startet möglicherweise noch)"
    fi
    echo ""
    echo "📝 Logs: tail -f $LOG_FILE"
    echo "🛑 Beenden: pkill -f 'ssh.*$LOCAL_PORT'"
else
    echo "❌ SSH-Tunnel konnte nicht erstellt werden"
    echo "Logs:"
    cat "$LOG_FILE"
    exit 1
fi

