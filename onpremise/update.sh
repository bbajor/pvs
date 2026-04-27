#!/usr/bin/env bash
# Updates the native IVOMPlaner installation from a release JAR or URL.

set -euo pipefail

INSTALL_DIR="${INSTALL_DIR:-/opt/pvs}"
APP_DIR="$INSTALL_DIR/app"
BACKUP_DIR="$INSTALL_DIR/backups"
SERVICE_NAME="${SERVICE_NAME:-pvs-onpremise}"
ARTIFACT="${1:-${RELEASE_ARTIFACT_URL:-}}"

if [ "$(id -u)" -ne 0 ]; then
    echo "Error: run this script as root."
    exit 1
fi

if [ -z "$ARTIFACT" ]; then
    echo "Usage: $0 <path-or-url-to-pvs-app.jar>"
    echo "Alternatively set RELEASE_ARTIFACT_URL in $INSTALL_DIR/.env."
    exit 1
fi

if [ -f "$INSTALL_DIR/.env" ]; then
    # shellcheck disable=SC1091
    set -a
    . "$INSTALL_DIR/.env"
    set +a
fi

mkdir -p "$APP_DIR" "$BACKUP_DIR"

timestamp="$(date -u +%Y%m%d%H%M%S)"
incoming="$APP_DIR/pvs-app-$timestamp.jar"
current="$APP_DIR/pvs-app.jar"
previous="$APP_DIR/pvs-app.previous.jar"

echo "Creating database backup before update..."
"$(dirname "$0")/backup.sh" "$BACKUP_DIR/pre-update-$timestamp.dump"

echo "Fetching release artifact..."
case "$ARTIFACT" in
    http://*|https://*)
        if command -v curl >/dev/null 2>&1; then
            curl -fL "$ARTIFACT" -o "$incoming"
        else
            wget -O "$incoming" "$ARTIFACT"
        fi
        ;;
    *)
        cp "$ARTIFACT" "$incoming"
        ;;
esac

chown pvs:pvs "$incoming"
chmod 640 "$incoming"

if [ -f "$current" ]; then
    cp "$current" "$previous"
    chown pvs:pvs "$previous"
fi

mv "$incoming" "$current"
chown pvs:pvs "$current"

echo "Restarting $SERVICE_NAME..."
systemctl restart "$SERVICE_NAME"

echo "Waiting for health endpoint..."
for attempt in $(seq 1 30); do
    if curl -fsS "http://127.0.0.1:${PORT:-8080}/actuator/health" >/dev/null 2>&1; then
        echo "Update completed successfully."
        exit 0
    fi
    sleep 2
done

echo "Health check failed."
if [ -f "$previous" ]; then
    echo "Rolling back to previous JAR..."
    cp "$previous" "$current"
    chown pvs:pvs "$current"
    systemctl restart "$SERVICE_NAME"
fi

exit 1
