# Selbst-hosted GitHub Actions Runner Setup

Diese Anleitung führt dich Schritt für Schritt durch das Setup eines selbst-hosted Runners für automatisches lokales Deployment.

## 📋 Übersicht

Ein selbst-hosted Runner ermöglicht:
- ✅ **Sofortiges Deployment** nach Push zu `dev`
- ✅ **Keine öffentliche IP** nötig
- ✅ **Sicher** (läuft lokal)
- ✅ **Vollständige Kontrolle**

## 🚀 Schritt-für-Schritt Setup

### Schritt 1: Runner herunterladen

**Auf deinem lokalen Rechner:**

```bash
# Verzeichnis erstellen
cd ~
mkdir actions-runner && cd actions-runner

# Aktuelle Version herunterladen (Linux x64)
# Prüfe aktuelle Version: https://github.com/actions/runner/releases
curl -o actions-runner-linux-x64-2.311.0.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz

# Entpacken
tar xzf actions-runner-linux-x64-2.311.0.tar.gz

# Dateien prüfen
ls -la
```

**Für Windows:**
```powershell
# PowerShell als Administrator
cd C:\
mkdir actions-runner; cd actions-runner

# Runner herunterladen
Invoke-WebRequest -Uri https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-win-x64-2.311.0.zip -OutFile actions-runner.zip

# Entpacken
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD\actions-runner.zip", "$PWD")
```

**Für macOS:**
```bash
cd ~
mkdir actions-runner && cd actions-runner
curl -o actions-runner-osx-x64-2.311.0.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-osx-x64-2.311.0.tar.gz
tar xzf actions-runner-osx-x64-2.311.0.tar.gz
```

### Schritt 2: Runner-Token von GitHub holen

1. **Gehe zu GitHub Repository:**
   ```
   https://github.com/bbajor/pvs/settings/actions/runners/new
   ```

2. **Wähle Betriebssystem:**
   - Linux → Kopiere `configure.sh` und `run.sh` Befehle
   - Windows → Kopiere `config.cmd` Befehl
   - macOS → Kopiere `configure.sh` und `run.sh` Befehle

3. **Notiere den Token** (wird nur einmal angezeigt!)

**Oder via GitHub CLI:**
```bash
gh auth login
gh runner registration-token --repo bbajor/pvs
```

### Schritt 3: Runner konfigurieren

**Linux/macOS:**
```bash
cd ~/actions-runner

# Runner konfigurieren
./config.sh --url https://github.com/bbajor/pvs --token <DEIN_TOKEN_VON_SCHRITT_2>

# Antworten auf Fragen:
# → Runner name: [Enter für Default] oder z.B. "pvs-local-dev"
# → Labels: [Enter für Default] oder z.B. "self-hosted,linux,dev"
# → Work folder: [Enter für Default: _work]
```

**Windows:**
```powershell
cd C:\actions-runner

# Runner konfigurieren
.\config.cmd --url https://github.com/bbajor/pvs --token <DEIN_TOKEN>

# Gleiche Fragen wie oben
```

### Schritt 4: Runner testen

**Linux/macOS:**
```bash
# Runner im Vordergrund starten (zum Testen)
./run.sh
```

**Windows:**
```powershell
# Runner im Vordergrund starten
.\run.cmd
```

**Erwartete Ausgabe:**
```
√ Connected to GitHub

Runner listener started
```

### Schritt 5: Runner als Service installieren (Auto-Start)

**Linux (systemd):**
```bash
sudo ./svc.sh install
sudo ./svc.sh start
sudo ./svc.sh status
```

**Windows:**
```powershell
# PowerShell als Administrator
.\config.cmd --url https://github.com/bbajor/pvs --token <TOKEN> --runasservice

# Oder manuell:
.\svc.sh install
.\svc.sh start
```

**macOS (launchd):**
```bash
sudo ./svc.sh install
brew services start runner
```

### Schritt 6: Runner prüfen

1. **In GitHub:** 
   ```
   https://github.com/bbajor/pvs/settings/actions/runners
   ```

2. **Sollte zeigen:**
   - ✅ Runner Name (z.B. "pvs-local-dev")
   - ✅ Status: "Online" (grün)
   - ✅ Labels: self-hosted, ...

### Schritt 7: Environment-Variablen setzen (Optional)

**Falls Docker-Compose anderswo liegt:**
```bash
# In ~/.bashrc oder ~/.zshrc:
export PVS_LOCAL_PATH=~/pvs
export GITHUB_REPO_OWNER=bbajor

# Neu laden
source ~/.bashrc
```

**Oder direkt im Runner-Kontext:**
```bash
# Edit Runner-Service-Config
sudo nano /etc/systemd/system/actions.runner.*.service

# Füge Environment hinzu:
[Service]
Environment="PVS_LOCAL_PATH=/home/user/pvs"
Environment="GITHUB_REPO_OWNER=bbajor"

# Service neu laden
sudo systemctl daemon-reload
sudo systemctl restart actions.runner.*.service
```

## 🧪 Testen

### 1. Workflow testen

**Push zu dev Branch:**
```bash
git checkout dev
# Kleine Änderung machen
echo "# Test" >> README.md
git add README.md
git commit -m "test: Test Runner"
git push origin dev
```

### 2. GitHub Actions prüfen

1. Gehe zu: `https://github.com/bbajor/pvs/actions`
2. Klicke auf: **"Deploy Dev to Local Machine"**
3. Prüfe Logs:
   - ✅ Runner sollte genutzt werden: `Self-hosted runner`
   - ✅ Deployment-Logs sollten zeigen: `🔄 Pulling latest dev image...`

### 3. Lokal prüfen

```bash
cd ~/pvs  # Oder dein PVS-Verzeichnis
docker-compose -f docker-compose.dev.yml ps
docker-compose -f docker-compose.dev.yml logs pvs-app-dev
```

## 🔧 Troubleshooting

### Runner erscheint nicht online

```bash
# Runner-Status prüfen
cd ~/actions-runner
./run.sh  # Im Vordergrund starten, um Fehler zu sehen

# Oder Service-Status
sudo systemctl status actions.runner.*.service
```

### Permission-Denied bei Docker

```bash
# User zu docker-Gruppe hinzufügen
sudo usermod -aG docker $USER
newgrp docker

# Service neu starten
sudo systemctl restart actions.runner.*.service
```

### Workflow läuft nicht auf Runner

**Prüfe Runner-Labels:**
```bash
# Runner neu konfigurieren mit Labels
./config.sh --url https://github.com/bbajor/pvs --token <TOKEN> \
  --labels self-hosted,linux,dev

# Oder in GitHub UI Labels hinzufügen:
# Settings → Actions → Runners → Runner → Edit → Labels
```

**Workflow muss richtige Labels haben:**
```yaml
jobs:
  deploy-local:
    runs-on: [self-hosted, linux]  # Labels müssen passen!
```

### Runner-Token abgelaufen

```bash
# Neuen Token holen
# GitHub → Settings → Actions → Runners → Runner → Configure → Reset Token

# Runner neu konfigurieren
cd ~/actions-runner
./config.sh remove --token <ALTES_TOKEN>
./config.sh --url https://github.com/bbajor/pvs --token <NEUES_TOKEN>
./svc.sh restart
```

## 🔄 Runner aktualisieren

```bash
cd ~/actions-runner

# Aktuelle Version prüfen
cat RUNNER_VERSION

# Neue Version herunterladen
curl -o actions-runner.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz

# Backup alter Version
mv bin bin.backup
tar xzf actions-runner.tar.gz

# Runner starten
./svc.sh restart
```

## 📝 Nützliche Befehle

```bash
# Runner-Status
sudo systemctl status actions.runner.*.service

# Runner-Logs
journalctl -u actions.runner.*.service -f

# Runner stoppen
sudo systemctl stop actions.runner.*.service

# Runner starten
sudo systemctl start actions.runner.*.service

# Runner entfernen
cd ~/actions-runner
./config.sh remove --token <TOKEN>
sudo ./svc.sh stop
sudo ./svc.sh uninstall
```

## ✅ Checkliste

- [ ] Runner heruntergeladen
- [ ] Token von GitHub geholt
- [ ] Runner konfiguriert
- [ ] Runner getestet (läuft im Vordergrund)
- [ ] Runner als Service installiert
- [ ] Runner erscheint online in GitHub
- [ ] Test-Push zu dev gemacht
- [ ] Workflow läuft auf Runner
- [ ] Lokales Deployment erfolgreich

## 🎯 Nächste Schritte

Nach erfolgreichem Setup:

1. **Teste mit Push zu dev:**
   ```bash
   git push origin dev
   ```

2. **Prüfe GitHub Actions:**
   - Gehe zu: `https://github.com/bbajor/pvs/actions`
   - Workflow sollte automatisch auf deinem Runner laufen

3. **Prüfe lokales Deployment:**
   ```bash
   docker-compose -f docker-compose.dev.yml ps
   ```

Fertig! 🎉 Jeder Push zu `dev` deployed nun automatisch auf deinem lokalen Rechner.

