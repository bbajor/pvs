#!/bin/bash
# Update a native IVOMPlaner installation from a release tarball.

set -euo pipefail

INSTALL_DIR="${INSTALL_DIR:-/opt/ivomplaner}"
ENV_FILE="${ENV_FILE:-/etc/ivomplaner/ivomplaner.env}"
SERVICE_NAME="${SERVICE_NAME:-ivomplaner}"
SERVICE_USER="${SERVICE_USER:-ivomplaner}"
ARTIFACT="${1:-}"
DEFAULT_RELEASE_BASE_URL="${DEFAULT_RELEASE_BASE_URL:-https://github.com/bbajor/pvs/releases/latest/download}"
DEFAULT_RELEASE_FILE="${DEFAULT_RELEASE_FILE:-ivomplaner-onpremise-latest.tar.gz}"

if [ "$(id -u)" -ne 0 ]; then
    echo "Error: run this script as root."
    exit 1
fi

if [ "$ARTIFACT" = "latest" ]; then
    ARTIFACT=""
fi

if [ -z "$ARTIFACT" ]; then
    if [ -f "$ENV_FILE" ]; then
        # shellcheck disable=SC1090
        . "$ENV_FILE"
    fi
    ARTIFACT="${RELEASE_ARTIFACT_URL:-$DEFAULT_RELEASE_BASE_URL/$DEFAULT_RELEASE_FILE}"
fi

if [ -z "$ARTIFACT" ]; then
    echo "Usage: $0 [latest|path-or-url-to-ivomplaner-onpremise-<version>.tar.gz]"
    echo "Alternatively set RELEASE_ARTIFACT_URL in $ENV_FILE."
    exit 1
fi

if [ -f "$ENV_FILE" ]; then
    set -a
    # shellcheck disable=SC1090
    . "$ENV_FILE"
    set +a
fi

mkdir -p "$INSTALL_DIR/releases" "$INSTALL_DIR/backups" "$INSTALL_DIR/tmp"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
work_dir="$(mktemp -d "$INSTALL_DIR/tmp/update-$timestamp.XXXXXX")"
artifact_file="$work_dir/release.tar.gz"
previous_target=""

cleanup() {
    rm -rf "$work_dir"
}
trap cleanup EXIT

fetch_artifact() {
    case "$ARTIFACT" in
        http://*|https://*)
            curl -fL "$ARTIFACT" -o "$artifact_file"
            checksum_url="${ARTIFACT}.sha256"
            if curl -fsL "$checksum_url" -o "$artifact_file.sha256"; then
                expected="$(awk '{print $1}' "$artifact_file.sha256")"
                actual="$(sha256sum "$artifact_file" | awk '{print $1}')"
                if [ "$expected" != "$actual" ]; then
                    echo "Error: checksum mismatch for $ARTIFACT"
                    exit 1
                fi
            else
                echo "Warning: no checksum file found at $checksum_url"
            fi
            ;;
        *)
            cp "$ARTIFACT" "$artifact_file"
            ;;
    esac
}

wait_for_health() {
    for _ in $(seq 1 30); do
        if curl -fsS "http://127.0.0.1:${PORT:-8080}/actuator/health" >/dev/null 2>&1; then
            return 0
        fi
        sleep 2
    done
    return 1
}

echo "Creating database backup before update..."
/usr/local/bin/ivomplaner-backup "$INSTALL_DIR/backups/pre-update-$timestamp.dump"

echo "Fetching release artifact..."
fetch_artifact

echo "Unpacking release..."
tar -xzf "$artifact_file" -C "$work_dir"
release_root="$(find "$work_dir" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
if [ -z "$release_root" ] || [ ! -f "$release_root/VERSION" ] || [ ! -f "$release_root/app/pvs-app.jar" ]; then
    echo "Error: invalid release package."
    exit 1
fi

version="$(tr -d '\r\n' < "$release_root/VERSION")"
target="$INSTALL_DIR/releases/$version"
if [ -e "$target" ]; then
    echo "Error: release already installed: $version"
    exit 1
fi

if [ -L "$INSTALL_DIR/current" ]; then
    previous_target="$(readlink -f "$INSTALL_DIR/current")"
fi

mv "$release_root" "$target"
chown -R "$SERVICE_USER:$SERVICE_USER" "$target"
chmod -R go-rwx "$target"
ln -sfn "$target" "$INSTALL_DIR/current"
chown -h "$SERVICE_USER:$SERVICE_USER" "$INSTALL_DIR/current"

echo "Restarting $SERVICE_NAME..."
systemctl restart "$SERVICE_NAME"

echo "Waiting for health endpoint..."
if wait_for_health; then
    echo "Update completed successfully: $version"
    exit 0
fi

echo "Health check failed."
if [ -n "$previous_target" ] && [ -d "$previous_target" ]; then
    echo "Rolling back to previous release..."
    ln -sfn "$previous_target" "$INSTALL_DIR/current"
    chown -h "$SERVICE_USER:$SERVICE_USER" "$INSTALL_DIR/current"
    systemctl restart "$SERVICE_NAME" || true
fi

exit 1
