#!/bin/bash
# Repository Reorganisations-Script
# Fuehrt die geplante Strukturierung des Repositories durch

set -e

echo "Repository Reorganisation"
echo "=============================="
echo ""

# Non-Interactive Modus wenn FORCE=1 gesetzt
if [ "$FORCE" != "1" ]; then
    echo "WICHTIG: Dieses Script verschiebt und konsolidiert Dateien!"
    echo "   Erstelle ein Backup oder commit erst, bevor du fortfaehrst!"
    echo ""
    read -p "Fortfahren? (y/N): " confirm
    
    if [ "$confirm" != "y" ]; then
        echo "Abgebrochen."
        exit 0
    fi
else
    echo "Non-Interactive Modus aktiviert (FORCE=1)"
    echo ""
fi

# Neue Verzeichnisse erstellen
echo ""
echo "Erstelle neue Verzeichnisse..."
mkdir -p docs/security
mkdir -p docs/administration
mkdir -p scripts/deployment
mkdir -p scripts/security
mkdir -p scripts/utilities

# Security-Dokumentation
echo "Reorganisiere Security-Dokumentation..."
if [ -f "SSH_KEY_CLEANUP_ANLEITUNG.md" ]; then
    mv SSH_KEY_CLEANUP_ANLEITUNG.md docs/security/SSH_KEY_CLEANUP.md
fi
if [ -f "SSH_SETUP_ANLEITUNG.md" ]; then
    mv SSH_SETUP_ANLEITUNG.md docs/security/SSH_KEY_SETUP.md
fi

# Security-Scripts
echo "Verschiebe Security-Scripts..."
if [ -f "cleanup-ssh-key.sh" ]; then
    mv cleanup-ssh-key.sh scripts/security/
fi
if [ -f "generate-new-ssh-key.sh" ]; then
    mv generate-new-ssh-key.sh scripts/security/
fi
if [ -f "generate-new-ssh-key.ps1" ]; then
    mv generate-new-ssh-key.ps1 scripts/security/
fi
if [ -f "ssh-key-setup.ps1" ]; then
    mv ssh-key-setup.ps1 scripts/security/ 2>/dev/null || rm ssh-key-setup.ps1
fi

# Deployment-Scripts
echo "Verschiebe Deployment-Scripts..."
if [ -f "setup-server.sh" ]; then
    mv setup-server.sh scripts/deployment/
fi
if [ -f "init-databases.sh" ]; then
    mv init-databases.sh scripts/deployment/
fi

# Utilities
echo "Verschiebe Utility-Scripts..."
if [ -f "check-ip.sh" ]; then
    mv check-ip.sh scripts/utilities/
fi

# Konsolidierte Dokumentation erstellen
echo ""
echo "Konsolidiere Dokumentation..."

# Veraltete Dateien markieren (nicht direkt loeschen)
echo ""
echo "Veraltete Dateien gefunden (kannst du nach Pruefung loeschen):"
[ -f "SSH_KEY_CLEANUP_QUICKSTART.md" ] && echo "  - SSH_KEY_CLEANUP_QUICKSTART.md (Inhalt in docs/security/SSH_KEY_CLEANUP.md)"
[ -f "SSH_KEY_CLEANUP_WINDOWS.md" ] && echo "  - SSH_KEY_CLEANUP_WINDOWS.md (Inhalt in docs/security/SSH_KEY_SETUP.md)"
[ -f "setup-ssh-windows.md" ] && echo "  - setup-ssh-windows.md (Inhalt in docs/security/SSH_KEY_SETUP.md)"
[ -f "SECURITY_CLEANUP_CHECKLIST.md" ] && echo "  - SECURITY_CLEANUP_CHECKLIST.md (kann in docs/security/SECURITY_INCIDENT.md integriert werden)"
[ -f "CLEANUP_MANUAL.md" ] && echo "  - CLEANUP_MANUAL.md (Inhalt in docs/security/SSH_KEY_CLEANUP.md)"
[ -f "server-setup-nach-passwort.md" ] && echo "  - server-setup-nach-passwort.md (veraltet)"
[ -f "SETUP_CHECKLIST.md" ] && echo "  - SETUP_CHECKLIST.md (Inhalt in docs/deployment/HETZNER_COMPLETE_SETUP.md)"
[ -f "DEPLOYMENT_START.md" ] && echo "  - DEPLOYMENT_START.md (kann entfernt werden)"
[ -f "START_HIER.md" ] && echo "  - START_HIER.md (README.md zeigt den Einstieg)"

echo ""
echo "Reorganisation abgeschlossen!"
echo ""
echo "Naechste Schritte:"
echo "1. Pruefe die verschobenen Dateien: git status"
echo "2. Entferne veraltete Dateien nach Pruefung"
echo "3. Aktualisiere README.md mit neuen Pfaden"
echo "4. Committe die Aenderungen: git add . && git commit -m 'refactor: Reorganize repository structure'"

