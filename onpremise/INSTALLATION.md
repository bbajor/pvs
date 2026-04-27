# Detaillierte Installationsanleitung

Diese Anleitung führt Schritt für Schritt durch die native Installation von IVOMPlaner On-Premise.

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

# Java prüfen (falls bereits installiert)
java -version
```

### 2. Java und PostgreSQL installieren

#### Linux

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install -y openjdk-21-jre-headless postgresql postgresql-client curl openssl
```

**Fedora/RHEL/CentOS:**
```bash
sudo dnf install -y java-21-openjdk-headless postgresql-server postgresql curl openssl
```

**Arch/Manjaro:**
```bash
sudo pacman -S jre21-openjdk postgresql curl openssl
```

## Installation

### Variante A: direkt aus GitHub Releases

```bash
export IVOMPLANER_RELEASE_BASE_URL="https://github.com/<org>/<repo>/releases/latest/download"
curl -fsSL "$IVOMPLANER_RELEASE_BASE_URL/install.sh" | sudo IVOMPLANER_RELEASE_BASE_URL="$IVOMPLANER_RELEASE_BASE_URL" bash
```

### Variante B: lokales Release-Paket

```bash
tar -xzf ivomplaner-onpremise-1.2.3.tar.gz
sudo bash ivomplaner-onpremise-1.2.3/install.sh /path/to/ivomplaner-onpremise-1.2.3.tar.gz
```

Der Installer:

- prüft und installiert Java 21/PostgreSQL falls möglich,
- erstellt Service-User `ivomplaner`,
- erstellt `/opt/ivomplaner/releases/<version>` und `/opt/ivomplaner/current`,
- erstellt `/etc/ivomplaner/ivomplaner.env`,
- generiert sichere Passwörter,
- erstellt Datenbank und DB-User lokal in PostgreSQL,
- installiert `ivomplaner-update`, `ivomplaner-backup`, `ivomplaner-restore`,
- installiert und startet den systemd-Service `ivomplaner`.

### Konfiguration anpassen

```bash
sudo nano /etc/ivomplaner/ivomplaner.env
sudo systemctl restart ivomplaner
```

Wichtige Einstellungen:

1. Datenbank-Passwörter: werden beim Erstinstallieren generiert.
2. SMTP-Konfiguration: falls E-Mail-Versand benötigt wird.
3. Ports: Standard ist 8080, kann angepasst werden.
4. Whisper: lokaler Whisper-Service muss separat bereitgestellt werden.
5. App-Updates: `APP_UPDATE_ENABLED=true` aktiviert den Update-Hinweis in der App.

### Anwendung testen

```bash
# Health-Check
curl http://localhost:8080/actuator/health

# Im Browser öffnen
# http://localhost:8080
```

### App-Update aus der Anwendung

Super-Admins sehen im Menü `System-Update`, ob ein neues Release verfügbar ist.
Beim Klick auf `Update installieren` zeigt die App zuerst einen Hinweis, dass offene Änderungen gespeichert werden sollen.
Danach startet die App den Wrapper `sudo -n /usr/local/bin/ivomplaner-update-wrapper latest`.

Der Wrapper startet das eigentliche Update in einer separaten systemd-Unit. Dadurch kann die Web-Anwendung sich selbst neu starten, ohne dem eigenen Kindprozess den Stuhl wegzutreten.

Update-Logs liegen unter:

```bash
sudo ls -lh /opt/ivomplaner/logs/
sudo journalctl -u 'ivomplaner-update-*'
```

## Post-Installation

### 1. Erste Anmeldung

1. Öffne die Anwendung im Browser: `http://localhost:8080`
2. Melde Dich als `superadmin` an.
3. Das initiale Passwort steht beim ersten Start im Journal, sofern `SUPER_ADMIN_INITIAL_PASSWORD` leer war:
   ```bash
   sudo journalctl -u ivomplaner -b | grep -A 5 "SUPER-ADMIN INITIAL CREDENTIALS"
   ```
4. Ändere das Passwort direkt nach dem ersten Login.

### 2. SMTP konfigurieren (optional)

Falls E-Mail-Versand benötigt wird:

1. Bearbeite `/etc/ivomplaner/ivomplaner.env`
2. Setze die SMTP-Variablen:
   ```
   SMTP_HOST=smtp.example.com
   SMTP_PORT=587
   SMTP_USERNAME=noreply@example.com
   SMTP_PASSWORD=your_password
   SMTP_FROM_ADDRESS=noreply@example.com
   ```
3. **WICHTIG**: Setze `SMTP_ENCRYPTION_KEY` (wird beim Installer generiert)
4. Starte die Anwendung neu:
   ```bash
   sudo systemctl restart ivomplaner
   ```

### 3. Whisper aktivieren (optional)

Für lokale Spracherkennung:

1. Bearbeite die `.env`-Datei:
   ```
   AI_WHISPER_LOCAL_ENABLED=true
   ```
2. Stelle einen Whisper-kompatiblen lokalen Service auf `AI_WHISPER_LOCAL_HOST:AI_WHISPER_LOCAL_PORT` bereit.
3. Starte IVOMPlaner neu: `sudo systemctl restart ivomplaner`.

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

### Service-Status prüfen

```bash
sudo systemctl status ivomplaner
sudo systemctl status postgresql
```

Beide Services sollten aktiv sein.

### Health-Checks

```bash
# Application Health
curl http://localhost:8080/actuator/health

# PostgreSQL
pg_isready -h 127.0.0.1 -U pvs -d pvs
```

### Logs prüfen

```bash
# Linux - Systemd-Logs
sudo journalctl -u ivomplaner -f
```

## Deinstallation

Falls PVS OnPremise entfernt werden soll, verwende das Deinstallationsskript:

### Linux

```bash
# Als root ausführen
sudo ivomplaner-uninstall
```

**WICHTIG**: Das Deinstallationsskript fragt explizit nach:
- **PostgreSQL-Datenbank und DB-User**: Enthalten IVOM-Behandlungsdaten, Patientendaten, etc.
  - Standardmäßig werden diese nicht automatisch gelöscht
  - Du kannst sie explizit behalten oder löschen
- **Backup-Verzeichnis**: Kann separat behalten werden
- **Installations-Verzeichnis**: Kann komplett oder teilweise entfernt werden

Am Ende wird eine Zusammenfassung ausgegeben, was entfernt wurde und was noch vorhanden ist.

## Nächste Schritte

- [README.md](./README.md) - Übersicht und Betrieb
- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) - Problemlösung
- [../docs/](../docs/) - Weitere Dokumentation

