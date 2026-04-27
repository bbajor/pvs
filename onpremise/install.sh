#!/bin/bash
# IVOMPlaner native on-premise installer for Linux.

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

INSTALL_DIR="/opt/pvs"
APP_DIR="$INSTALL_DIR/app"
BACKUP_DIR="$INSTALL_DIR/backups"
SERVICE_USER="pvs"
SERVICE_NAME="pvs-onpremise"
POSTGRES_VERSION="${POSTGRES_VERSION:-15}"
APP_JAR_SOURCE="${APP_JAR_SOURCE:-}"

echo -e "${GREEN}=== IVOMPlaner Native On-Premise Installer ===${NC}"
echo ""

if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}Error: run this script as root.${NC}"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

install_packages() {
    if [ ! -f /etc/os-release ]; then
        echo -e "${RED}Error: cannot detect Linux distribution.${NC}"
        exit 1
    fi

    . /etc/os-release
    case "$ID" in
        ubuntu|debian)
            apt-get update
            apt-get install -y "openjdk-21-jre-headless" "postgresql" "postgresql-client" curl openssl
            ;;
        fedora|rhel|centos)
            dnf install -y java-21-openjdk-headless postgresql-server postgresql curl openssl
            if [ ! -d /var/lib/pgsql/data/base ]; then
                postgresql-setup --initdb || true
            fi
            POSTGRES_SERVICE="postgresql"
            ;;
        arch|manjaro)
            pacman -S --noconfirm jre21-openjdk postgresql curl openssl
            ;;
        *)
            echo -e "${RED}Error: unsupported distribution '$ID'. Install Java 21 and PostgreSQL manually.${NC}"
            exit 1
            ;;
    esac
}

ensure_command() {
    local command_name="$1"
    if ! command -v "$command_name" >/dev/null 2>&1; then
        return 1
    fi
}

if ! ensure_command java || ! java -version 2>&1 | grep -q "21"; then
    echo "Installing Java 21 and PostgreSQL packages..."
    install_packages
fi

if ! ensure_command psql; then
    echo "Installing PostgreSQL packages..."
    install_packages
fi

systemctl enable postgresql.service >/dev/null 2>&1 || true
systemctl start postgresql.service

if ! id "$SERVICE_USER" >/dev/null 2>&1; then
    echo "Creating service user '$SERVICE_USER'..."
    useradd -r -s /usr/sbin/nologin -d "$INSTALL_DIR" -m "$SERVICE_USER"
fi

echo "Creating installation directories..."
mkdir -p "$APP_DIR" "$BACKUP_DIR" "$INSTALL_DIR/logs" /var/log/pvs

echo "Copying runtime files..."
cp "$SCRIPT_DIR/env.example" "$INSTALL_DIR/.env.example"
cp "$SCRIPT_DIR/pvs-onpremise.service" "/etc/systemd/system/$SERVICE_NAME.service"
for helper in update.sh backup.sh restore.sh; do
    if [ -f "$SCRIPT_DIR/$helper" ]; then
        cp "$SCRIPT_DIR/$helper" "$INSTALL_DIR/$helper"
        chmod 750 "$INSTALL_DIR/$helper"
    fi
done

if [ -z "$APP_JAR_SOURCE" ]; then
    if [ -f "$SCRIPT_DIR/pvs-app.jar" ]; then
        APP_JAR_SOURCE="$SCRIPT_DIR/pvs-app.jar"
    elif [ -f "$SCRIPT_DIR/../app/pvs-app.jar" ]; then
        APP_JAR_SOURCE="$SCRIPT_DIR/../app/pvs-app.jar"
    elif [ -f "$SCRIPT_DIR/../build/libs/pvs-app-1.0-SNAPSHOT.jar" ]; then
        APP_JAR_SOURCE="$SCRIPT_DIR/../build/libs/pvs-app-1.0-SNAPSHOT.jar"
    fi
fi

if [ -n "$APP_JAR_SOURCE" ] && [ -f "$APP_JAR_SOURCE" ]; then
    cp "$APP_JAR_SOURCE" "$APP_DIR/pvs-app.jar"
else
    echo -e "${YELLOW}No application JAR found. Copy it later to $APP_DIR/pvs-app.jar or run update.sh.${NC}"
fi

if [ ! -f "$INSTALL_DIR/.env" ]; then
    echo "Creating environment file..."
    cp "$INSTALL_DIR/.env.example" "$INSTALL_DIR/.env"
    POSTGRES_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)
    SMTP_KEY=$(openssl rand -base64 32 | tr -d '\n' | cut -c1-32)
    sed -i "s/CHANGE_ME_SECURE_PASSWORD/$POSTGRES_PASSWORD/g" "$INSTALL_DIR/.env"
    sed -i "s/SMTP_ENCRYPTION_KEY=$/SMTP_ENCRYPTION_KEY=$SMTP_KEY/g" "$INSTALL_DIR/.env"
    echo -e "${GREEN}Environment file created. Secrets are stored in $INSTALL_DIR/.env only.${NC}"
fi

chmod 750 "$INSTALL_DIR"
chmod 750 "$APP_DIR" "$BACKUP_DIR" "$INSTALL_DIR/logs"
chmod 600 "$INSTALL_DIR/.env" "$INSTALL_DIR/.env.example"
chown -R "$SERVICE_USER:$SERVICE_USER" "$INSTALL_DIR" /var/log/pvs

set -a
. "$INSTALL_DIR/.env"
set +a

echo "Configuring PostgreSQL database and role..."
sudo -u postgres psql <<SQL
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${POSTGRES_USER}') THEN
        CREATE ROLE ${POSTGRES_USER} LOGIN PASSWORD '${POSTGRES_PASSWORD}';
    ELSE
        ALTER ROLE ${POSTGRES_USER} WITH LOGIN PASSWORD '${POSTGRES_PASSWORD}';
    END IF;
END
\$\$;
SELECT 'CREATE DATABASE ${POSTGRES_DB} OWNER ${POSTGRES_USER}'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${POSTGRES_DB}')\\gexec
ALTER DATABASE ${POSTGRES_DB} OWNER TO ${POSTGRES_USER};
SQL

systemctl daemon-reload
systemctl enable "$SERVICE_NAME.service"

echo ""
echo -e "${GREEN}=== Installation complete ===${NC}"
echo ""
echo "Next steps:"
echo "1. Review $INSTALL_DIR/.env."
echo "2. Copy the application JAR to $APP_DIR/pvs-app.jar if it is not there yet."
echo "3. Start the service: systemctl start $SERVICE_NAME"
echo "4. Check status: systemctl status $SERVICE_NAME"
echo "5. Follow logs: journalctl -u $SERVICE_NAME -f"
echo ""

