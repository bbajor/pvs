#!/bin/bash
# Native IVOMPlaner on-premise uninstaller for Linux.

set -euo pipefail

INSTALL_DIR="${INSTALL_DIR:-/opt/ivomplaner}"
CONFIG_DIR="${CONFIG_DIR:-/etc/ivomplaner}"
SERVICE_NAME="${SERVICE_NAME:-ivomplaner}"
SERVICE_USER="${SERVICE_USER:-ivomplaner}"

if [ "$(id -u)" -ne 0 ]; then
    echo "Error: run this script as root."
    exit 1
fi

ask_yes_no() {
    local prompt="$1"
    local default="${2:-n}"
    local answer

    if [ "$default" = "y" ]; then
        read -r -p "$prompt [Y/n]: " answer
        answer="${answer:-y}"
    else
        read -r -p "$prompt [y/N]: " answer
        answer="${answer:-n}"
    fi

    case "$answer" in
        [Yy]|[Yy][Ee][Ss]) return 0 ;;
        *) return 1 ;;
    esac
}

echo "=== IVOMPlaner Native On-Premise Uninstaller ==="
echo ""

if systemctl list-unit-files "$SERVICE_NAME.service" >/dev/null 2>&1; then
    if ask_yes_no "Stop and remove systemd service '$SERVICE_NAME'?" "y"; then
        systemctl stop "$SERVICE_NAME.service" || true
        systemctl disable "$SERVICE_NAME.service" || true
        rm -f "/etc/systemd/system/$SERVICE_NAME.service"
        systemctl daemon-reload
        echo "Service removed."
    fi
fi

for command_path in /usr/local/bin/ivomplaner-update /usr/local/bin/ivomplaner-backup /usr/local/bin/ivomplaner-restore /usr/local/bin/ivomplaner-uninstall; do
    rm -f "$command_path"
done

if [ -d "$INSTALL_DIR/backups" ] && [ "$(ls -A "$INSTALL_DIR/backups" 2>/dev/null)" ]; then
    echo "Backups exist in $INSTALL_DIR/backups."
    if ask_yes_no "Keep backups?" "y"; then
        backup_target="/tmp/ivomplaner-backups-$(date -u +%Y%m%dT%H%M%SZ)"
        mv "$INSTALL_DIR/backups" "$backup_target"
        echo "Backups moved to $backup_target."
    fi
fi

if [ -d "$INSTALL_DIR" ]; then
    if ask_yes_no "Remove installation directory '$INSTALL_DIR'?" "y"; then
        rm -rf "$INSTALL_DIR"
        echo "Installation directory removed."
    fi
fi

if [ -d "$CONFIG_DIR" ]; then
    if ask_yes_no "Remove configuration directory '$CONFIG_DIR'?" "n"; then
        rm -rf "$CONFIG_DIR"
        echo "Configuration directory removed."
    fi
fi

if id "$SERVICE_USER" >/dev/null 2>&1; then
    if ask_yes_no "Remove service user '$SERVICE_USER'?" "n"; then
        userdel "$SERVICE_USER" || true
        echo "Service user removed."
    fi
fi

echo ""
echo "PostgreSQL database and role are intentionally not removed."
echo "Remove them manually only after a verified backup."
