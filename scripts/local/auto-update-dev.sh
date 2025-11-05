#!/bin/bash
# Script für automatisches Update der lokalen Dev-Umgebung
# Kann als Cron-Job oder via Watchtower genutzt werden

set -e

REPO_OWNER="${GITHUB_REPO_OWNER:-bbajor}"
IMAGE_NAME="ghcr.io/${REPO_OWNER}/pvs:dev-latest"
COMPOSE_FILE="podman-compose.dev.yml"
ENV_FILE="podman-compose.dev.env"
COMPOSE_DIR="${PVS_LOCAL_PATH:-$HOME/pvs}"

cd "$COMPOSE_DIR" || {
  echo "❌ Verzeichnis nicht gefunden: $COMPOSE_DIR"
  exit 1
}

echo "🔍 Prüfe auf neues dev Image..."
echo "   Image: $IMAGE_NAME"

# Prüfe ob ein neues Image verfügbar ist
CURRENT_IMAGE_ID=$(podman images --format "{{.ID}}" "$IMAGE_NAME" 2>/dev/null | head -1)
podman pull "$IMAGE_NAME" 2>/dev/null || {
  echo "⚠️  Image pull fehlgeschlagen (möglicherweise noch nicht gebaut)"
  exit 0
}

NEW_IMAGE_ID=$(podman images --format "{{.ID}}" "$IMAGE_NAME" 2>/dev/null | head -1)

if [ "$CURRENT_IMAGE_ID" = "$NEW_IMAGE_ID" ] && [ -n "$CURRENT_IMAGE_ID" ]; then
  echo "✅ Bereits neueste Version installiert"
  exit 0
fi

echo "🔄 Neues Image gefunden - deploye..."
# Try podman compose first, fallback to podman-compose
if command -v podman-compose &> /dev/null; then
  COMPOSE_CMD="podman-compose"
elif podman compose version &> /dev/null; then
  COMPOSE_CMD="podman compose"
else
  echo "❌ Weder podman-compose noch podman compose verfügbar"
  exit 1
fi

$COMPOSE_CMD -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d

echo "⏳ Warte auf Health Check..."
sleep 30

if $COMPOSE_CMD -f "$COMPOSE_FILE" ps | grep -q "Up (healthy)"; then
  echo "✅ Deployment erfolgreich"
else
  echo "⚠️  Deployment abgeschlossen, prüfe Status:"
  $COMPOSE_CMD -f "$COMPOSE_FILE" ps
fi

