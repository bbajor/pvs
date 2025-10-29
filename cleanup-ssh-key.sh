#!/bin/bash

# SSH Key Cleanup Script
# Entfernt den privaten SSH-Key aus der Git-Historie

set -e

echo "🔐 SSH Key Cleanup Script"
echo "========================="
echo ""

# Backup erstellen
echo "📦 Erstelle Backup des aktuellen Zustands..."
git stash push -m "backup-before-ssh-cleanup-$(date +%Y%m%d-%H%M%S)"

# Prüfe ob wir in einem Git-Repository sind
if [ ! -d ".git" ]; then
    echo "❌ Fehler: Nicht in einem Git-Repository!"
    exit 1
fi

echo "🔍 Prüfe aktuelle Branch..."
CURRENT_BRANCH=$(git branch --show-current)
echo "Aktueller Branch: $CURRENT_BRANCH"

# Prüfe ob es uncommitted changes gibt
if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "⚠️  Warnung: Es gibt uncommitted changes. Diese werden gestasht."
    git stash push -m "uncommitted-changes-before-cleanup"
fi

echo ""
echo "🎯 Optionen für SSH-Key Cleanup:"
echo "1) BFG Repo-Cleaner (empfohlen - schneller und sicherer)"
echo "2) Git Filter-Branch (eingebaut, aber langsamer)"
echo "3) Nur Dateien ersetzen (ohne Historie-Cleanup)"
echo ""

read -p "Wähle Option (1-3): " choice

case $choice in
    1)
        echo "🚀 BFG Repo-Cleaner Option gewählt"
        cleanup_with_bfg
        ;;
    2)
        echo "🔧 Git Filter-Branch Option gewählt"
        cleanup_with_filter_branch
        ;;
    3)
        echo "📝 Nur Dateien ersetzen"
        replace_files_only
        ;;
    *)
        echo "❌ Ungültige Option. Script beendet."
        exit 1
        ;;
esac

cleanup_with_bfg() {
    echo ""
    echo "📥 Installiere BFG Repo-Cleaner..."
    
    # Prüfe ob Java installiert ist (BFG benötigt Java)
    if ! command -v java &> /dev/null; then
        echo "❌ Java ist nicht installiert. BFG benötigt Java."
        echo "Installiere Java mit: sudo apt-get install openjdk-11-jre"
        exit 1
    fi
    
    # BFG herunterladen
    if [ ! -f "bfg.jar" ]; then
        echo "⬇️  Lade BFG herunter..."
        wget -O bfg.jar https://repo1.maven.org/maven2/com/madgag/bfg/1.14.0/bfg-1.14.0.jar
    fi
    
    echo "🧹 Führe BFG Cleanup aus..."
    
    # Erstelle eine Liste der zu entfernenden Keys
    cat > keys-to-remove.txt << 'EOF'
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
QyNTUxOQAAACCFH5rQb56Zcrc3rF3Ip5fV0nJ9lyiX6RVg18yD6Q+v9QAAAKBELcUvRC3F
LwAAAAtzc2gtZWQyNTUxOQAAACCFH5rQb56Zcrc3rF3Ip5fV0nJ9lyiX6RVg18yD6Q+v9Q
AAAEDvUABOhWu8o3UTxluFJViuC/UMjJATT2hPqSvDSE9LZoUfmtBvnplytzesXcinl9XS
cn2XKJfpFWDXzIPpD6/1AAAAFmdpdGh1Yi1hY3Rpb25zLWhldHpuZXIBAgMEBQYH
-----END OPENSSH PRIVATE KEY-----
EOF
    
    # BFG ausführen
    java -jar bfg.jar --replace-text keys-to-remove.txt .
    
    # Git cleanup
    git reflog expire --expire=now --all
    git gc --prune=now --aggressive
    
    echo "✅ BFG Cleanup abgeschlossen!"
    ;;
}

cleanup_with_filter_branch() {
    echo ""
    echo "🔧 Führe Git Filter-Branch aus..."
    
    # Erstelle temporäre Datei mit dem zu ersetzenden Key
    cat > /tmp/ssh_key_to_remove.txt << 'EOF'
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
QyNTUxOQAAACCFH5rQb56Zcrc3rF3Ip5fV0nJ9lyiX6RVg18yD6Q+v9QAAAKBELcUvRC3F
LwAAAAtzc2gtZWQyNTUxOQAAACCFH5rQb56Zcrc3rF3Ip5fV0nJ9lyiX6RVg18yD6Q+v9Q
AAAEDvUABOhWu8o3UTxluFJViuC/UMjJATT2hPqSvDSE9LZoUfmtBvnplytzesXcinl9XS
cn2XKJfpFWDXzIPpD6/1AAAAFmdpdGh1Yi1hY3Rpb25zLWhldHpuZXIBAgMEBQYH
-----END OPENSSH PRIVATE KEY-----
EOF
    
    # Erstelle Ersatz-Text
    REPLACEMENT="[PRIVATE_SSH_KEY_ENTFERNT_$(date +%Y%m%d)]"
    
    echo "🔄 Ersetze SSH-Key in Git-Historie..."
    
    # Filter-Branch ausführen
    git filter-branch --force --index-filter \
        "git rm --cached --ignore-unmatch GITHUB_SECRETS_EINFACH.md GITHUB_SECRETS.md || true" \
        --prune-empty --tag-name-filter cat -- --all
    
    # Cleanup
    git for-each-ref --format="delete %(refname)" refs/original | git update-ref --stdin
    git reflog expire --expire=now --all
    git gc --prune=now --aggressive
    
    echo "✅ Git Filter-Branch Cleanup abgeschlossen!"
    ;;
}

replace_files_only() {
    echo ""
    echo "📝 Ersetze SSH-Key in aktuellen Dateien..."
    
    # Erstelle Backup der betroffenen Dateien
    cp GITHUB_SECRETS_EINFACH.md GITHUB_SECRETS_EINFACH.md.backup
    cp GITHUB_SECRETS.md GITHUB_SECRETS.md.backup
    
    # Ersetze den SSH-Key in den Dateien
    REPLACEMENT="[PRIVATE_SSH_KEY_ENTFERNT_$(date +%Y%m%d)]"
    
    # SSH-Key in GITHUB_SECRETS_EINFACH.md ersetzen
    sed -i "s/-----BEGIN OPENSSH PRIVATE KEY-----.*-----END OPENSSH PRIVATE KEY-----/$REPLACEMENT/s" GITHUB_SECRETS_EINFACH.md
    
    # SSH-Key in GITHUB_SECRETS.md ersetzen
    sed -i "s/-----BEGIN OPENSSH PRIVATE KEY-----.*-----END OPENSSH PRIVATE KEY-----/$REPLACEMENT/s" GITHUB_SECRETS.md
    
    echo "✅ Dateien aktualisiert!"
    echo "⚠️  Wichtig: Der SSH-Key ist noch in der Git-Historie!"
    echo "   Verwende Option 1 oder 2 für vollständige Entfernung."
    ;;
}

echo ""
echo "🎉 Cleanup abgeschlossen!"
echo ""
echo "📋 Nächste Schritte:"
echo "1. Prüfe die geänderten Dateien: git diff"
echo "2. Committe die Änderungen: git add . && git commit -m 'security: Remove private SSH key from documentation'"
echo "3. Erstelle einen neuen SSH-Key für das Deployment"
echo "4. Aktualisiere die GitHub Secrets mit dem neuen Key"
echo ""
echo "🔐 Sicherheitshinweise:"
echo "- Der alte SSH-Key sollte als kompromittiert betrachtet werden"
echo "- Erstelle einen neuen SSH-Key für das Deployment"
echo "- Aktualisiere alle Server mit dem neuen Public Key"
echo ""