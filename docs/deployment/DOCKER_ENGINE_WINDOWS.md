# Docker Engine Setup auf Windows 11 (ohne Docker Desktop)

Dieser Guide zeigt dir, wie du Docker Desktop entfernst und nur die Docker Engine über WSL2 installierst. Zusätzlich richten wir Portainer CE als kostenfreies Web-UI ein.

## 🎯 Warum Docker Engine ohne Desktop?

- ✅ **Weniger Ressourcen**: Keine GUI-Anwendung, die im Hintergrund läuft
- ✅ **Mehr Kontrolle**: Direkter Zugriff auf die Engine ohne zusätzliche Abstraktion
- ✅ **Kostenfrei & Open Source**: Keine Lizenzbeschränkungen wie bei Docker Desktop
- ✅ **Besser für Development**: Direkte CLI-Nutzung, Scripting-freundlich

## 📋 Voraussetzungen

- Windows 11
- Administrator-Rechte
- WSL2 bereits installiert (oder wird im Guide installiert)
- PowerShell (als Administrator)

## 🚀 Schritt 1: Docker Desktop entfernen

### Option A: Über die Systemsteuerung

1. **Windows + R** drücken → `appwiz.cpl` eingeben
2. **Docker Desktop** suchen
3. **Deinstallieren** klicken
4. Setup-Assistenten folgen

### Option B: Über PowerShell

```powershell
# Als Administrator ausführen
Get-Package "*docker*" | Uninstall-Package

# Oder spezifisch Docker Desktop
Get-Package "Docker Desktop" | Uninstall-Package
```

### Option C: Manuelle Bereinigung (falls nötig)

```powershell
# Docker Desktop Ordner entfernen
Remove-Item -Path "$env:ProgramFiles\Docker" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$env:ProgramFiles\Docker\Docker" -Recurse -Force -ErrorAction SilentlyContinue

# Docker Desktop AppData entfernen
Remove-Item -Path "$env:APPDATA\Docker" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$env:LOCALAPPDATA\Docker" -Recurse -Force -ErrorAction SilentlyContinue

# Registry-Bereinigung (optional, vorsichtig!)
# Nur wenn Docker Desktop nicht vollständig deinstalliert wurde
# reg delete "HKCU\Software\Docker Inc." /f
```

**✅ Prüfen ob entfernt:**

```powershell
docker --version
# Sollte eine Fehlermeldung geben, wenn Docker Desktop entfernt wurde
```

## 🔧 Schritt 2: WSL2 installieren & konfigurieren

### WSL2 Installation

```powershell
# Als Administrator ausführen

# WSL2 aktivieren
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart

# System neu starten (wichtig!)
Write-Host "System muss neu gestartet werden!" -ForegroundColor Yellow
Write-Host "Nach dem Neustart: wsl --update in PowerShell ausführen" -ForegroundColor Yellow
Restart-Computer
```

**Nach dem Neustart:**

```powershell
# WSL2 auf neueste Version aktualisieren
wsl --update

# Standard-Version auf WSL2 setzen
wsl --set-default-version 2

# Prüfen
wsl --status
```

### Linux-Distribution installieren

```powershell
# Verfügbare Distributionen anzeigen
wsl --list --online

# Ubuntu installieren (empfohlen)
wsl --install -d Ubuntu

# Oder eine andere Distribution:
# wsl --install -d Debian
# wsl --install -d openSUSE-Leap-15-4
```

**Nach der Installation:**

1. WSL öffnen (Ubuntu im Startmenü suchen)
2. Username und Passwort für den WSL-User anlegen
3. Initiales Update durchführen:

```bash
sudo apt update && sudo apt upgrade -y
```

## 🐳 Schritt 3: Docker Engine in WSL2 installieren

### In WSL2 (Ubuntu) ausführen:

```bash
# Als WSL-User (nicht als root!)

# Alte Docker-Versionen entfernen (falls vorhanden)
sudo apt-get remove docker docker-engine docker.io containerd runc

# Docker Repository hinzufügen
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg lsb-release

# Docker GPG Key hinzufügen
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Repository hinzufügen
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker Engine installieren
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Docker Compose V2 ist bereits enthalten (als Plugin)
```

### Docker Service konfigurieren

```bash
# Docker Service starten
sudo service docker start

# Service automatisch starten (bei WSL-Start)
# Hinzufügen in ~/.bashrc oder ~/.zshrc:
echo 'sudo service docker start > /dev/null 2>&1' >> ~/.bashrc

# Docker ohne sudo nutzen (optional, aber empfohlen)
sudo usermod -aG docker $USER

# Neu einloggen oder Gruppen neu laden
newgrp docker

# Testen
docker --version
docker compose version
sudo docker run hello-world
```

### Docker ohne sudo nutzen (Wichtig!)

Standardmäßig benötigst du `sudo` für Docker-Befehle. Um das zu vermeiden:

```bash
# User zur docker-Gruppe hinzufügen
sudo usermod -aG docker $USER

# Aktueller Session neu laden
newgrp docker

# Jetzt sollte es ohne sudo funktionieren
docker run hello-world
```

**⚠️ Hinweis:** Wenn WSL neu gestartet wird, muss der Docker-Service manuell gestartet werden. Automatisierung weiter unten.

## 🔄 Schritt 4: Docker von Windows aus nutzen

### Docker CLI in Windows installieren

Du kannst Docker-Befehle auch direkt aus Windows PowerShell ausführen, wenn die Docker CLI installiert ist:

```powershell
# Docker CLI für Windows herunterladen
# Von: https://download.docker.com/win/static/stable/x86_64/

# Oder über Chocolatey (wenn installiert):
choco install docker-cli

# Oder über Scoop (wenn installiert):
scoop install docker
```

**Alternative:** Nutze Docker-Befehle über WSL aus Windows:

```powershell
# Docker-Befehle über WSL ausführen
wsl docker --version
wsl docker ps
wsl docker compose up -d

# Oder WSL direkt öffnen und dort arbeiten
wsl
```

### Docker Context konfigurieren (Optional)

Falls du Docker CLI in Windows installiert hast, kannst du einen Context für WSL2 erstellen:

```powershell
# Docker Context erstellen (falls Docker CLI installiert)
docker context create wsl2 --docker "host=unix://\\wsl$\Ubuntu\var\run\docker.sock"
docker context use wsl2
```

## 🎨 Schritt 5: Portainer CE installieren (Web-UI)

Portainer Community Edition ist ein kostenfreies Docker-Web-UI mit Apache 2.0 Lizenz - perfekt für Development! 🎉

### Installation über Docker Compose

Erstelle eine `docker-compose.portainer.yml` Datei:

```yaml
version: '3.8'

services:
  portainer:
    image: portainer/portainer-ce:latest
    container_name: portainer
    restart: unless-stopped
    security_opt:
      - no-new-privileges:true
    volumes:
      - /etc/localtime:/etc/localtime:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - portainer_data:/data
    ports:
      - "9000:9000"
    command: -H unix:///var/run/docker.sock

volumes:
  portainer_data:
```

**In WSL2 starten:**

```bash
# Portainer starten
docker compose -f docker-compose.portainer.yml up -d

# Status prüfen
docker compose -f docker-compose.portainer.yml ps

# Logs ansehen
docker compose -f docker-compose.portainer.yml logs -f
```

**Portainer aufrufen:**

```
http://localhost:9000
```

**Bei der ersten Anmeldung:**
1. Admin-User anlegen (Username & Passwort)
2. Environment wählen: **Docker** (nicht Docker Swarm)
3. Verbindung wählen: **Docker socket** → `/var/run/docker.sock`

### Alternative: Direkter Docker Run

```bash
docker run -d -p 9000:9000 --name portainer --restart=unless-stopped \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v portainer_data:/data \
  portainer/portainer-ce:latest
```

### Portainer stoppen/starten

```bash
# Stoppen
docker compose -f docker-compose.portainer.yml down

# Starten
docker compose -f docker-compose.portainer.yml up -d

# Neu starten
docker restart portainer
```

## ⚙️ Schritt 6: Docker Service automatisch starten

Da WSL2 den Docker-Service nicht automatisch startet, kannst du das automatisieren:

### Option A: .bashrc/.zshrc (Einfach)

```bash
# In WSL2 Terminal öffnen
nano ~/.bashrc

# Am Ende hinzufügen:
# Start Docker service if not running
if ! pgrep -x "dockerd" > /dev/null; then
    sudo service docker start > /dev/null 2>&1
fi
```

### Option B: Windows Task Scheduler (Professionell)

Erstelle eine PowerShell-Datei `start-docker-wsl.ps1`:

```powershell
# start-docker-wsl.ps1
wsl sudo service docker start
```

**Task Scheduler konfigurieren:**

1. **Windows + R** → `taskschd.msc`
2. **Task erstellen** → **Allgemein:**
   - Name: `Start Docker in WSL2`
   - Als: **"Unabhängig von Benutzeranmeldung"**
   - **"Mit höchsten Rechten ausführen"** aktivieren
3. **Trigger:**
   - **Bei Start des Computers**
   - **Bei Anmeldung** (optional)
4. **Aktion:**
   - Programm: `powershell.exe`
   - Argumente: `-ExecutionPolicy Bypass -File "C:\Pfad\zu\start-docker-wsl.ps1"`

### Option C: Systemd in WSL2 (Neuere WSL-Versionen)

Neuere WSL-Versionen unterstützen systemd:

```bash
# In /etc/wsl.conf hinzufügen:
sudo nano /etc/wsl.conf
```

```ini
[boot]
systemd=true
```

**WSL neu starten:**
```powershell
wsl --shutdown
wsl
```

Dann Docker Service aktivieren:
```bash
sudo systemctl enable docker
sudo systemctl start docker
```

## ✅ Verifizierung & Test

### Docker Engine testen

```bash
# In WSL2 ausführen:

# Docker Version prüfen
docker --version
docker compose version

# Docker Service Status
sudo service docker status

# Test-Container starten
docker run hello-world

# Container-Liste
docker ps -a

# Images auflisten
docker images
```

### Portainer testen

1. Browser öffnen: `http://localhost:9000`
2. Admin-Login durchführen
3. Container im Web-UI sehen
4. Test-Container über Portainer starten/stoppen

### Docker Compose testen

```bash
# Test docker-compose.yml erstellen
cat > test-compose.yml << EOF
version: '3.8'
services:
  nginx:
    image: nginx:alpine
    ports:
      - "8080:80"
EOF

# Container starten
docker compose -f test-compose.yml up -d

# Status prüfen
docker compose -f test-compose.yml ps

# Stoppen & aufräumen
docker compose -f test-compose.yml down
```

## 🔧 Tipps & Troubleshooting

### Docker Service startet nicht

```bash
# Service manuell starten
sudo service docker start

# Logs prüfen
sudo journalctl -u docker.service

# Docker Socket prüfen
ls -la /var/run/docker.sock
```

### Permission denied Fehler

```bash
# User zur docker-Gruppe hinzufügen
sudo usermod -aG docker $USER

# Neu einloggen
newgrp docker

# Oder Gruppen neu laden
exec su -l $USER
```

### Portainer kann Docker nicht verbinden

```bash
# Docker Socket-Berechtigung prüfen
ls -la /var/run/docker.sock

# Falls nötig: Berechtigung anpassen
sudo chmod 666 /var/run/docker.sock

# Oder: docker-Gruppe prüfen
groups
# Sollte "docker" enthalten
```

### WSL2 startet Docker nicht automatisch

- **Option 1:** `.bashrc` Script (siehe oben)
- **Option 2:** Windows Task Scheduler (siehe oben)
- **Option 3:** systemd nutzen (neuere WSL-Versionen)

### Port 9000 bereits belegt

```bash
# Anderen Prozess auf Port 9000 finden
sudo netstat -tulpn | grep 9000

# Port in docker-compose.portainer.yml ändern
# z.B. "9001:9000" statt "9000:9000"
```

### Docker Compose Befehle funktionieren nicht

```bash
# Docker Compose Plugin prüfen
docker compose version

# Falls nicht installiert:
sudo apt-get install docker-compose-plugin

# Oder V1 installieren (falls benötigt):
sudo apt-get install docker-compose
```

## 📚 Nützliche Ressourcen

- **Docker Engine Dokumentation**: https://docs.docker.com/engine/
- **WSL2 Dokumentation**: https://learn.microsoft.com/en-us/windows/wsl/
- **Portainer CE Dokumentation**: https://docs.portainer.io/
- **Docker Compose Dokumentation**: https://docs.docker.com/compose/

## 🎯 Zusammenfassung

1. ✅ Docker Desktop entfernt
2. ✅ WSL2 installiert & konfiguriert
3. ✅ Docker Engine in WSL2 installiert
4. ✅ Docker ohne sudo nutzbar
5. ✅ Portainer CE als Web-UI installiert
6. ✅ Docker Service automatisch startend

**Du hast jetzt eine schlanke, kostenfreie Docker-Umgebung ohne Docker Desktop!** 🚀

## 💡 Warum Portainer CE?

- ✅ **100% kostenfrei** - Community Edition
- ✅ **Apache 2.0 Lizenz** - Keine Beschränkungen für Development
- ✅ **Aktiv entwickelt** - Regelmäßige Updates
- ✅ **Umfangreiches Feature-Set** - Container, Images, Networks, Volumes verwalten
- ✅ **Multi-Environment Support** - Mehrere Docker-Environments verwalten
- ✅ **Web-basiert** - Keine Installation auf dem Client nötig

**Perfekt für Development ohne Lizenz-Stress!** 😊

