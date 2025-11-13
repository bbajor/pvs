#!/bin/bash
# Firewall-Setup für PVS Hetzner Server
# Konfiguriert UFW: Nur SSH (Port 22) offen, alle anderen Ports blockiert

set -e

echo "🔥 PVS Firewall Setup"
echo "===================="
echo ""

# Prüfe ob Script als root ausgeführt wird
if [ "$EUID" -ne 0 ]; then 
    echo "❌ Script muss als root ausgeführt werden"
    echo "Verwende: sudo $0"
    exit 1
fi

# Prüfe ob UFW installiert ist
if ! command -v ufw &> /dev/null; then
    echo "📦 Installiere UFW..."
    apt-get update
    apt-get install -y ufw
fi

# Firewall zurücksetzen
echo "🔄 Setze Firewall zurück..."
ufw --force reset

# Standard-Policies
echo "⚙️  Setze Standard-Policies..."
ufw default deny incoming
ufw default allow outgoing

# SSH erlauben (wichtig: vorher konfigurieren!)
echo "🔐 Erlaube SSH (Port 22)..."
ufw allow 22/tcp comment 'SSH'

# Firewall aktivieren
echo "🚀 Aktiviere Firewall..."
ufw --force enable

# Status anzeigen
echo ""
echo "✅ Firewall konfiguriert!"
echo ""
echo "📊 Firewall-Status:"
ufw status verbose
echo ""
echo "⚠️  WICHTIG:"
echo "   - Nur Port 22 (SSH) ist von außen erreichbar"
echo "   - Application-Ports (8080, 8081, etc.) sind nur auf localhost gebunden"
echo "   - Zugriff auf Application erfolgt über SSH-Tunnel"
echo ""
echo "🔍 Ports prüfen:"
echo "   sudo netstat -tulpn | grep LISTEN"
echo "   sudo ss -tulpn"
echo ""

