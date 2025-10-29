#!/bin/bash

# Neuer SSH-Key Generator für Hetzner Deployment
# Erstellt einen neuen SSH-Key und bereitet ihn für GitHub Secrets vor

set -e

echo "🔑 Neuer SSH-Key Generator"
echo "=========================="
echo ""

# Prüfe ob .ssh Verzeichnis existiert
if [ ! -d "$HOME/.ssh" ]; then
    echo "📁 Erstelle .ssh Verzeichnis..."
    mkdir -p "$HOME/.ssh"
    chmod 700 "$HOME/.ssh"
fi

# Generiere neuen SSH-Key
KEY_NAME="hetzner_deploy_$(date +%Y%m%d_%H%M%S)"
KEY_PATH="$HOME/.ssh/$KEY_NAME"

echo "🔧 Generiere neuen SSH-Key..."
echo "Key-Name: $KEY_NAME"
echo "Key-Pfad: $KEY_PATH"
echo ""

# SSH-Key generieren (ohne Passphrase für GitHub Actions)
ssh-keygen -t ed25519 -C "github-actions-hetzner-$(date +%Y%m%d)" -f "$KEY_PATH" -N ""

echo ""
echo "✅ SSH-Key erfolgreich generiert!"
echo ""

# Zeige Public Key
echo "📋 Public Key (für Server):"
echo "============================"
cat "${KEY_PATH}.pub"
echo ""

# Zeige Private Key
echo "🔐 Private Key (für GitHub Secret):"
echo "===================================="
cat "$KEY_PATH"
echo ""

# Erstelle Anweisungen
echo "📝 Nächste Schritte:"
echo "===================="
echo ""
echo "1. 🔑 Public Key auf Server kopieren:"
echo "   ssh-copy-id -i ${KEY_PATH}.pub root@188.245.253.179"
echo "   ODER manuell:"
echo "   cat ${KEY_PATH}.pub | ssh root@188.245.253.179 'mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys'"
echo ""
echo "2. 🔐 Private Key als GitHub Secret speichern:"
echo "   - Gehe zu: https://github.com/bbajor/pvs/settings/secrets/actions"
echo "   - Bearbeite 'HETZNER_SSH_KEY'"
echo "   - Ersetze den Inhalt mit dem Private Key oben"
echo ""
echo "3. 🧪 Teste die Verbindung:"
echo "   ssh -i $KEY_PATH root@188.245.253.179 'echo \"SSH funktioniert!\"'"
echo ""
echo "4. 🗑️  Alten Key entfernen (falls vorhanden):"
echo "   rm ~/.ssh/hetzner_deploy*"
echo ""

# Erstelle eine .env Datei für lokale Tests
echo "💾 Erstelle .env Datei für lokale Tests..."
cat > .env.ssh-key << EOF
# SSH Key für Hetzner Deployment
HETZNER_SSH_KEY_PATH=$KEY_PATH
HETZNER_HOST=188.245.253.179
HETZNER_USER=root
EOF

echo "✅ .env.ssh-key Datei erstellt!"
echo ""
echo "⚠️  Wichtig:"
echo "- Bewahre den Private Key sicher auf"
echo "- Füge .env.ssh-key zu .gitignore hinzu"
echo "- Der alte SSH-Key sollte als kompromittiert betrachtet werden"
echo ""

# Prüfe ob .gitignore existiert und füge .env.ssh-key hinzu
if [ -f ".gitignore" ]; then
    if ! grep -q ".env.ssh-key" .gitignore; then
        echo "📝 Füge .env.ssh-key zu .gitignore hinzu..."
        echo ".env.ssh-key" >> .gitignore
        echo "✅ .gitignore aktualisiert!"
    fi
else
    echo "📝 Erstelle .gitignore mit .env.ssh-key..."
    echo ".env.ssh-key" > .gitignore
    echo "✅ .gitignore erstellt!"
fi

echo ""
echo "🎉 Setup abgeschlossen! Der neue SSH-Key ist bereit für das Deployment."