#!/bin/bash
# Bash Script für schnellen Start der lokalen Dev-Umgebung
# Startet docker-compose.dev.yml direkt ohne GitHub/GitHub Actions

set -e

COMPOSE_FILE="docker-compose.dev.yml"
ENV_FILE="docker-compose.dev.env"
COMPOSE_DIR="${PVS_LOCAL_PATH:-$(cd "$(dirname "$0")/../.." && pwd)}"

cd "$COMPOSE_DIR"
echo "🚀 Starte PVS Dev-Umgebung..."
echo "Verzeichnis: $COMPOSE_DIR"

# Prüfe ob Docker verfügbar ist
if ! command -v docker &> /dev/null; then
    echo "❌ Docker nicht gefunden. Bitte Docker installieren."
    exit 1
fi
echo "✓ Docker verfügbar"

# Prüfe ob docker-compose.dev.env existiert
if [ ! -f "$ENV_FILE" ]; then
    echo "⚠️  docker-compose.dev.env nicht gefunden - verwende Defaults"
    echo "💡 Tipp: Kopiere docker-compose.dev.env.example zu docker-compose.dev.env für eigene Konfiguration"
    
    read -p "Soll docker-compose.dev.env mit Defaults erstellt werden? (j/n) " create_env
    if [ "$create_env" = "j" ] || [ "$create_env" = "J" ] || [ "$create_env" = "y" ] || [ "$create_env" = "Y" ]; then
        cp docker-compose.dev.env.example "$ENV_FILE"
        echo "✓ docker-compose.dev.env erstellt"
    fi
fi

# Baue lokale Images falls nötig
echo "📦 Baue lokale Images (falls nötig)..."
if [ -f "$ENV_FILE" ]; then
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build --pull || echo "⚠️  Build-Warnung (möglicherweise Images bereits vorhanden)"
else
    docker compose -f "$COMPOSE_FILE" build --pull || echo "⚠️  Build-Warnung (möglicherweise Images bereits vorhanden)"
fi
echo "✓ Images bereit"

# Starte Container
echo "🚀 Starte Container..."
if [ -f "$ENV_FILE" ]; then
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
else
    docker compose -f "$COMPOSE_FILE" up -d
fi

if [ $? -eq 0 ]; then
    echo "✓ Container gestartet"
    echo ""
    echo "📊 Status:"
    if [ -f "$ENV_FILE" ]; then
        docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
    else
        docker compose -f "$COMPOSE_FILE" ps
    fi
    echo ""
    echo "🌐 App erreichbar unter: http://localhost:8130"
    echo "🔍 Logs anzeigen mit: docker compose -f $COMPOSE_FILE logs -f"
    echo "🛑 Stoppen mit: docker compose -f $COMPOSE_FILE down"
else
    echo "❌ Fehler beim Starten der Container"
    echo "🔍 Prüfe Logs mit: docker compose -f $COMPOSE_FILE logs"
    exit 1
fi

