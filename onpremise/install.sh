#!/bin/bash
# Install IVOMPlaner from a native release package.

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

INSTALL_DIR="${INSTALL_DIR:-/opt/ivomplaner}"
CONFIG_DIR="${CONFIG_DIR:-/etc/ivomplaner}"
LOG_DIR="${LOG_DIR:-/var/log/ivomplaner}"
BIN_DIR="${BIN_DIR:-/usr/local/bin}"
SERVICE_USER="${SERVICE_USER:-ivomplaner}"
SERVICE_NAME="${SERVICE_NAME:-ivomplaner}"
RELEASE_ARCHIVE="${1:-${RELEASE_ARCHIVE:-}}"
RELEASE_URL="${RELEASE_URL:-}"
GITHUB_REPOSITORY="${GITHUB_REPOSITORY:-bbajor/pvs}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${GREEN}=== IVOMPlaner On-Premise Installer ===${NC}"

if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}Error: run this script as root.${NC}"
    exit 1
fi

install_packages() {
    if [ ! -f /etc/os-release ]; then
        echo -e "${RED}Error: cannot detect Linux distribution.${NC}"
        exit 1
    fi

    . /etc/os-release
    case "$ID" in
        ubuntu|debian)
            apt-get update
            apt-get install -y openjdk-21-jre-headless postgresql postgresql-client curl openssl tar gzip
            ;;
        fedora|rhel|centos)
            dnf install -y java-21-openjdk-headless postgresql-server postgresql curl openssl tar gzip
            if [ ! -d /var/lib/pgsql/data/base ]; then
                postgresql-setup --initdb || true
            fi
            ;;
        arch|manjaro)
            pacman -S --noconfirm jre21-openjdk postgresql curl openssl tar gzip
            ;;
        *)
            echo -e "${RED}Error: unsupported distribution '$ID'. Install Java 21 and PostgreSQL manually.${NC}"
            exit 1
            ;;
    esac
}

ensure_command() {
    command -v "$1" >/dev/null 2>&1
}

find_local_archive() {
    if [ -n "$RELEASE_ARCHIVE" ]; then
        return
    fi
    if [ -n "$RELEASE_URL" ]; then
        RELEASE_ARCHIVE="$(mktemp --suffix=.tar.gz /tmp/ivomplaner-release.XXXXXX)"
        curl -fL "$RELEASE_URL" -o "$RELEASE_ARCHIVE"
        return
    fi
    if [ -f "$SCRIPT_DIR/../VERSION" ] && [ -f "$SCRIPT_DIR/../app/pvs-app.jar" ]; then
        package_dir="$(cd "$SCRIPT_DIR/.." && pwd)"
        RELEASE_ARCHIVE="$package_dir"
        return
    fi
    local candidate
    candidate="$(ls "$SCRIPT_DIR"/ivomplaner-onpremise-*.tar.gz 2>/dev/null | head -n 1 || true)"
    if [ -n "$candidate" ]; then
        RELEASE_ARCHIVE="$candidate"
        return
    fi
    RELEASE_URL="https://github.com/$GITHUB_REPOSITORY/releases/latest/download/ivomplaner-onpremise-latest.tar.gz"
    RELEASE_ARCHIVE="$(mktemp /tmp/ivomplaner-release.XXXXXX.tar.gz)"
    curl -fL "$RELEASE_URL" -o "$RELEASE_ARCHIVE"
}

if ! ensure_command java || ! java -version 2>&1 | grep -q "21"; then
    echo "Installing Java 21 and PostgreSQL packages..."
    install_packages
fi

if ! ensure_command psql || ! ensure_command pg_dump || ! ensure_command curl; then
    echo "Installing missing runtime packages..."
    install_packages
fi

if ! ensure_command sha256sum; then
    echo -e "${RED}Error: sha256sum is required but not available.${NC}"
    exit 1
fi

systemctl enable postgresql.service >/dev/null 2>&1 || true
systemctl start postgresql.service

if ! id "$SERVICE_USER" >/dev/null 2>&1; then
    echo "Creating service user '$SERVICE_USER'..."
    useradd -r -s /usr/sbin/nologin -d "$INSTALL_DIR" -m "$SERVICE_USER"
fi

mkdir -p "$INSTALL_DIR/releases" "$INSTALL_DIR/backups" "$CONFIG_DIR" "$LOG_DIR"
chown -R "$SERVICE_USER:$SERVICE_USER" "$INSTALL_DIR" "$LOG_DIR"
chmod 750 "$INSTALL_DIR" "$INSTALL_DIR/releases" "$INSTALL_DIR/backups" "$LOG_DIR"

find_local_archive
if [ -z "$RELEASE_ARCHIVE" ] || { [ ! -f "$RELEASE_ARCHIVE" ] && [ ! -d "$RELEASE_ARCHIVE" ]; }; then
    echo -e "${RED}Error: release archive not found.${NC}"
    echo "Usage: sudo bash install.sh /path/to/ivomplaner-onpremise-VERSION.tar.gz"
    echo "Or set RELEASE_URL=https://.../ivomplaner-onpremise-VERSION.tar.gz"
    exit 1
fi

if [ -n "$RELEASE_URL" ]; then
    checksum_url="${RELEASE_URL}.sha256"
    checksum_file="$(mktemp /tmp/ivomplaner-release.XXXXXX.sha256)"
    if curl -fsL "$checksum_url" -o "$checksum_file"; then
        expected="$(awk '{print $1}' "$checksum_file")"
        actual="$(sha256sum "$RELEASE_ARCHIVE" | awk '{print $1}')"
        if [ "$expected" != "$actual" ]; then
            echo -e "${RED}Error: checksum mismatch for release archive.${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}Warning: no checksum found at $checksum_url${NC}"
    fi
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT
if [ -d "$RELEASE_ARCHIVE" ]; then
    release_root="$RELEASE_ARCHIVE"
else
    tar -xzf "$RELEASE_ARCHIVE" -C "$tmp_dir"
    release_root="$(find "$tmp_dir" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
fi
if [ -z "$release_root" ]; then
    echo -e "${RED}Error: release archive has no top-level directory.${NC}"
    exit 1
fi
if [ ! -f "$release_root/VERSION" ] || [ ! -f "$release_root/app/pvs-app.jar" ]; then
    echo -e "${RED}Error: invalid IVOMPlaner release package.${NC}"
    exit 1
fi
version="$(cat "$release_root/VERSION")"
target_release="$INSTALL_DIR/releases/$version"
rm -rf "$target_release"
mkdir -p "$target_release"
cp -a "$release_root/." "$target_release/"
chown -R "$SERVICE_USER:$SERVICE_USER" "$target_release"
ln -sfn "$target_release" "$INSTALL_DIR/current"
chown -h "$SERVICE_USER:$SERVICE_USER" "$INSTALL_DIR/current"

cp "$target_release/env.example" "$CONFIG_DIR/ivomplaner.env.example"
if [ ! -f "$CONFIG_DIR/ivomplaner.env" ]; then
    cp "$CONFIG_DIR/ivomplaner.env.example" "$CONFIG_DIR/ivomplaner.env"
    POSTGRES_PASSWORD="$(openssl rand -base64 32 | tr -d '=+/' | cut -c1-25)"
    SMTP_KEY="$(openssl rand -base64 32 | tr -d '\n' | cut -c1-32)"
    sed -i "s/CHANGE_ME_SECURE_PASSWORD/$POSTGRES_PASSWORD/g" "$CONFIG_DIR/ivomplaner.env"
    sed -i "s#^IVOMPLANER_HOME=.*#IVOMPLANER_HOME=$INSTALL_DIR#" "$CONFIG_DIR/ivomplaner.env"
    sed -i "s#^IVOMPLANER_CONFIG=.*#IVOMPLANER_CONFIG=$CONFIG_DIR/ivomplaner.env#" "$CONFIG_DIR/ivomplaner.env"
    sed -i "s#^RELEASE_ARTIFACT_URL=.*#RELEASE_ARTIFACT_URL=${RELEASE_URL:-https://github.com/$GITHUB_REPOSITORY/releases/latest/download/ivomplaner-onpremise-latest.tar.gz}#" "$CONFIG_DIR/ivomplaner.env"
    sed -i "s#^IVOMPLANER_RELEASE_BASE_URL=.*#IVOMPLANER_RELEASE_BASE_URL=https://github.com/$GITHUB_REPOSITORY/releases/latest/download#" "$CONFIG_DIR/ivomplaner.env"
    sed -i "s/SMTP_ENCRYPTION_KEY=$/SMTP_ENCRYPTION_KEY=$SMTP_KEY/g" "$CONFIG_DIR/ivomplaner.env"
    echo -e "${GREEN}Created $CONFIG_DIR/ivomplaner.env with generated secrets.${NC}"
fi
chmod 600 "$CONFIG_DIR/ivomplaner.env" "$CONFIG_DIR/ivomplaner.env.example"
chown root:"$SERVICE_USER" "$CONFIG_DIR/ivomplaner.env" "$CONFIG_DIR/ivomplaner.env.example"

set -a
. "$CONFIG_DIR/ivomplaner.env"
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

install -m 0755 "$target_release/scripts/ivomplaner-update" "$BIN_DIR/ivomplaner-update"
install -m 0755 "$target_release/scripts/ivomplaner-backup" "$BIN_DIR/ivomplaner-backup"
install -m 0755 "$target_release/scripts/ivomplaner-restore" "$BIN_DIR/ivomplaner-restore"
install -m 0755 "$target_release/scripts/ivomplaner-uninstall" "$BIN_DIR/ivomplaner-uninstall"
install -m 0755 "$target_release/scripts/ivomplaner-update-wrapper" "$BIN_DIR/ivomplaner-update-wrapper"
install -m 0644 "$target_release/systemd/ivomplaner.service" "/etc/systemd/system/$SERVICE_NAME.service"
cat > "/etc/sudoers.d/ivomplaner-update" <<EOF
$SERVICE_USER ALL=(root) NOPASSWD: $BIN_DIR/ivomplaner-update-wrapper
EOF
chmod 0440 "/etc/sudoers.d/ivomplaner-update"

systemctl daemon-reload
systemctl enable "$SERVICE_NAME.service"
systemctl restart "$SERVICE_NAME.service"

echo ""
echo -e "${GREEN}=== Installation complete ===${NC}"
echo "Version: $version"
echo "Config: $CONFIG_DIR/ivomplaner.env"
echo "Status: systemctl status $SERVICE_NAME"
echo "Logs: journalctl -u $SERVICE_NAME -f"

