# Cloud Setup Guide - Hetzner Cloud

Dieser Guide beschreibt die Einrichtung der PVS-Anwendung in der Hetzner Cloud.

## Voraussetzungen

- Hetzner Cloud Account
- GitHub Account mit Repository-Zugriff
- SSH-Zugriff auf Hetzner Server
- Domain (optional, für HTTPS)

## 1. Hetzner Server einrichten

### 1.1 Server erstellen

1. Gehe zu [Hetzner Cloud Console](https://console.hetzner.cloud)
2. Erstelle einen neuen Server:
   - **Type**: CX21 (2 vCPU, 4GB RAM) oder größer
   - **Location**: Nürnberg oder Falkenstein (DSGVO-konform)
   - **Image**: Ubuntu 22.04 LTS
   - **SSH Key**: Füge deinen öffentlichen SSH-Key hinzu

### 1.2 Server-Setup ausführen

```bash
# Auf dem Hetzner Server (als root)
cd /root
curl -fsSL https://raw.githubusercontent.com/bbajor/pvs/master/scripts/deployment/setup-server.sh | bash

# Oder manuell:
# 1. Script herunterladen
wget https://raw.githubusercontent.com/bbajor/pvs/master/scripts/deployment/setup-server.sh
chmod +x setup-server.sh
sudo ./setup-server.sh
```

Das Script installiert automatisch:
- Podman & Podman-Compose
- Firewall (UFW) - nur SSH offen
- Fail2Ban für SSH-Schutz
- Benutzer `pvs` für Deployment
- Verzeichnisstruktur `/opt/pvs`
- Log-Rotation
- Automatische Updates

### 1.3 Firewall konfigurieren

```bash
# Firewall-Setup ausführen (nur SSH offen)
sudo ./scripts/deployment/setup-firewall.sh

# Status prüfen
sudo ufw status verbose
```

**Wichtig:** Die Firewall blockiert alle Ports außer SSH (22). Application-Ports sind nur auf localhost gebunden und werden über SSH-Tunnel erreicht.

## 2. Datenbank einrichten

### 2.1 Managed PostgreSQL (Empfohlen)

1. Erstelle eine Managed PostgreSQL-Datenbank in der Hetzner Cloud Console
2. Notiere die Verbindungsdaten:
   - Host
   - Port
   - Database Name
   - Username
   - Password

### 2.2 Oder: Container-basierte PostgreSQL

```bash
cd /opt/pvs
podman-compose -f podman-compose.production.yml --profile prod up -d postgres-prod
```

## 3. Environment-Variablen konfigurieren

Erstelle `/opt/pvs/.env`:

```bash
# Datenbank
DATABASE_URL=jdbc:postgresql://<host>:<port>/<database>
DATABASE_USERNAME=<username>
DATABASE_PASSWORD=<password>

# Secrets (ERFORDERLICH für Cloud)
SMTP_ENCRYPTION_KEY=$(openssl rand -base64 32 | head -c 32)

# SMTP
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=noreply@example.com
SMTP_PASSWORD=<password>
SMTP_FROM_ADDRESS=noreply@example.com

# Redis (für Session-Storage)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=<password>

# AI/Whisper
AI_API_KEY=<your-api-key>
WHISPER_REMOTE_ENABLED=true

# Server
PORT=8080
SESSION_TIMEOUT=30m
```

## 4. GitHub Secrets konfigurieren

In GitHub → Settings → Secrets → Actions:

- `HETZNER_HOST`: Server-IP-Adresse
- `HETZNER_USER`: SSH-User (meist `root`)
- `HETZNER_SSH_KEY`: Privater SSH-Key für Deployment
- `PROD_DB_HOST`: Datenbank-Host
- `PROD_DB_NAME`: Datenbank-Name
- `PROD_DB_USER`: Datenbank-User
- `PROD_DB_PASSWORD`: Datenbank-Passwort

## 5. Test-Instanz Setup

### 5.1 Test-Instanz auf Server einrichten

Die Test-Instanz läuft auf separaten Ports (8081 statt 8080) und ist nicht öffentlich erreichbar.

```bash
# Als Benutzer 'pvs' einloggen
su - pvs
cd /opt/pvs

# Environment-Datei erstellen
cp .env.example .env
nano .env  # Passwörter anpassen

# Repository klonen oder Dateien kopieren
git clone https://github.com/bbajor/pvs.git .
# Oder: podman-compose.production.yml kopieren

# Test-Instanz starten
podman-compose -f podman-compose.production.yml --profile test up -d
```

### 5.2 SSH-Tunnel für Test-Instanz

Die Test-Instanz ist nur über SSH-Tunnel erreichbar:

```bash
# Lokal (auf deinem Rechner)
./scripts/deployment/ssh-tunnel-test.sh user@hetzner-server.example.com

# Oder manuell
ssh -L 8081:localhost:8081 -N user@hetzner-server.example.com
```

Dann erreichbar unter: `http://localhost:8081`

Siehe [SSH_TUNNEL_SETUP.md](./SSH_TUNNEL_SETUP.md) für Details.

### 5.3 Unterschiede Test vs. Production

| Feature | Test | Production |
|---------|------|-----------|
| Port | 8081 | 8080 |
| Redis Port | 6380 | 6379 |
| Whisper Port | 9001 | 9000 |
| Zugriff | SSH-Tunnel | Öffentlich (HTTPS) |
| Firewall | Nur SSH | SSH + HTTP/HTTPS |
| Traefik | Nein | Ja |

## 6. Deployment

### 6.1 Automatisches Deployment via GitHub Actions

1. Gehe zu GitHub → Actions → "Cloud Deployment (Hetzner)"
2. Klicke "Run workflow"
3. Wähle Environment: `test` oder `prod`
4. Starte das Deployment

### 6.2 Manuelles Deployment

```bash
# Auf dem Server (als Benutzer 'pvs')
cd /opt/pvs

# Test-Instanz
./scripts/deployment/deploy-hetzner.sh test latest

# Production-Instanz
./scripts/deployment/deploy-hetzner.sh prod latest
```

Siehe [CLOUD_DEPLOYMENT.md](./CLOUD_DEPLOYMENT.md) für vollständigen Deployment-Workflow.

## 6. Backup-Strategie

### 6.1 Automatische Backups einrichten

```bash
# Crontab für tägliche Backups
crontab -e

# Füge hinzu:
0 2 * * * /opt/pvs/scripts/deployment/backup-postgres.sh
```

### 6.2 Backup wiederherstellen

```bash
# Liste verfügbare Backups
ls -lh /opt/pvs/backups/

# Restore
./scripts/deployment/restore-postgres.sh /opt/pvs/backups/pvs_prod_20240101_020000.sql.gz
```

## 7. Monitoring

### 7.1 Health Checks

- Health Endpoint: `http://<server-ip>:8080/actuator/health`
- Prometheus Metrics: `http://<server-ip>:8080/actuator/prometheus`

### 7.2 Logs

```bash
# Application Logs
podman logs -f pvs-prod

# Log-Dateien
tail -f /var/log/pvs/application.log
```

## 7. Test-Instanz vs. Production

### Test-Instanz

- **Zugriff:** Nur über SSH-Tunnel (`localhost:8081`)
- **Ports:** 8081 (App), 6380 (Redis), 9001 (Whisper)
- **Firewall:** Nur SSH (22) offen
- **Zweck:** Testing vor Production-Deployment

### Production-Instanz

- **Zugriff:** Öffentlich über HTTPS
- **Ports:** 8080 (App), 6379 (Redis), 9000 (Whisper)
- **Firewall:** SSH (22), HTTP (80), HTTPS (443)
- **Zweck:** Live-System

Siehe [SSH_TUNNEL_SETUP.md](./SSH_TUNNEL_SETUP.md) für Test-Instanz-Zugriff.

## 8. Deployment-Workflow

Siehe [CLOUD_DEPLOYMENT.md](./CLOUD_DEPLOYMENT.md) für vollständigen Workflow:
- Lokal → dev → test → prod
- Merge-Strategie
- Deployment-Prozess
- Rollback-Verfahren

## 9. Troubleshooting

Siehe [CLOUD_TROUBLESHOOTING.md](./CLOUD_TROUBLESHOOTING.md)

## 10. Sicherheit

- **Firewall:** Nur notwendige Ports offen (Test: nur SSH, Prod: SSH + HTTP/HTTPS)
- **SSH:** Key-basierte Authentifizierung, Fail2Ban
- **Updates:** Automatische Security-Updates
- **Secrets:** Über Environment-Variablen (nie im Code)
- **Test-Instanz:** Nicht öffentlich erreichbar (nur SSH-Tunnel)

## 11. Skalierung

Für High Availability:
- Mehrere App-Instanzen
- Load Balancer (Hetzner Load Balancer)
- Redis für Session-Storage
- Managed PostgreSQL mit Replikation

Siehe [CLOUD_HIGH_AVAILABILITY.md](./CLOUD_HIGH_AVAILABILITY.md)


