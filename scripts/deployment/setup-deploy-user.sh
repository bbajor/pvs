#!/bin/bash
# Setup Script für deploy-User auf Hetzner Server
# Führe dieses Script als root auf dem Hetzner Server aus

set -e

echo "🚀 Setup deploy-User für Hetzner Deployment"
echo "============================================"
echo ""

# Prüfe ob Script als root ausgeführt wird
if [ "$EUID" -ne 0 ]; then 
    echo "❌ Script muss als root ausgeführt werden"
    echo "Verwende: sudo $0"
    exit 1
fi

# Prüfe ob User 'pvs' existiert
if ! id "pvs" &>/dev/null; then
    echo "❌ User 'pvs' existiert nicht!"
    echo "Führe zuerst setup-server.sh aus oder erstelle den User manuell."
    exit 1
fi

# User 'deploy' erstellen (falls nicht vorhanden)
echo "👤 Erstelle User 'deploy'..."
if ! id "deploy" &>/dev/null; then
    useradd -m -s /bin/bash deploy
    echo "✅ User 'deploy' erstellt"
else
    echo "ℹ️  User 'deploy' existiert bereits"
fi

# deploy zur sudo-Gruppe hinzufügen
echo "🔐 Konfiguriere sudo-Rechte..."
usermod -aG sudo deploy

# Sudo-Konfiguration für deploy (nur für pvs-Befehle)
echo "📝 Erstelle sudo-Konfiguration..."
cat > /etc/sudoers.d/deploy <<'EOF'
# deploy-User kann nur als pvs-User Befehle ausführen (ohne Passwort)
deploy ALL=(pvs) NOPASSWD: /bin/bash
deploy ALL=(pvs) NOPASSWD: ALL
EOF

# Sudoers-Syntax prüfen
if visudo -c -f /etc/sudoers.d/deploy; then
    echo "✅ Sudo-Konfiguration ist gültig"
else
    echo "❌ Fehler in sudo-Konfiguration!"
    rm -f /etc/sudoers.d/deploy
    exit 1
fi

# SSH-Verzeichnis für deploy erstellen
echo "📁 Erstelle SSH-Verzeichnis..."
mkdir -p /home/deploy/.ssh
chmod 700 /home/deploy/.ssh
chown -R deploy:deploy /home/deploy/.ssh

echo ""
echo "✅ deploy-User Setup abgeschlossen!"
echo ""
echo "📝 Nächste Schritte:"
echo "===================="
echo ""
echo "1. 🔑 Public SSH-Key für deploy-User hinzufügen:"
echo "   sudo -u deploy nano /home/deploy/.ssh/authorized_keys"
echo "   # Füge deinen öffentlichen SSH-Key hinzu"
echo "   # Dann: chmod 600 /home/deploy/.ssh/authorized_keys"
echo ""
echo "2. 🧪 Teste die Verbindung:"
echo "   ssh -i /path/to/private/key deploy@$(hostname -I | awk '{print $1}') 'sudo -u pvs whoami'"
echo "   # Sollte 'pvs' ausgeben"
echo ""
echo "3. 🔐 GitHub Secrets konfigurieren:"
echo "   - HETZNER_USER = 'deploy'"
echo "   - HETZNER_SSH_KEY = Private Key für deploy-User"
echo ""
echo "4. ✅ Teste Deployment über GitHub Actions"
echo ""



