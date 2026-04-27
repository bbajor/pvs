#!/bin/bash
# Build a native on-premise release package without containers.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="${1:-${RELEASE_VERSION:-local}}"
PACKAGE_NAME="ivomplaner-onpremise-$VERSION"
DIST_DIR="$ROOT_DIR/build/distributions"
WORK_DIR="$ROOT_DIR/build/onpremise"
PACKAGE_DIR="$WORK_DIR/$PACKAGE_NAME"

cd "$ROOT_DIR"

./gradlew clean bootJar -Pvaadin.productionMode --no-daemon

rm -rf "$WORK_DIR" "$DIST_DIR/$PACKAGE_NAME.tar.gz" "$DIST_DIR/SHA256SUMS"
mkdir -p "$PACKAGE_DIR/app" "$PACKAGE_DIR/scripts" "$PACKAGE_DIR/systemd" "$PACKAGE_DIR/docs" "$DIST_DIR"

cp build/libs/*.jar "$PACKAGE_DIR/app/pvs-app.jar"
cp onpremise/install.sh "$PACKAGE_DIR/install.sh"
cp onpremise/env.example "$PACKAGE_DIR/"
cp onpremise/update.sh "$PACKAGE_DIR/scripts/ivomplaner-update"
cp onpremise/backup.sh "$PACKAGE_DIR/scripts/ivomplaner-backup"
cp onpremise/restore.sh "$PACKAGE_DIR/scripts/ivomplaner-restore"
cp onpremise/uninstall.sh "$PACKAGE_DIR/scripts/ivomplaner-uninstall"
cp onpremise/ivomplaner.service "$PACKAGE_DIR/systemd/ivomplaner.service"
cp onpremise/README.md "$PACKAGE_DIR/docs/"
cp onpremise/INSTALLATION.md "$PACKAGE_DIR/docs/"
cp onpremise/TROUBLESHOOTING.md "$PACKAGE_DIR/docs/"
cp CURSOR-AI-ONPREMISE.md "$PACKAGE_DIR/docs/" 2>/dev/null || true

printf '%s\n' "$VERSION" > "$PACKAGE_DIR/VERSION"
printf 'ivomplaner-onpremise-%s.tar.gz\n' "$VERSION" > "$PACKAGE_DIR/ARTIFACT"
chmod +x "$PACKAGE_DIR/install.sh" "$PACKAGE_DIR/scripts/"*

(
    cd "$WORK_DIR"
    tar -czf "$DIST_DIR/$PACKAGE_NAME.tar.gz" "$PACKAGE_NAME"
)

(
    cd "$DIST_DIR"
    sha256sum "$PACKAGE_NAME.tar.gz" > SHA256SUMS
    cp "$PACKAGE_NAME.tar.gz" ivomplaner-onpremise-latest.tar.gz
    sha256sum ivomplaner-onpremise-latest.tar.gz > ivomplaner-onpremise-latest.tar.gz.sha256
    sha256sum "$PACKAGE_NAME.tar.gz" > "$PACKAGE_NAME.tar.gz.sha256"
)

echo "Created $DIST_DIR/$PACKAGE_NAME.tar.gz"
echo "Created $DIST_DIR/SHA256SUMS"
