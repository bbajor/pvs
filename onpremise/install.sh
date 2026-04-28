#!/bin/bash
# PVS OnPremise Installer für Linux
# Installiert PVS auf einem Linux-System mit Podman

set -euo pipefail

# Farben für Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Installation-Verzeichnis
INSTALL_DIR="/opt/pvs"
SERVICE_USER="pvs"

echo -e "${GREEN}=== PVS OnPremise Installer ===${NC}"
echo ""

# Prüfe Root-Rechte
if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}Fehler: Dieses Skript muss als root ausgeführt werden${NC}"
    exit 1
fi

# Prüfe Podman
if ! command -v podman &> /dev/null; then
    echo -e "${YELLOW}Podman nicht gefunden. Installiere Podman...${NC}"
    
    # Erkenne Distribution
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        case $ID in
            ubuntu|debian)
                apt-get update
                apt-get install -y podman podman-compose
                ;;
            fedora|rhel|centos)
                dnf install -y podman podman-compose
                ;;
            arch|manjaro)
                pacman -S --noconfirm podman podman-compose
                ;;
            *)
                echo -e "${RED}Unbekannte Distribution. Bitte installiere Podman manuell.${NC}"
                exit 1
                ;;
        esac
    else
        echo -e "${RED}Konnte Distribution nicht erkennen. Bitte installiere Podman manuell.${NC}"
        exit 1
    fi
fi

# Prüfe podman-compose
if ! command -v podman-compose &> /dev/null; then
    echo -e "${YELLOW}podman-compose nicht gefunden. Installiere podman-compose...${NC}"
    pip3 install podman-compose || {
        echo -e "${RED}Fehler beim Installieren von podman-compose. Bitte manuell installieren.${NC}"
        exit 1
    }
fi

echo -e "${GREEN}✓ Podman und podman-compose sind installiert${NC}"

# Erstelle Service-User
if ! id "$SERVICE_USER" &>/dev/null; then
    echo "Erstelle Service-User '$SERVICE_USER'..."
    useradd -r -s /bin/bash -d "$INSTALL_DIR" -m "$SERVICE_USER"
    echo -e "${GREEN}✓ Service-User erstellt${NC}"
else
    echo -e "${GREEN}✓ Service-User existiert bereits${NC}"
fi

# Erstelle Installations-Verzeichnis
echo "Erstelle Installations-Verzeichnis..."
mkdir -p "$INSTALL_DIR"
mkdir -p "$INSTALL_DIR/backups"
mkdir -p "$INSTALL_DIR/logs"

# Kopiere Dateien
echo "Kopiere Installationsdateien..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cp "$SCRIPT_DIR/podman-compose.onpremise.yml" "$INSTALL_DIR/"
cp "$SCRIPT_DIR/env.example" "$INSTALL_DIR/.env.example"
cp "$SCRIPT_DIR/pvs-onpremise.service" "$INSTALL_DIR/"

# Setze Berechtigungen
chown -R "$SERVICE_USER:$SERVICE_USER" "$INSTALL_DIR"
chmod 755 "$INSTALL_DIR"
chmod 644 "$INSTALL_DIR/podman-compose.onpremise.yml"
chmod 600 "$INSTALL_DIR/.env.example" 2>/dev/null || true

# Erstelle .env falls nicht vorhanden
if [ ! -f "$INSTALL_DIR/.env" ]; then
    echo "Erstelle .env-Datei..."
    cp "$INSTALL_DIR/.env.example" "$INSTALL_DIR/.env"
    chown "$SERVICE_USER:$SERVICE_USER" "$INSTALL_DIR/.env"
    chmod 600 "$INSTALL_DIR/.env"
    
    # Generiere sichere Passwörter
    echo "Generiere sichere Passwörter..."
    POSTGRES_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)
    SMTP_KEY=$(openssl rand -base64 32 | head -c 32)
    
    sed -i "s/POSTGRES_PASSWORD=CHANGE_ME_SECURE_PASSWORD/POSTGRES_PASSWORD=$POSTGRES_PASSWORD/g" "$INSTALL_DIR/.env"
    sed -i "s/SMTP_ENCRYPTION_KEY=$/SMTP_ENCRYPTION_KEY=$SMTP_KEY/g" "$INSTALL_DIR/.env"
    
    echo -e "${GREEN}✓ .env-Datei erstellt mit generierten Passwörtern${NC}"
    echo -e "${YELLOW}⚠️  WICHTIG: Speichere die Passwörter sicher!${NC}"
    echo "   POSTGRES_PASSWORD: $POSTGRES_PASSWORD"
    echo "   SMTP_ENCRYPTION_KEY: $SMTP_KEY"
else
    echo -e "${GREEN}✓ .env-Datei existiert bereits${NC}"
fi

# Installiere Systemd-Service
echo "Installiere Systemd-Service..."
cp "$INSTALL_DIR/pvs-onpremise.service" /etc/systemd/system/
systemctl daemon-reload
systemctl enable pvs-onpremise.service
echo -e "${GREEN}✓ Systemd-Service installiert und aktiviert${NC}"

# Konfiguriere Podman für Service-User
echo "Konfiguriere Podman..."
# Erlaube Service-User, Podman ohne Root zu nutzen (optional)
# usermod --add-subuids 100000-165535 --add-subgids 100000-165535 "$SERVICE_USER"

echo ""
echo -e "${GREEN}=== Installation abgeschlossen ===${NC}"
echo ""
echo "Nächste Schritte:"
echo "1. Bearbeite $INSTALL_DIR/.env und passe die Konfiguration an"
echo "2. Starte den Service mit: systemctl start pvs-onpremise"
echo "3. Prüfe den Status mit: systemctl status pvs-onpremise"
echo "4. Prüfe die Logs mit: journalctl -u pvs-onpremise -f"
echo ""
echo "Die Anwendung ist nach dem Start erreichbar unter:"
echo "  http://localhost:${APP_PORT:-8080}"
echo ""

