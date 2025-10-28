# SSH Key Setup für Windows 11 & Cursor

## Option 1: OpenSSH (bereits in Windows 11) ⭐ EMPFOHLEN

### SSH Key generieren

Öffne **PowerShell** oder **Windows Terminal** und führe aus:

```powershell
# Navigiere zum .ssh Verzeichnis
cd ~\.ssh

# SSH Key für Hetzner Deployment generieren
ssh-keygen -t ed25519 -C "github-actions-hetzner" -f hetzner_deploy

# Keine Passphrase eingeben (Enter drücken)
```

**Wichtig:** Drücke 2x Enter (keine Passphrase, sonst funktioniert GitHub Actions nicht)

### Public Key auf Server kopieren

```powershell
# Public Key anzeigen
cat ~\.ssh\hetzner_deploy.pub

# Public Key auf Server kopieren (manuell oder via SSH)
type ~\.ssh\hetzner_deploy.pub | ssh root@188.245.253.179 "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

### Private Key für GitHub Secret

```powershell
# Private Key anzeigen (für GitHub Secret)
type ~\.ssh\hetzner_deploy
```

**Kopiere den ganzen Inhalt** (inkl. `-----BEGIN OPENSSH PRIVATE KEY-----` und `-----END...`)

## Option 2: Windows Terminal (Empfohlen für bessere UX)

1. **Windows Terminal öffnen** (Win + X → Terminal)
2. Gleiche Befehle wie oben verwenden
3. Besserer Support für SSH/Kopieren

## Option 3: Git Bash (Falls Git installiert)

```bash
# In Git Bash
cd ~/.ssh
ssh-keygen -t ed25519 -C "github-actions-hetzner" -f hetzner_deploy

# Public Key auf Server
ssh-copy-id -i ~/.ssh/hetzner_deploy.pub root@188.245.253.179
```

## Cursor Integration

### SSH Key in Cursor nutzen

Cursor nutzt automatisch SSH-Keys aus `~/.ssh/` - keine extra Konfiguration nötig!

### SSH-Zugriff in Cursor Terminal

1. **Terminal öffnen**: `Ctrl + `` (Backtick)
2. **SSH verbinden**:
   ```powershell
   ssh -i ~\.ssh\hetzner_deploy root@188.245.253.179
   ```

## Verifikation

```powershell
# Test SSH-Zugriff (sollte ohne Passwort funktionieren)
ssh -i ~\.ssh\hetzner_deploy root@188.245.253.179 "echo 'SSH funktioniert!'"
```

## Troubleshooting

### "Permission denied (publickey)"

- Prüfe: `~\.ssh\hetzner_deploy.pub` wurde auf Server kopiert?
- Test: `ssh -v -i ~\.ssh\hetzner_deploy root@188.245.253.179`

### "ssh-keygen nicht gefunden"

- Windows Features aktivieren: Settings → Apps → Optional Features → OpenSSH Client aktivieren
- Oder Git Bash nutzen

### Dateien finden

```powershell
# SSH Keys anzeigen
dir ~\.ssh\

# Inhalt anzeigen
type ~\.ssh\hetzner_deploy.pub
```

## Nächste Schritte

1. ✅ SSH Key erstellt
2. ✅ Public Key auf Server kopiert
3. ✅ Private Key als GitHub Secret speichern
4. → Server-Setup starten!

