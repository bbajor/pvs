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

- Prüft und installiert Java 21/PostgreSQL falls möglich
- Erstellt Service-User `pvs`
- Erstellt Installations-Verzeichnis `/opt/pvs`
- Kopiert Konfigurationsdateien
- Generiert sichere Passwörter
- Erstellt Datenbank und DB-User lokal in PostgreSQL
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

#### Schritt 4: Release-Artefakt installieren

```bash
# Variante A: vorhandene JAR-Datei
sudo install -o pvs -g pvs -m 0644 pvs-app.jar /opt/pvs/app/pvs-app.jar

# Variante B: Release-Tarball
sudo tar -xzf ivomplaner-onpremise-<version>.tar.gz -C /tmp
sudo install -o pvs -g pvs -m 0644 /tmp/ivomplaner-onpremise-<version>/pvs-app.jar /opt/pvs/app/pvs-app.jar
```

#### Schritt 5: Service starten

```bash
# Service starten
sudo systemctl start pvs-onpremise

# Status prüfen
sudo systemctl status pvs-onpremise

# Logs anzeigen
sudo journalctl -u pvs-onpremise -f
```

#### Schritt 6: Anwendung testen

```bash
# Health-Check
curl http://localhost:8080/actuator/health

# Im Browser öffnen
# http://localhost:8080
```

## Post-Installation

### 1. Erste Anmeldung

1. Öffne die Anwendung im Browser: `http://localhost:8080`
2. Melde Dich als `superadmin` an.
3. Das initiale Passwort steht beim ersten Start im Journal, sofern `SUPER_ADMIN_INITIAL_PASSWORD` leer war:
   ```bash
   sudo journalctl -u pvs-onpremise -b | grep -A 5 "SUPER-ADMIN INITIAL CREDENTIALS"
   ```
4. Ändere das Passwort direkt nach dem ersten Login.

### 2. SMTP konfigurieren (optional)

Falls E-Mail-Versand benötigt wird:

1. Bearbeite `/opt/pvs/.env`
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
   sudo systemctl restart pvs-onpremise
   ```

### 3. Whisper aktivieren (optional)

Für lokale Spracherkennung:

1. Bearbeite die `.env`-Datei:
   ```
   AI_WHISPER_LOCAL_ENABLED=true
   ```
2. Stelle einen Whisper-kompatiblen lokalen Service auf `AI_WHISPER_LOCAL_HOST:AI_WHISPER_LOCAL_PORT` bereit.
3. Starte IVOMPlaner neu: `sudo systemctl restart pvs-onpremise`.

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
sudo systemctl status pvs-onpremise
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
sudo journalctl -u pvs-onpremise -f
```

## Deinstallation

Falls PVS OnPremise entfernt werden soll, verwende das Deinstallationsskript:

### Linux

```bash
# Als root ausführen
sudo bash onpremise/uninstall.sh
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

