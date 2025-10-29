#!/bin/bash
# Hetzner Server Setup Script
# Führe dieses Script auf dem Hetzner Server aus

set -e

echo "🚀 PVS Hetzner Server Setup"
echo "============================"

# Docker installieren
echo "📦 Installiere Docker..."
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
rm get-docker.sh

# Docker Compose installieren
echo "📦 Installiere Docker Compose..."
DOCKER_COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep 'tag_name' | cut -d\" -f4)
sudo curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# User zu Docker-Gruppe hinzufügen
echo "👤 Konfiguriere Docker-Gruppe..."
sudo usermod -aG docker $USER

# Projekt-Verzeichnis erstellen
echo "📁 Erstelle Projekt-Verzeichnis..."
sudo mkdir -p /opt/pvs
sudo chown $USER:$USER /opt/pvs
cd /opt/pvs

# Environment-Datei erstellen
echo "⚙️ Erstelle Environment-Datei..."
cat > .env <<EOF
# Database Configuration
POSTGRES_DB=pvs
POSTGRES_USER=pvs_user
POSTGRES_PASSWORD=$(openssl rand -base64 32)

# Docker Registry
DOCKER_REGISTRY=ghcr.io
DOCKER_IMAGE=bbajor/pvs

# Let's Encrypt (anpassen!)
LETSENCRYPT_EMAIL=deine@email.de
EOF

echo ""
echo "✅ Server Setup abgeschlossen!"
echo ""
echo "📝 Nächste Schritte:"
echo "1. Kopiere docker-compose.production.yml nach /opt/pvs/"
echo "2. Starte PostgreSQL: docker-compose -f docker-compose.production.yml up -d postgres"
echo "3. Erstelle Datenbanken: siehe HETZNER_SETUP.md"
echo ""
echo "🔐 WICHTIG: Postgres Password aus .env Datei kopieren:"
grep POSTGRES_PASSWORD .env

