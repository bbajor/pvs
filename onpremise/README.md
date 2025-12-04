# PVS OnPremise Installation

Diese Dokumentation beschreibt die Installation und den Betrieb von PVS als OnPremise-Lösung auf einem lokalen Server (z.B. in einer Praxis).

## Übersicht

Die OnPremise-Lösung nutzt **Podman** als Container-Runtime und bietet:

- ✅ Automatischer Start nach Systemneustart
- ✅ Einfache Installation via Installer-Skript
- ✅ Isolierte Container-Umgebung
- ✅ PostgreSQL-Datenbank inklusive
- ✅ KBV Master Data Service
- ✅ Optional: Whisper AI Service (lokal)

## Systemanforderungen

### Linux
- Linux-Distribution (Ubuntu, Debian, Fedora, RHEL, Arch, etc.)
- Podman 4.0+ und podman-compose
- Mindestens 4 GB RAM (8 GB empfohlen)
- 20 GB freier Festplattenspeicher
- Root-Zugriff für Installation

### Windows
- Windows 10/11 oder Windows Server 2019+
- Podman Desktop (https://podman-desktop.io/)
- Mindestens 4 GB RAM (8 GB empfohlen)
- 20 GB freier Festplattenspeicher
- Administrator-Rechte für Installation

## Schnellstart

### Linux

```bash
# 1. Repository klonen oder Dateien kopieren
cd /tmp
git clone <repository-url> pvs
cd pvs/onpremise

# 2. Installer ausführen (als root)
sudo bash install.sh

# 3. Konfiguration anpassen
sudo nano /opt/pvs/.env

# 4. Service starten
sudo systemctl start pvs-onpremise

# 5. Status prüfen
sudo systemctl status pvs-onpremise
```

### Windows

```powershell
# 1. PowerShell als Administrator öffnen
# 2. Ins Installationsverzeichnis wechseln
cd C:\path\to\pvs\onpremise

# 3. Installer ausführen
.\install.ps1

# 4. Konfiguration anpassen
notepad "C:\Program Files\PVS\.env"

# 5. PVS starten
"C:\Program Files\PVS\start-pvs.bat"
```

## Detaillierte Installation

Siehe [INSTALLATION.md](./INSTALLATION.md) für eine ausführliche Installationsanleitung.

## Konfiguration

### Environment-Variablen

Die wichtigsten Konfigurationsoptionen befinden sich in der `.env`-Datei:

- **Datenbank**: PostgreSQL-Zugangsdaten
- **SMTP**: E-Mail-Versand (optional)
- **Ports**: Anpassung der Ports
- **AI/Whisper**: Aktivierung des lokalen Whisper-Services

Siehe [env.example](./env.example) für alle verfügbaren Optionen.

### Automatischer Start

#### Linux
Der Systemd-Service wird automatisch installiert und aktiviert. Die Container starten nach jedem Systemneustart automatisch.

```bash
# Service aktivieren (bereits bei Installation geschehen)
sudo systemctl enable pvs-onpremise

# Service manuell starten/stoppen
sudo systemctl start pvs-onpremise
sudo systemctl stop pvs-onpremise

# Status prüfen
sudo systemctl status pvs-onpremise

# Logs anzeigen
sudo journalctl -u pvs-onpremise -f
```

#### Windows
Ein Windows Task wird erstellt, der die Container nach dem Systemstart automatisch startet.

```powershell
# Task-Status prüfen
Get-ScheduledTask -TaskName "PVS-OnPremise-Start"

# Task manuell ausführen
Start-ScheduledTask -TaskName "PVS-OnPremise-Start"
```

## Betrieb

### Container-Verwaltung

#### Linux
```bash
# Als Service-User einloggen
sudo su - pvs
cd /opt/pvs

# Container starten
podman-compose -f podman-compose.onpremise.yml --env-file .env up -d

# Container stoppen
podman-compose -f podman-compose.onpremise.yml down

# Status prüfen
podman-compose -f podman-compose.onpremise.yml ps

# Logs anzeigen
podman-compose -f podman-compose.onpremise.yml logs -f
```

#### Windows
```batch
# In PowerShell oder CMD
cd "C:\Program Files\PVS"

# Container starten
.\start-pvs.bat

# Container stoppen
.\stop-pvs.bat

# Status prüfen
.\status-pvs.bat
```

### Backup

#### Datenbank-Backup
```bash
# Linux
sudo su - pvs
cd /opt/pvs
podman exec pvs-onpremise-postgres pg_dump -U pvs pvs > backups/pvs_backup_$(date +%Y%m%d_%H%M%S).sql

# Windows
podman exec pvs-onpremise-postgres pg_dump -U pvs pvs > "C:\Program Files\PVS\backups\pvs_backup_%date:~-4,4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%.sql"
```

### Updates

1. **Container-Images aktualisieren**:
   ```bash
   cd /opt/pvs
   podman-compose -f podman-compose.onpremise.yml pull
   podman-compose -f podman-compose.onpremise.yml up -d
   ```

2. **Anwendung neu bauen** (bei Code-Änderungen):
   ```bash
   podman-compose -f podman-compose.onpremise.yml build
   podman-compose -f podman-compose.onpremise.yml up -d
   ```

## Troubleshooting

Siehe [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) für häufige Probleme und Lösungen.

## Sicherheit

### Wichtige Sicherheitshinweise

1. **Passwörter ändern**: Nach der Installation sollten alle Standard-Passwörter geändert werden
2. **Firewall**: Nur notwendige Ports öffnen (standardmäßig nur 8080)
3. **Backups**: Regelmäßige Backups der Datenbank durchführen
4. **Updates**: Regelmäßig Container-Images aktualisieren
5. **SMTP_ENCRYPTION_KEY**: Sicher aufbewahren - bei Verlust können verschlüsselte SMTP-Passwörter nicht mehr entschlüsselt werden

### Netzwerk

Die Container sind standardmäßig nur auf `127.0.0.1` (localhost) erreichbar. Für externe Zugriffe:

1. Firewall-Regeln konfigurieren
2. Reverse Proxy (z.B. Nginx, Traefik) verwenden
3. SSL/TLS-Zertifikate einrichten

## Deinstallation

### Linux

```bash
# Als root ausführen
sudo bash onpremise/uninstall.sh
```

Das Skript führt interaktiv durch die Deinstallation:
- Entfernt Systemd-Service
- Stoppt und entfernt Container
- **WICHTIG**: Fragt explizit nach Datenbank-Volumes (IVOM-Behandlungsdaten können erhalten bleiben!)
- Entfernt Installations-Verzeichnis (Backups optional behalten)
- Zeigt Zusammenfassung am Ende

### Windows

```powershell
# PowerShell als Administrator öffnen
cd C:\path\to\pvs\onpremise
.\uninstall.ps1
```

Das Skript führt interaktiv durch die Deinstallation:
- Entfernt Scheduled Task
- Stoppt und entfernt Container
- **WICHTIG**: Fragt explizit nach Datenbank-Volumes (IVOM-Behandlungsdaten können erhalten bleiben!)
- Entfernt Installations-Verzeichnis (Backups optional behalten)
- Zeigt Zusammenfassung am Ende

**Hinweis**: Die Deinstallation fragt explizit nach Datenbank-Volumes, da diese die IVOM-Behandlungsdaten und andere kritische Daten enthalten. Standardmäßig werden diese **NICHT** gelöscht.

## Support

Bei Problemen oder Fragen:

1. Prüfe die [Troubleshooting-Dokumentation](./TROUBLESHOOTING.md)
2. Prüfe die Logs: `journalctl -u pvs-onpremise -f` (Linux) oder Container-Logs
3. Erstelle ein Issue im Repository

## Lizenz

Siehe [LICENSE.md](../LICENSE.md) für Lizenzinformationen.

