# Automatisches Deployment auf lokalen Rechner

Wenn du zu `dev` Branch pushst, kann automatisch auf deinem lokalen Rechner deployed werden.

## 🎯 Optionen

### Option 1: SSH von GitHub Actions (Empfohlen)

GitHub Actions kann per SSH auf deinen lokalen Rechner zugreifen und automatisch deployen.

#### Voraussetzungen

1. **SSH-Zugriff auf lokalen Rechner** (von GitHub aus erreichbar)
   - Entweder: Öffentliche IP oder Tunnel (z.B. ngrok, Tailscale)
   - Oder: Selbst-hosted GitHub Actions Runner (beste Option)

2. **GitHub Secrets konfigurieren:**
   ```
   LOCAL_MACHINE_HOST=deine-ip-oder-hostname
   LOCAL_MACHINE_USER=dein-username
   LOCAL_MACHINE_SSH_KEY=<privater-ssh-key>
   LOCAL_MACHINE_SSH_PORT=22 (optional)
   LOCAL_MACHINE_PVS_PATH=/path/to/pvs (optional, default: ~/pvs)
   ```

#### Setup

**A) Selbst-hosted GitHub Actions Runner (Beste Option)**

```bash
# Auf deinem lokalen Rechner:
# 1. Runner herunterladen
mkdir actions-runner && cd actions-runner
curl -o actions-runner-linux-x64-2.311.0.tar.gz -L https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz
tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz

# 2. Runner konfigurieren
./config.sh --url https://github.com/bbajor/pvs --token <TOKEN>

# Token holen: GitHub → Settings → Actions → Runners → New runner

# 3. Runner starten
./run.sh

# 4. Als Service installieren (optional, für Auto-Start)
sudo ./svc.sh install
sudo ./svc.sh start
```

**B) Öffentlicher SSH-Zugriff (mit Tunnel)**

Falls dein Rechner keine öffentliche IP hat, nutze einen Tunnel:

```bash
# Option 1: ngrok
ngrok tcp 22

# Option 2: Tailscale (empfohlen für Sicherheit)
# Installiere Tailscale, dann nutze Tailscale-IP

# Option 3: SSH Reverse Tunnel
ssh -R 2222:localhost:22 user@public-server
```

Dann in GitHub Secrets:
```
LOCAL_MACHINE_HOST=<tunnel-hostname>
LOCAL_MACHINE_SSH_PORT=2222
```

#### Workflow

Nach Push zu `dev`:
1. "Dev Branch CI & Build" läuft → Image wird gebaut
2. "Deploy Dev to Local Machine" läuft automatisch (per SSH)
3. Lokaler Rechner pulled Image und deployed

### Option 2: Lokaler Service (Polling)

Ein lokaler Service prüft regelmäßig auf neue Images.

#### Mit Watchtower (Automatisch)

```bash
# Watchtower installieren
docker run -d \
  --name watchtower \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v ~/pvs/docker-compose.dev.yml:/docker-compose.dev.yml \
  containrrr/watchtower \
  --interval 300 \
  --label-enable \
  --include-restart

# Docker Compose Labels für Watchtower
# In docker-compose.dev.yml:
labels:
  - "com.centurylinklabs.watchtower.enable=true"
```

#### Mit Custom Script (Cron-Job)

```bash
# Script ausführbar machen
chmod +x scripts/local/auto-update-dev.sh

# Cron-Job einrichten (alle 5 Minuten prüfen)
crontab -e

# Füge hinzu:
*/5 * * * * /path/to/pvs/scripts/local/auto-update-dev.sh >> /tmp/pvs-auto-update.log 2>&1

# Oder nutze systemd Timer (moderner)
```

**Systemd Timer Setup:**

```bash
# Erstelle Service-Datei
cat > ~/.config/systemd/user/pvs-auto-update.service <<EOF
[Unit]
Description=Auto-Update PVS Dev Environment

[Service]
Type=oneshot
ExecStart=/path/to/pvs/scripts/local/auto-update-dev.sh
WorkingDirectory=/path/to/pvs
Environment="GITHUB_REPO_OWNER=bbajor"
Environment="PVS_LOCAL_PATH=/path/to/pvs"
EOF

# Erstelle Timer
cat > ~/.config/systemd/user/pvs-auto-update.timer <<EOF
[Unit]
Description=Auto-Update PVS Dev Timer

[Timer]
OnBootSec=5min
OnUnitActiveSec=5min

[Install]
WantedBy=timers.target
EOF

# Timer aktivieren
systemctl --user enable pvs-auto-update.timer
systemctl --user start pvs-auto-update.timer
```

### Option 3: GitHub Webhook (Erweitert)

Ein lokaler Webhook-Server empfängt Events von GitHub.

```bash
# Beispiel: Node.js Webhook-Server
# Siehe: scripts/local/webhook-server.js (wird erstellt falls gewünscht)
```

## 🚀 Schnellstart (Selbst-hosted Runner - Empfohlen)

```bash
# 1. Auf lokalem Rechner: Runner installieren
cd ~
mkdir -p actions-runner && cd actions-runner
curl -o actions-runner.tar.gz -L https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz
tar xzf actions-runner.tar.gz

# 2. Token holen:
# GitHub → Settings → Actions → Runners → New runner → Linux

# 3. Runner konfigurieren
./config.sh --url https://github.com/bbajor/pvs --token <DEIN_TOKEN>
./run.sh

# 4. In docker-compose.dev.yml Labels hinzufügen für Watchtower (optional)
```

## 📋 Vergleich der Optionen

| Option | Pro | Contra |
|--------|-----|--------|
| **Selbst-hosted Runner** | ✅ Sofortige Deployment<br>✅ Keine öffentliche IP nötig<br>✅ Sicher | ❌ Runner muss laufen<br>❌ Einrichtung etwas komplexer |
| **SSH via Tunnel** | ✅ Einfach<br>✅ Direkt | ❌ Öffentliche IP/Tunnel nötig<br>❌ Sicherheits-Überlegungen |
| **Watchtower/Polling** | ✅ Automatisch<br>✅ Keine GitHub-Konfiguration | ❌ Verzögerung (Polling)<br>❌ Läuft ständig |

## 🔧 Troubleshooting

### Runner startet nicht

```bash
# Logs prüfen
cat ~/actions-runner/_diag/Runner_*.log

# Runner neu starten
cd ~/actions-runner
./run.sh
```

### SSH-Verbindung fehlgeschlagen

```bash
# Teste SSH manuell
ssh -i ~/.ssh/github_deploy user@localhost

# Prüfe GitHub Secrets
# GitHub → Settings → Secrets → Actions
```

### Image Pull fehlgeschlagen

```bash
# Manuell testen
docker pull ghcr.io/bbajor/pvs:dev-latest

# Falls nicht öffentlich: Login nötig
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
```

## ⚙️ Konfiguration

### Environment-Variablen für Script

```bash
export GITHUB_REPO_OWNER=bbajor
export PVS_LOCAL_PATH=~/pvs
```

### Docker Compose Labels für Watchtower

```yaml
services:
  pvs-app-dev:
    labels:
      - "com.centurylinklabs.watchtower.enable=true"
      - "com.centurylinklabs.watchtower.lifecycle.pre-update=./scripts/local/pre-update.sh"
```

## 🎯 Empfehlung

**Für Entwicklung:** Selbst-hosted GitHub Actions Runner
- Sofortiges Deployment nach Push
- Keine Wartezeit durch Polling
- Sicher (kein öffentlicher SSH nötig)

**Für einfachen Setup:** Watchtower mit Cron
- Minimaler Setup-Aufwand
- Funktioniert out-of-the-box
- 5-10 Minuten Verzögerung akzeptabel

