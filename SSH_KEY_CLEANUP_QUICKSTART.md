# SSH Key Cleanup - Quick Start Guide

## Problem
Private SSH key wurde versehentlich committet in:
- `GITHUB_SECRETS_EINFACH.md`
- `GITHUB_SECRETS.md`

## Quick Fix (Ohne Scripts)

### Option 1: Manuell Dateien ersetzen (Schnell)

```bash
# Backup erstellen
cp GITHUB_SECRETS_EINFACH.md GITHUB_SECRETS_EINFACH.md.backup
cp GITHUB_SECRETS.md GITHUB_SECRETS.md.backup

# SSH-Key entfernen (Linux/Mac/Git Bash)
sed -i 's/-----BEGIN OPENSSH PRIVATE KEY-----.*-----END OPENSSH PRIVATE KEY-----/[PRIVATE_SSH_KEY_REMOVED]/g' GITHUB_SECRETS_EINFACH.md
sed -i 's/-----BEGIN OPENSSH PRIVATE KEY-----.*-----END OPENSSH PRIVATE KEY-----/[PRIVATE_SSH_KEY_REMOVED]/g' GITHUB_SECRETS.md

# Oder in Windows Cursor: Suche & Ersetze
# Finde: -----BEGIN OPENSSH PRIVATE KEY-----.*-----END OPENSSH PRIVATE KEY-----
# Ersetze mit: [PRIVATE_SSH_KEY_REMOVED]
```

### Option 2: Git Filter-Branch (Entfernt aus Historie)

```bash
# Backup erstellen
git stash push -m "backup-before-cleanup"

# Git Filter-Branch
git filter-branch --force --index-filter "git rm --cached --ignore-unmatch GITHUB_SECRETS_EINFACH.md GITHUB_SECRETS.md || true" --prune-empty --tag-name-filter cat -- --all

# Cleanup
git for-each-ref --format="delete %(refname)" refs/original | git update-ref --stdin
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

## Neue SSH Key erstellen

```bash
# Windows
cd %USERPROFILE%\.ssh
ssh-keygen -t ed25519 -C "github-actions-hetzner" -f hetzner_deploy -N ""

# Public Key anzeigen
type hetzner_deploy.pub

# Private Key anzeigen (fuer GitHub Secret)
type hetzner_deploy
```

## Public Key auf Server kopieren

```bash
# Windows
type %USERPROFILE%\.ssh\hetzner_deploy.pub | ssh root@188.245.253.179 "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

## GitHub Secrets aktualisieren

1. Gehe zu: https://github.com/bbajor/pvs/settings/secrets/actions
2. Bearbeite `HETZNER_SSH_KEY`
3. Ersetze mit dem neuen Private Key

## Committen

```bash
git add .
git commit -m "security: Remove private SSH key from documentation"
git push
```

## Hinweise

- Der alte SSH-Key ist kompromittiert - verwende ihn nicht mehr!
- Erstelle einen neuen SSH-Key
- Teste die Verbindung: `ssh -i ~/.ssh/hetzner_deploy root@188.245.253.179`

