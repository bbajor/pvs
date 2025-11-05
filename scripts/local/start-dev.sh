#!/bin/bash
# Bash Script für schnellen Start der lokalen Dev-Umgebung
# Startet podman-compose.dev.yml direkt ohne GitHub/GitHub Actions

set -e

COMPOSE_FILE="podman-compose.dev.yml"
ENV_FILE="podman-compose.dev.env"
COMPOSE_DIR="${PVS_LOCAL_PATH:-$(cd "$(dirname "$0")/../.." && pwd)}"

cd "$COMPOSE_DIR"
echo "🚀 Starte PVS Dev-Umgebung..."
echo "Verzeichnis: $COMPOSE_DIR"

# Prüfe ob Podman verfügbar ist
if ! command -v podman &> /dev/null; then
    echo "❌ Podman nicht gefunden. Bitte Podman installieren."
    exit 1
fi
echo "✓ Podman verfügbar"

# Prüfe ob podman-compose.dev.env existiert
if [ ! -f "$ENV_FILE" ]; then
    echo "⚠️  podman-compose.dev.env nicht gefunden - verwende Defaults"
    echo "💡 Tipp: Kopiere podman-compose.dev.env.example zu podman-compose.dev.env für eigene Konfiguration"
    
    read -p "Soll podman-compose.dev.env mit Defaults erstellt werden? (j/n) " create_env
    if [ "$create_env" = "j" ] || [ "$create_env" = "J" ] || [ "$create_env" = "y" ] || [ "$create_env" = "Y" ]; then
        cp podman-compose.dev.env.example "$ENV_FILE"
        echo "✓ podman-compose.dev.env erstellt"
    fi
fi

# Baue lokale Images falls nötig
echo "📦 Baue lokale Images (falls nötig)..."
# Try podman compose first, fallback to podman-compose
if command -v podman-compose &> /dev/null; then
    COMPOSE_CMD="podman-compose"
elif podman compose version &> /dev/null; then
    COMPOSE_CMD="podman compose"
else
    echo "❌ Weder podman-compose noch podman compose verfügbar. Bitte installieren."
    exit 1
fi

if [ -f "$ENV_FILE" ]; then
    $COMPOSE_CMD -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build --pull || echo "⚠️  Build-Warnung (möglicherweise Images bereits vorhanden)"
else
    $COMPOSE_CMD -f "$COMPOSE_FILE" build --pull || echo "⚠️  Build-Warnung (möglicherweise Images bereits vorhanden)"
fi
echo "✓ Images bereit"

# Starte Container
echo "🚀 Starte Container..."
if [ -f "$ENV_FILE" ]; then
    $COMPOSE_CMD -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
else
    $COMPOSE_CMD -f "$COMPOSE_FILE" up -d
fi

if [ $? -eq 0 ]; then
    echo "✓ Container gestartet"
    echo ""
    echo "📊 Status:"
    if [ -f "$ENV_FILE" ]; then
        $COMPOSE_CMD -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
    else
        $COMPOSE_CMD -f "$COMPOSE_FILE" ps
    fi
    echo ""
    echo "🌐 App erreichbar unter: http://localhost:8130"
    echo "🔍 Logs anzeigen mit: $COMPOSE_CMD -f $COMPOSE_FILE logs -f"
    echo "🛑 Stoppen mit: $COMPOSE_CMD -f $COMPOSE_FILE down"
else
    echo "❌ Fehler beim Starten der Container"
    echo "🔍 Prüfe Logs mit: $COMPOSE_CMD -f $COMPOSE_FILE logs"
    exit 1
fi

