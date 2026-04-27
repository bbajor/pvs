#!/bin/bash
# Restore an IVOMPlaner PostgreSQL backup.

set -euo pipefail

INSTALL_DIR="${INSTALL_DIR:-/opt/ivomplaner}"
CONFIG_DIR="${CONFIG_DIR:-/etc/ivomplaner}"
ENV_FILE="${ENV_FILE:-$CONFIG_DIR/ivomplaner.env}"
SERVICE_NAME="${SERVICE_NAME:-ivomplaner}"

if [ "$EUID" -ne 0 ]; then
    echo "Error: restore.sh must be run as root."
    exit 1
fi

if [ $# -ne 1 ]; then
    echo "Usage: $0 /path/to/backup.dump"
    exit 1
fi

BACKUP_FILE="$1"
if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: Backup file not found: $BACKUP_FILE"
    exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
    echo "Error: Environment file not found: $ENV_FILE"
    exit 1
fi

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

DB_NAME="${POSTGRES_DB:-pvs}"
DB_USER="${POSTGRES_USER:-pvs}"

echo "This will overwrite database '$DB_NAME'."
read -r -p "Type RESTORE to continue: " CONFIRMATION
if [ "$CONFIRMATION" != "RESTORE" ]; then
    echo "Restore cancelled."
    exit 0
fi

systemctl stop "$SERVICE_NAME.service" || true

if ! PGPASSWORD="${POSTGRES_PASSWORD:-${DB_PASSWORD:-}}" pg_restore \
    --clean \
    --if-exists \
    --no-owner \
    --host=127.0.0.1 \
    --port="${POSTGRES_PORT:-5432}" \
    --username="$DB_USER" \
    --dbname="$DB_NAME" \
    "$BACKUP_FILE"; then
    echo "Restore failed."
    systemctl start "$SERVICE_NAME.service" || true
    exit 1
fi

sudo -u postgres psql -v ON_ERROR_STOP=1 <<SQL
ALTER DATABASE "$DB_NAME" OWNER TO "$DB_USER";
SQL

systemctl start "$SERVICE_NAME.service"
echo "Restore completed."
