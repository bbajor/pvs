# 🔐 SSH Key Cleanup - Windows PowerShell Anleitung

## Problem
Der private SSH-Key wurde versehentlich in die Git-Historie committet und ist in folgenden Dateien sichtbar:
- `GITHUB_SECRETS_EINFACH.md`
- `GITHUB_SECRETS.md`

## ⚠️ Sicherheitsrisiko
- Der private SSH-Key ist in der Git-Historie gespeichert
- Jeder mit Repository-Zugriff kann den Key sehen
- Der Key sollte als kompromittiert betrachtet werden

## 🚀 Lösung (PowerShell)

### Voraussetzungen
- PowerShell (Windows 10/11)
- Git für Windows installiert
- OpenSSH Client (teil von Windows 10/11)

### Schritt 1: OpenSSH Client aktivieren (falls nötig)

```powershell
# PowerShell als Administrator ausführen
Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux
```

### Schritt 2: SSH-Key aus Git-Historie entfernen

```powershell
# PowerShell-Script ausführen (Rechte Maustaste → Mit PowerShell ausführen)
.\cleanup-ssh-key.ps1
```

**Optionen:**
1. **BFG Repo-Cleaner** (empfohlen) - Schnell und sicher, benötigt Java
2. **Git Filter-Branch** - Eingebaute Git-Funktionalität
3. **Nur Dateien ersetzen** - Schnell, aber Key bleibt in Historie

### Schritt 3: Neuen SSH-Key generieren

```powershell
# PowerShell-Script ausführen
.\generate-new-ssh-key.ps1
```

Das Script erstellt:
- ✅ Neuen SSH-Key im `%USERPROFILE%\.ssh\` Verzeichnis
- ✅ Zeigt Public Key für Server-Setup
- ✅ Zeigt Private Key für GitHub Secrets
- ✅ Erstellt `.env.ssh-key` für lokale Tests
- ✅ Fügt `.env.ssh-key` zu `.gitignore` hinzu

### Schritt 4: Public Key auf Server kopieren

**Option A: Automatisch (empfohlen)**
```powershell
# Teste zuerst die Verbindung (mit Passwort)
ssh root@188.245.253.179

# Dann kopiere den Public Key
ssh-copy-id -i $env:USERPROFILE\.ssh\hetzner_deploy_YYYYMMDD_HHMMSS.pub root@188.245.253.179
```

**Option B: Manuell**
```powershell
# Public Key anzeigen
Get-Content $env:USERPROFILE\.ssh\hetzner_deploy_YYYYMMDD_HHMMSS.pub

# Auf Server kopieren (überschreiben falls nötig)
type $env:USERPROFILE\.ssh\hetzner_deploy_YYYYMMDD_HHMMSS.pub | ssh root@188.245.253.179 'mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys'
```

### Schritt 5: GitHub Secrets aktualisieren

1. Gehe zu: https://github.com/bbajor/pvs/settings/secrets/actions
2. Bearbeite `HETZNER_SSH_KEY`
3. Kopiere den Private Key von der PowerShell-Ausgabe
4. Ersetze den Inhalt mit dem neuen Private Key
5. Speichere die Änderungen

### Schritt 6: Testen

```powershell
# SSH-Verbindung testen
ssh -i $env:USERPROFILE\.ssh\hetzner_deploy_YYYYMMDD_HHMMSS root@188.245.253.179 'echo "SSH funktioniert!"'

# GitHub Actions testen
# Gehe zu: GitHub → Actions → "Build and Push Docker Images (Hetzner)" → "Run workflow"
```

## 🔧 Manuelle Alternative

Falls die Scripts nicht funktionieren:

### 1. Dateien manuell bearbeiten

```powershell
# Ersetze SSH-Key in GITHUB_SECRETS_EINFACH.md
$content = Get-Content "GITHUB_SECRETS_EINFACH.md" -Raw
$content = $content -replace '(?s)-----BEGIN OPENSSH PRIVATE KEY-----(.*?)-----END OPENSSH PRIVATE KEY-----', '[PRIVATE_SSH_KEY_ENTFERNT]'
$content | Set-Content "GITHUB_SECRETS_EINFACH.md" -Encoding UTF8

# Ersetze SSH-Key in GITHUB_SECRETS.md
$content = Get-Content "GITHUB_SECRETS.md" -Raw
$content = $content -replace '(?s)-----BEGIN OPENSSH PRIVATE KEY-----(.*?)-----END OPENSSH PRIVATE KEY-----', '[PRIVATE_SSH_KEY_ENTFERNT]'
$content | Set-Content "GITHUB_SECRETS.md" -Encoding UTF8
```

### 2. Git Filter-Branch manuell

```powershell
# Erstelle Backup
git stash push -m "backup-before-cleanup"

# Filter-Branch ausführen
git filter-branch --force --index-filter `
    "git rm --cached --ignore-unmatch GITHUB_SECRETS_EINFACH.md GITHUB_SECRETS.md || true" `
    --prune-empty --tag-name-filter cat -- --all

# Cleanup
git for-each-ref --format="delete %(refname)" refs/original | git update-ref --stdin
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

### 3. Manuell SSH-Key generieren

```powershell
# Navigiere zum .ssh Verzeichnis
cd $env:USERPROFILE\.ssh

# SSH-Key generieren
ssh-keygen -t ed25519 -C "github-actions-hetzner" -f hetzner_deploy

# Public Key anzeigen
Get-Content $env:USERPROFILE\.ssh\hetzner_deploy.pub

# Private Key anzeigen
Get-Content $env:USERPROFILE\.ssh\hetzner_deploy
```

## ✅ Checkliste

- [ ] OpenSSH Client aktiviert
- [ ] SSH-Key aus Git-Historie entfernt
- [ ] Neuen SSH-Key generiert
- [ ] Public Key auf Server kopiert
- [ ] GitHub Secret aktualisiert
- [ ] SSH-Verbindung getestet
- [ ] GitHub Actions getestet
- [ ] Alten SSH-Key als kompromittiert markiert

## 🛡️ Sicherheitshinweise

- **Der alte SSH-Key ist kompromittiert** - verwende ihn nicht mehr
- Erstelle einen neuen SSH-Key für alle zukünftigen Deployments
- Überwache den Server auf verdächtige Aktivitäten
- Verwende `.env` Dateien für lokale Tests (nicht committen!)
- Bewahre Private Keys sicher auf

## 🆘 Troubleshooting

### "Permission denied (publickey)"
```powershell
# Verbindung mit Debug-Output testen
ssh -v -i $env:USERPROFILE\.ssh\hetzner_deploy root@188.245.253.179

# Prüfe ob Public Key korrekt auf Server kopiert wurde
ssh -i $env:USERPROFILE\.ssh\hetzner_deploy root@188.245.253.179 'cat ~/.ssh/authorized_keys'
```

### "ssh-keygen nicht gefunden"
```powershell
# Prüfe ob OpenSSH installiert ist
Get-WindowsCapability -Online | Where-Object Name -like 'OpenSSH*'

# Installiere OpenSSH Client
Add-WindowsCapability -Online -Name OpenSSH.Client~~~~0.0.1.0
```

### "Script kann nicht ausgeführt werden"
```powershell
# Ausführungsrichtlinie temporär ändern
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process

# Script ausführen
.\cleanup-ssh-key.ps1
```

### GitHub Actions schlägt fehl
1. Prüfe ob GitHub Secret korrekt gesetzt wurde
2. Teste SSH-Verbindung manuell
3. Prüfe Server-Logs
4. Schaue in GitHub Actions Logs

### Java nicht gefunden (für BFG)
```powershell
# Mit Chocolatey installieren
choco install openjdk

# ODER Download von Oracle:
# https://www.oracle.com/java/technologies/downloads/
```

## 📝 Windows-spezifische Tipps

### SSH-Keys in Windows
- SSH-Keys werden in `C:\Users\DeinName\.ssh\` gespeichert
- Benutze Git Bash für bessere SSH-Support
- PowerShell funktioniert, aber Git Bash ist manchmal einfacher

### Git Bash Alternative
```bash
# In Git Bash öffnen
cd ~/.ssh
ssh-keygen -t ed25519 -C "github-actions-hetzner" -f hetzner_deploy
ssh-copy-id -i ~/.ssh/hetzner_deploy.pub root@188.245.253.179
```

### Berechtigungen prüfen
```powershell
# SSH-Verzeichnis berechtigungen prüfen
icacls $env:USERPROFILE\.ssh

# SSH-Keys sollten nur vom Benutzer lesbar sein
```

## 📞 Support

Bei Problemen:
1. Prüfe die Logs in GitHub Actions
2. Teste SSH-Verbindung manuell
3. Überprüfe Server-Konfiguration
4. Erstelle neuen SSH-Key falls nötig
5. Nutze Git Bash falls PowerShell Probleme macht

