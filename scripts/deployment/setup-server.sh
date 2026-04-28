#!/bin/bash
# Hetzner Server Setup Script für PVS
# Führe dieses Script auf dem Hetzner Server aus (als root oder mit sudo)

set -e

echo "🚀 PVS Hetzner Server Setup"
echo "============================"
echo ""

# Prüfe ob Script als root ausgeführt wird
if [ "$EUID" -ne 0 ]; then 
    echo "⚠️  Script sollte als root ausgeführt werden"
    echo "Verwende: sudo $0"
    read -p "Trotzdem fortfahren? (j/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Jj]$ ]]; then
        exit 1
    fi
fi

# System aktualisieren
echo "📦 System aktualisieren..."
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get upgrade -y

# Basis-Pakete installieren
echo "📦 Installiere Basis-Pakete..."
apt-get install -y \
    curl \
    wget \
    git \
    vim \
    htop \
    ufw \
    fail2ban \
    unattended-upgrades \
    logrotate

# Podman installieren
echo "📦 Installiere Podman..."
. /etc/os-release
sh -c "echo 'deb http://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/xUbuntu_${VERSION_ID}/ /' > /etc/apt/sources.list.d/devel:kubic:libcontainers:stable.list"
wget -nv https://download.opensuse.org/repositories/devel:kubic:libcontainers:stable/xUbuntu_${VERSION_ID}/Release.key -O Release.key
apt-key add Release.key
rm Release.key
apt-get update
apt-get install -y podman podman-compose

# Podman-Service konfigurieren
echo "🔧 Konfiguriere Podman..."
systemctl enable podman.socket
systemctl start podman.socket

# Benutzer 'pvs' erstellen (falls nicht vorhanden)
echo "👤 Erstelle Benutzer 'pvs'..."
if ! id "pvs" &>/dev/null; then
    useradd -m -s /bin/bash pvs
    usermod -aG podman pvs
    echo "✅ Benutzer 'pvs' erstellt"
else
    echo "ℹ️  Benutzer 'pvs' existiert bereits"
    usermod -aG podman pvs
fi

# Projekt-Verzeichnis erstellen
echo "📁 Erstelle Projekt-Verzeichnis..."
mkdir -p /opt/pvs
mkdir -p /opt/pvs/backups
mkdir -p /opt/pvs/logs
chown -R pvs:pvs /opt/pvs

# SSH-Verzeichnis für Benutzer
mkdir -p /home/pvs/.ssh
chmod 700 /home/pvs/.ssh
chown -R pvs:pvs /home/pvs/.ssh

# Firewall konfigurieren
echo "🔥 Konfiguriere Firewall..."
ufw --force reset
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment 'SSH'
ufw --force enable
echo "✅ Firewall konfiguriert (nur SSH offen)"

# Fail2Ban konfigurieren
echo "🛡️  Konfiguriere Fail2Ban..."
cat > /etc/fail2ban/jail.local <<EOF
[DEFAULT]
bantime = 3600
findtime = 600
maxretry = 3

[sshd]
enabled = true
port = 22
logpath = /var/log/auth.log
maxretry = 3
bantime = 3600
EOF

systemctl enable fail2ban
systemctl restart fail2ban
echo "✅ Fail2Ban konfiguriert"

# Log-Rotation konfigurieren
echo "📝 Konfiguriere Log-Rotation..."
cat > /etc/logrotate.d/pvs <<EOF
/opt/pvs/logs/*.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0644 pvs pvs
    sharedscripts
    postrotate
        systemctl reload podman > /dev/null 2>&1 || true
    endscript
}
EOF
echo "✅ Log-Rotation konfiguriert"

# Automatische Updates konfigurieren
echo "🔄 Konfiguriere automatische Updates..."
cat > /etc/apt/apt.conf.d/50unattended-upgrades <<EOF
Unattended-Upgrade::Allowed-Origins {
    "\${distro_id}:\${distro_codename}-security";
    "\${distro_id}:\${distro_codename}-updates";
};
Unattended-Upgrade::AutoFixInterruptedDpkg "true";
Unattended-Upgrade::MinimalSteps "true";
Unattended-Upgrade::Remove-Unused-Kernel-Packages "true";
Unattended-Upgrade::Remove-Unused-Dependencies "true";
Unattended-Upgrade::Automatic-Reboot "false";
EOF
systemctl enable unattended-upgrades
echo "✅ Automatische Updates konfiguriert"

# Environment-Datei-Vorlage erstellen
echo "⚙️ Erstelle Environment-Datei-Vorlage..."
cat > /opt/pvs/.env.example <<EOF
# Database Configuration (Test)
POSTGRES_DB_TEST=pvs_test
POSTGRES_USER_TEST=pvs_user
POSTGRES_PASSWORD_TEST=$(openssl rand -base64 32)

# Database Configuration (Production)
POSTGRES_DB_PROD=pvs_prod
POSTGRES_USER_PROD=pvs_user
POSTGRES_PASSWORD_PROD=$(openssl rand -base64 32)

# Redis Configuration
REDIS_PASSWORD_TEST=$(openssl rand -base64 32)
REDIS_PASSWORD=$(openssl rand -base64 32)

# Docker Registry
DOCKER_REGISTRY=ghcr.io
DOCKER_IMAGE=bbajor/pvs

# Let's Encrypt (anpassen!)
LETSENCRYPT_EMAIL=deine@email.de
EOF

chown pvs:pvs /opt/pvs/.env.example
echo "✅ Environment-Datei-Vorlage erstellt: /opt/pvs/.env.example"

# Deployment-Scripts kopieren (falls vorhanden)
if [ -d "/tmp/pvs-scripts" ]; then
    echo "📋 Kopiere Deployment-Scripts..."
    cp -r /tmp/pvs-scripts/* /opt/pvs/scripts/ 2>/dev/null || true
    chown -R pvs:pvs /opt/pvs/scripts
    chmod +x /opt/pvs/scripts/*.sh 2>/dev/null || true
    echo "✅ Deployment-Scripts kopiert"
fi

echo ""
echo "✅ Server Setup abgeschlossen!"
echo ""
echo "📝 Nächste Schritte:"
echo "1. Als Benutzer 'pvs' einloggen:"
echo "   su - pvs"
echo ""
echo "2. SSH-Key für Benutzer 'pvs' hinzufügen:"
echo "   sudo -u pvs mkdir -p /home/pvs/.ssh"
echo "   sudo -u pvs nano /home/pvs/.ssh/authorized_keys"
echo "   # Füge deinen öffentlichen SSH-Key hinzu"
echo ""
echo "3. Environment-Datei erstellen:"
echo "   cd /opt/pvs"
echo "   cp .env.example .env"
echo "   nano .env  # Passwörter anpassen"
echo ""
echo "4. Repository klonen oder Dateien kopieren:"
echo "   cd /opt/pvs"
echo "   git clone https://github.com/bbajor/pvs.git ."
echo "   # Oder: podman-compose.production.yml kopieren"
echo ""
echo "5. Test-Instanz starten:"
echo "   podman-compose -f podman-compose.production.yml --profile test up -d"
echo ""
echo "6. Firewall-Status prüfen:"
echo "   sudo ufw status verbose"
echo ""
echo "🔐 WICHTIG: Passwörter aus .env.example notieren!"
echo ""
