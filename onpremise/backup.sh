#!/bin/bash
# Create an IVOMPlaner PostgreSQL backup.

set -euo pipefail

INSTALL_DIR="${INSTALL_DIR:-/opt/pvs}"
BACKUP_DIR="${BACKUP_DIR:-$INSTALL_DIR/backups}"
ENV_FILE="${ENV_FILE:-$INSTALL_DIR/.env}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

TARGET_FILE="${1:-}"

if [ -f "$ENV_FILE" ]; then
    set -a
    # shellcheck disable=SC1090
    . "$ENV_FILE"
    set +a
fi

POSTGRES_DB="${POSTGRES_DB:-pvs}"
POSTGRES_USER="${POSTGRES_USER:-pvs}"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_file="${TARGET_FILE:-$BACKUP_DIR/${POSTGRES_DB}_${timestamp}.dump}"

echo "Creating backup: $backup_file"
PGPASSWORD="${POSTGRES_PASSWORD:-${DB_PASSWORD:-}}" pg_dump \
    --host=127.0.0.1 \
    --port="${POSTGRES_PORT:-5432}" \
    --username="$POSTGRES_USER" \
    --format=custom \
    --file="$backup_file" \
    "$POSTGRES_DB"

chmod 600 "$backup_file"

if [ "$RETENTION_DAYS" -gt 0 ]; then
    find "$BACKUP_DIR" -type f -name "${POSTGRES_DB}_*.dump" -mtime +"$RETENTION_DAYS" -delete
fi

echo "Backup created successfully."
