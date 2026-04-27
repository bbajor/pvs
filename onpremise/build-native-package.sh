#!/bin/bash
# Build a native on-premise release package without containers.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST_DIR="$ROOT_DIR/build/onpremise"
PACKAGE_DIR="$DIST_DIR/ivomplaner-onpremise"

cd "$ROOT_DIR"

./gradlew clean bootJar -Pvaadin.productionMode --no-daemon

rm -rf "$PACKAGE_DIR"
mkdir -p "$PACKAGE_DIR/app" "$PACKAGE_DIR/onpremise"

cp build/libs/*.jar "$PACKAGE_DIR/app/pvs-app.jar"
cp "$PACKAGE_DIR/app/pvs-app.jar" "$PACKAGE_DIR/onpremise/pvs-app.jar"
cp onpremise/env.example "$PACKAGE_DIR/onpremise/"
cp onpremise/install.sh "$PACKAGE_DIR/onpremise/"
cp onpremise/update.sh "$PACKAGE_DIR/onpremise/"
cp onpremise/backup.sh "$PACKAGE_DIR/onpremise/"
cp onpremise/restore.sh "$PACKAGE_DIR/onpremise/"
cp onpremise/pvs-onpremise.service "$PACKAGE_DIR/onpremise/"
cp onpremise/README.md "$PACKAGE_DIR/onpremise/"
cp onpremise/INSTALLATION.md "$PACKAGE_DIR/onpremise/"
cp onpremise/TROUBLESHOOTING.md "$PACKAGE_DIR/onpremise/"

chmod +x "$PACKAGE_DIR/onpremise/"*.sh

(
    cd "$DIST_DIR"
    tar -czf ivomplaner-onpremise.tar.gz ivomplaner-onpremise
)

echo "Created $DIST_DIR/ivomplaner-onpremise.tar.gz"
