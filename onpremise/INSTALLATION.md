# Detaillierte Installationsanleitung

Diese Anleitung führt Schritt für Schritt durch die Installation von PVS OnPremise.

## Vorbereitung

### 1. Systemvoraussetzungen prüfen

#### Linux
```bash
# Betriebssystem-Informationen
cat /etc/os-release

# Verfügbarer Speicher
df -h

# Verfügbarer RAM
free -h

# Podman prüfen (falls bereits installiert)
podman --version
```

#### Windows
```powershell
# System-Informationen
systeminfo | Select-String "OS Name", "Total Physical Memory"

# Verfügbarer Speicher
Get-PSDrive C | Select-Object Used,Free

# Podman prüfen (falls bereits installiert)
podman --version
```

### 2. Podman installieren

#### Linux

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install -y podman podman-compose
```

**Fedora/RHEL/CentOS:**
```bash
sudo dnf install -y podman podman-compose
```

**Arch/Manjaro:**
```bash
sudo pacman -S podman podman-compose
```

**podman-compose (falls nicht verfügbar):**
```bash
pip3 install podman-compose
```

#### Windows

1. Lade Podman Desktop herunter: https://podman-desktop.io/
2. Installiere Podman Desktop
3. Starte Podman Desktop und warte, bis es vollständig gestartet ist
4. Prüfe die Installation:
   ```powershell
   podman --version
   ```

**podman-compose für Windows:**
```powershell
pip install podman-compose
```

## Installation

### Linux-Installation

#### Schritt 1: Repository klonen oder Dateien kopieren

```bash
# Option 1: Git-Repository klonen
cd /tmp
git clone <repository-url> pvs
cd pvs/onpremise

# Option 2: Dateien manuell kopieren
# Kopiere das gesamte 'onpremise'-Verzeichnis auf den Server
```

#### Schritt 2: Installer ausführen

```bash
# Als root ausführen
sudo bash install.sh
```

Der Installer:
- Prüft und installiert Podman/podman-compose falls nötig
- Erstellt Service-User `pvs`
- Erstellt Installations-Verzeichnis `/opt/pvs`
- Kopiert Konfigurationsdateien
- Generiert sichere Passwörter
- Installiert Systemd-Service für Auto-Start

#### Schritt 3: Konfiguration anpassen

```bash
# .env-Datei bearbeiten
sudo nano /opt/pvs/.env
```

**Wichtige Einstellungen:**

1. **Datenbank-Passwörter**: Sollten bereits generiert sein, können aber geändert werden
2. **SMTP-Konfiguration**: Falls E-Mail-Versand benötigt wird
3. **Ports**: Standard ist 8080, kann angepasst werden
4. **Whisper**: Lokaler Whisper-Service (benötigt mehr Ressourcen)

#### Schritt 4: Service starten

```bash
# Service starten
sudo systemctl start pvs-onpremise

# Status prüfen
sudo systemctl status pvs-onpremise

# Logs anzeigen
sudo journalctl -u pvs-onpremise -f
```

#### Schritt 5: Anwendung testen

```bash
# Health-Check
curl http://localhost:8080/actuator/health

# Im Browser öffnen
# http://localhost:8080
```

### Windows-Installation

#### Schritt 1: Dateien vorbereiten

```powershell
# PowerShell als Administrator öffnen
# Ins Installationsverzeichnis wechseln
cd C:\path\to\pvs\onpremise
```

#### Schritt 2: Installer ausführen

```powershell
.\install.ps1
```

Der Installer:
- Prüft Podman-Installation
- Erstellt Installations-Verzeichnis `C:\Program Files\PVS`
- Kopiert Konfigurationsdateien
- Generiert sichere Passwörter
- Erstellt Start/Stop/Status-Skripte
- Erstellt Windows Task für Auto-Start

#### Schritt 3: Konfiguration anpassen

```powershell
notepad "C:\Program Files\PVS\.env"
```

**Wichtige Einstellungen:** (siehe Linux-Installation)

#### Schritt 4: PVS starten

```powershell
cd "C:\Program Files\PVS"
.\start-pvs.bat
```

#### Schritt 5: Status prüfen

```powershell
.\status-pvs.bat

# Oder im Browser
# http://localhost:8080
```

## Post-Installation

### 1. Erste Anmeldung

1. Öffne die Anwendung im Browser: `http://localhost:8080`
2. Erstelle einen Admin-Account (falls noch nicht vorhanden)
3. Konfiguriere die Anwendung

### 2. SMTP konfigurieren (optional)

Falls E-Mail-Versand benötigt wird:

1. Bearbeite `/opt/pvs/.env` (Linux) oder `C:\Program Files\PVS\.env` (Windows)
2. Setze die SMTP-Variablen:
   ```
   SMTP_HOST=smtp.example.com
   SMTP_PORT=587
   SMTP_USERNAME=noreply@example.com
   SMTP_PASSWORD=your_password
   SMTP_FROM_ADDRESS=noreply@example.com
   ```
3. **WICHTIG**: Setze `SMTP_ENCRYPTION_KEY` (wird beim Installer generiert)
4. Starte die Container neu:
   ```bash
   # Linux
   sudo systemctl restart pvs-onpremise
   
   # Windows
   .\stop-pvs.bat
   .\start-pvs.bat
   ```

### 3. Whisper aktivieren (optional)

Für lokale Spracherkennung:

1. Bearbeite die `.env`-Datei:
   ```
   AI_WHISPER_LOCAL_ENABLED=true
   ```
2. Starte Container mit Whisper-Profil:
   ```bash
   # Linux
   sudo su - pvs
   cd /opt/pvs
   podman-compose -f podman-compose.onpremise.yml --profile whisper up -d
   
   # Windows
   cd "C:\Program Files\PVS"
   podman-compose -f podman-compose.onpremise.yml --profile whisper up -d
   ```

**Hinweis**: Whisper benötigt deutlich mehr Ressourcen (RAM, CPU).

### 4. Firewall konfigurieren

#### Linux (UFW)
```bash
sudo ufw allow 8080/tcp
sudo ufw reload
```

#### Linux (firewalld)
```bash
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

#### Windows
```powershell
New-NetFirewallRule -DisplayName "PVS OnPremise" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```

### 5. Reverse Proxy einrichten (optional)

Für Produktionsumgebungen empfohlen:

**Nginx-Beispiel:**
```nginx
server {
    listen 80;
    server_name pvs.example.com;
    
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Verifikation

### Container-Status prüfen

```bash
# Linux
sudo su - pvs
cd /opt/pvs
podman-compose -f podman-compose.onpremise.yml ps

# Windows
cd "C:\Program Files\PVS"
podman-compose -f podman-compose.onpremise.yml ps
```

Alle Container sollten den Status "Up" haben.

### Health-Checks

```bash
# Application Health
curl http://localhost:8080/actuator/health

# PostgreSQL
podman exec pvs-onpremise-postgres pg_isready -U pvs
```

### Logs prüfen

```bash
# Linux - Systemd-Logs
sudo journalctl -u pvs-onpremise -f

# Linux/Windows - Container-Logs
podman-compose -f podman-compose.onpremise.yml logs -f
```

## Deinstallation

Falls PVS OnPremise entfernt werden soll, verwende das Deinstallationsskript:

### Linux

```bash
# Als root ausführen
sudo bash onpremise/uninstall.sh
```

### Windows

```powershell
# PowerShell als Administrator öffnen
cd C:\path\to\pvs\onpremise
.\uninstall.ps1
```

**WICHTIG**: Das Deinstallationsskript fragt explizit nach:
- **Datenbank-Volumes**: Enthalten IVOM-Behandlungsdaten, Patientendaten, etc.
  - Standardmäßig werden diese **NICHT** gelöscht
  - Du kannst sie explizit behalten oder löschen
- **Backup-Verzeichnis**: Kann separat behalten werden
- **Installations-Verzeichnis**: Kann komplett oder teilweise entfernt werden

Am Ende wird eine Zusammenfassung ausgegeben, was entfernt wurde und was noch vorhanden ist.

## Nächste Schritte

- [README.md](./README.md) - Übersicht und Betrieb
- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) - Problemlösung
- [../docs/](../docs/) - Weitere Dokumentation

