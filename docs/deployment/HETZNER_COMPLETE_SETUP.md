# Hetzner Server - Komplettes Setup von Grund auf

Diese Anleitung führt dich Schritt für Schritt durch das komplette Setup eines Hetzner-Servers für PVS mit separaten PostgreSQL-Containern für Dev/Test/Prod.

**Ziel**: Auch in 6 Monaten noch alles nachvollziehbar und reproduzierbar aufsetzen können.

## 📋 Übersicht

- **Server**: Hetzner Cloud VPS (CX21, 5€/Monat)
- **Container**: Separate PostgreSQL-Container für Dev/Test/Prod
- **Deployment**: Docker Compose mit GitHub Actions
- **Dauer**: ~30 Minuten für komplettes Setup

---

## Schritt 1: Hetzner VPS erstellen

### 1.1 Server in Hetzner Cloud Console erstellen

1. Gehe zu [hetzner.com/cloud](https://www.hetzner.com/cloud)
2. **Add Server** klicken
3. **Location**: Nürnberg oder Falkenstein (Deutschland, DSGVO-konform)
4. **Image**: Ubuntu 22.04 LTS
5. **Type**: CX21 (2 vCPU, 4GB RAM) - **5€/Monat**
6. **SSH Key** hinzufügen (oder später einrichten)
7. Server erstellen und **IP-Adresse notieren**

### 1.2 SSH-Zugriff prüfen

```bash
ssh root@<HETZNER_IP>
```

Falls Probleme: Prüfe ob SSH-Key in Hetzner Console hinterlegt ist.

---

## Schritt 2: Server-Grundsetup

### 2.1 Initial Setup Script ausführen

```bash
# Auf dem Hetzner Server (als root)
cd /root
curl -fsSL https://raw.githubusercontent.com/bbajor/pvs/master/scripts/deployment/setup-server.sh | bash
```

**ODER manuell:**

```bash
# Docker installieren
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
rm get-docker.sh

# Docker Compose installieren
DOCKER_COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep 'tag_name' | cut -d\" -f4)
sudo curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Docker ohne sudo nutzen
sudo usermod -aG docker $USER
newgrp docker

# Projekt-Verzeichnis erstellen
sudo mkdir -p /opt/pvs
sudo chown $USER:$USER /opt/pvs
cd /opt/pvs
```

**Verifikation:**
```bash
docker --version        # Sollte Version anzeigen
docker-compose --version  # Sollte Version anzeigen
```

---

## Schritt 3: Environment-Variablen konfigurieren

### 3.1 .env Datei erstellen

```bash
cd /opt/pvs
nano .env
```

**Vollständige .env Datei einfügen:**

```bash
# ============================================
# PostgreSQL Configuration - DEV
# ============================================
POSTGRES_DB_DEV=pvs_dev
POSTGRES_USER_DEV=pvs_user
POSTGRES_PASSWORD_DEV=$(openssl rand -base64 32)

# ============================================
# PostgreSQL Configuration - TEST
# ============================================
POSTGRES_DB_TEST=pvs_test
POSTGRES_USER_TEST=pvs_user
POSTGRES_PASSWORD_TEST=$(openssl rand -base64 32)

# ============================================
# PostgreSQL Configuration - PRODUCTION
# ============================================
POSTGRES_DB_PROD=pvs_prod
POSTGRES_USER_PROD=pvs_user
POSTGRES_PASSWORD_PROD=$(openssl rand -base64 32)

# ============================================
# Docker Registry
# ============================================
DOCKER_REGISTRY=ghcr.io
DOCKER_IMAGE=bbajor/pvs

# ============================================
# Let's Encrypt / SSL
# ============================================
LETSENCRYPT_EMAIL=deine@email.de

# ============================================
# Optional: Domain-Konfiguration
# ============================================
# DOMAIN_DEV=dev.pvs.example.com
# DOMAIN_TEST=test.pvs.example.com
# DOMAIN_PROD=pvs.example.com
```

**WICHTIG**: Die `$(openssl rand -base64 32)` Kommandos werden beim Erstellen der Datei NICHT ausgeführt. **Du musst die Passwörter manuell generieren:**

```bash
# Passwörter generieren:
openssl rand -base64 32  # Für DEV
openssl rand -base64 32  # Für TEST
openssl rand -base64 32  # Für PROD (WICHTIG: Sicher aufbewahren!)
```

**Beispiel .env Datei (mit generierten Passwörtern):**

```bash
POSTGRES_DB_DEV=pvs_dev
POSTGRES_USER_DEV=pvs_user
POSTGRES_PASSWORD_DEV=aB3xY9mP2qR7tV5wZ8nK4jL6hG1fD0cE

POSTGRES_DB_TEST=pvs_test
POSTGRES_USER_TEST=pvs_user
POSTGRES_PASSWORD_TEST=kL8mN3pQ6rT9vW2xY5zA7bC1dE4fG0hJ

POSTGRES_DB_PROD=pvs_prod
POSTGRES_USER_PROD=pvs_user
POSTGRES_PASSWORD_PROD=xY9zA2bC5dE8fG1hJ4kL7mN0pQ3rT6vW  # NUR FÜR PROD - SICHER AUFBEWAHREN!

DOCKER_REGISTRY=ghcr.io
DOCKER_IMAGE=bbajor/pvs
LETSENCRYPT_EMAIL=deine@email.de
```

### 3.2 Dateirechte schützen

```bash
chmod 600 .env  # Nur Owner kann lesen/schreiben
ls -la .env     # Sollte -rw------- zeigen
```

### 3.3 Passwörter dokumentieren

**WICHTIG**: Sichere die Passwörter an einem sicheren Ort (Passwort-Manager, verschlüsselt):

- `POSTGRES_PASSWORD_DEV`: Für Development-Datenbank
- `POSTGRES_PASSWORD_TEST`: Für Test-Datenbank
- `POSTGRES_PASSWORD_PROD`: **KRITISCH** - Für Production-Datenbank

---

## Schritt 4: Docker Compose File kopieren

### 4.1 Von lokalem Rechner auf Server kopieren

```bash
# Von deinem lokalen Rechner aus:
scp docker-compose.production.yml root@<HETZNER_IP>:/opt/pvs/
```

### 4.2 ODER direkt auf Server herunterladen

```bash
# Auf dem Hetzner Server:
cd /opt/pvs
curl -O https://raw.githubusercontent.com/bbajor/pvs/master/docker-compose.production.yml
```

**Verifikation:**
```bash
ls -la docker-compose.production.yml
cat docker-compose.production.yml | head -20
```

---

## Schritt 5: PostgreSQL Container starten

### 5.1 Produktions-Datenbank starten

```bash
cd /opt/pvs
docker-compose -f docker-compose.production.yml --profile prod up -d postgres-prod
```

### 5.2 Container-Status prüfen

```bash
docker-compose -f docker-compose.production.yml ps
docker-compose -f docker-compose.production.yml logs postgres-prod
```

**Erwartete Ausgabe:**
```
Creating pvs-postgres-prod ... done
... database system is ready to accept connections
```

### 5.3 Health Check

```bash
docker exec pvs-postgres-prod pg_isready -U pvs_user
```

Sollte `pvs-postgres-prod:5432 - accepting connections` ausgeben.

---

## Schritt 6: Test- und Dev-Datenbanken starten (optional)

### 6.1 Test-Datenbank

```bash
docker-compose -f docker-compose.production.yml --profile test up -d postgres-test
docker-compose -f docker-compose.production.yml logs postgres-test
```

### 6.2 Dev-Datenbank

```bash
docker-compose -f docker-compose.production.yml --profile dev up -d postgres-dev
docker-compose -f docker-compose.production.yml logs postgres-dev
```

### 6.3 Alle Container prüfen

```bash
docker-compose -f docker-compose.production.yml ps
```

**Erwartete Ausgabe:**
```
NAME                STATUS          PORTS
pvs-postgres-dev   Up (healthy)    127.0.0.1:5433->5432/tcp
pvs-postgres-test  Up (healthy)    127.0.0.1:5434->5432/tcp
pvs-postgres-prod  Up (healthy)    127.0.0.1:5435->5432/tcp
```

---

## Schritt 7: Datenbank-Verbindung testen

### 7.1 Produktions-Datenbank testen

```bash
docker exec -it pvs-postgres-prod psql -U pvs_user -d pvs_prod -c "SELECT version();"
```

**Erwartete Ausgabe:**
```
PostgreSQL 15.x on x86_64...
```

### 7.2 Datenbanken auflisten

```bash
docker exec -it pvs-postgres-prod psql -U pvs_user -d postgres -c "\l"
```

Sollte `pvs_prod` anzeigen.

### 7.3 Von außen prüfen (nur lokal)

```bash
# Von lokalem Rechner (mit Port-Forward):
ssh -L 5435:127.0.0.1:5435 root@<HETZNER_IP>

# In neuem Terminal:
psql -h localhost -p 5435 -U pvs_user -d pvs_prod
# Passwort eingeben (POSTGRES_PASSWORD_PROD aus .env)
```

---

## Schritt 8: GitHub Secrets konfigurieren

### 8.1 GitHub Secrets öffnen

1. Gehe zu: https://github.com/bbajor/pvs/settings/secrets/actions
2. Klicke **"New repository secret"**

### 8.2 Secrets eintragen

**Secret 1: HETZNER_HOST**
```
Name: HETZNER_HOST
Secret: 188.245.253.179  (deine Server-IP)
```

**Secret 2: HETZNER_USER**
```
Name: HETZNER_USER
Secret: root
```

**Secret 3: HETZNER_SSH_KEY**
```
Name: HETZNER_SSH_KEY
Secret: [Dein privater SSH-Key - siehe SSH Setup Anleitung]
```

**Secret 4: PROD_DB_HOST**
```
Name: PROD_DB_HOST
Secret: localhost
```

**Secret 5: PROD_DB_NAME**
```
Name: PROD_DB_NAME
Secret: pvs_prod
```

**Secret 6: PROD_DB_USER**
```
Name: PROD_DB_USER
Secret: pvs_user
```

**Secret 7: PROD_DB_PASSWORD**
```
Name: PROD_DB_PASSWORD
Secret: [POSTGRES_PASSWORD_PROD aus .env Datei auf Server]
```

### 8.3 Secrets verifizieren

Prüfe dass alle 7 Secrets vorhanden sind:
- ✅ HETZNER_HOST
- ✅ HETZNER_USER
- ✅ HETZNER_SSH_KEY
- ✅ PROD_DB_HOST
- ✅ PROD_DB_NAME
- ✅ PROD_DB_USER
- ✅ PROD_DB_PASSWORD

---

## Schritt 9: Erste Deployment testen

### 9.1 GitHub Actions Workflow starten

1. Gehe zu: https://github.com/bbajor/pvs/actions
2. Wähle "Build and Push Docker Images (Hetzner)"
3. Klicke "Run workflow"
4. Stage: `dev` wählen
5. "Run workflow" klicken

### 9.2 Auf Server prüfen

```bash
# Auf Hetzner Server:
cd /opt/pvs
docker-compose -f docker-compose.production.yml --profile dev ps
docker-compose -f docker-compose.production.yml --profile dev logs pvs-dev
```

### 9.3 Health Check

```bash
curl http://localhost:8080/actuator/health
```

Sollte `{"status":"UP"}` zurückgeben.

---

## Schritt 10: DNS & Domain (optional)

### 10.1 DNS-Records setzen

In deinem DNS-Provider (z.B. Cloudflare, Hetzner DNS):

```
Type: A
Name: dev.pvs
Value: <HETZNER_IP>
TTL: 3600

Type: A
Name: test.pvs
Value: <HETZNER_IP>
TTL: 3600

Type: A
Name: pvs
Value: <HETZNER_IP>
TTL: 3600
```

### 10.2 Traefik starten

```bash
docker-compose -f docker-compose.production.yml up -d traefik
docker-compose -f docker-compose.production.yml logs traefik
```

Traefik erstellt automatisch SSL-Zertifikate via Let's Encrypt.

---

## Schritt 11: Backup-Setup

### 11.1 Backup-Verzeichnis erstellen

```bash
mkdir -p /opt/pvs/backups
chmod 700 /opt/pvs/backups
```

### 11.2 Backup-Script erstellen

```bash
cat > /opt/pvs/backup-db.sh <<'EOF'
#!/bin/bash
# Tägliches Backup-Script

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/opt/pvs/backups"

# Backup PROD
docker exec pvs-postgres-prod pg_dump -U pvs_user pvs_prod | gzip > "$BACKUP_DIR/pvs_prod_$DATE.sql.gz"

# Backup TEST
docker exec pvs-postgres-test pg_dump -U pvs_user pvs_test | gzip > "$BACKUP_DIR/pvs_test_$DATE.sql.gz"

# Backup DEV (optional)
# docker exec pvs-postgres-dev pg_dump -U pvs_user pvs_dev | gzip > "$BACKUP_DIR/pvs_dev_$DATE.sql.gz"

# Alte Backups löschen (älter als 30 Tage)
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +30 -delete

echo "✅ Backups erstellt: $(ls -lh $BACKUP_DIR | tail -n +2 | wc -l) Dateien"
EOF

chmod +x /opt/pvs/backup-db.sh
```

### 11.3 Cron-Job einrichten

```bash
crontab -e
```

Füge hinzu:
```
# Täglich um 2:00 Uhr Datenbank-Backup
0 2 * * * /opt/pvs/backup-db.sh >> /opt/pvs/backups/backup.log 2>&1
```

**Test Backup:**
```bash
/opt/pvs/backup-db.sh
ls -lh /opt/pvs/backups/
```

---

## Schritt 12: Firewall konfigurieren

### 12.1 UFW installieren

```bash
sudo apt update
sudo apt install -y ufw
```

### 12.2 Firewall-Regeln

```bash
# Standard: Alle eingehenden Verbindungen blocken
sudo ufw default deny incoming
sudo ufw default allow outgoing

# SSH erlauben (WICHTIG: Vor enable!)
sudo ufw allow 22/tcp

# HTTP/HTTPS erlauben
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Firewall aktivieren
sudo ufw enable
sudo ufw status
```

**Verifikation:**
```
Status: active

To                         Action      From
--                         ------      ----
22/tcp                     ALLOW       Anywhere
80/tcp                     ALLOW       Anywhere
443/tcp                    ALLOW       Anywhere
```

---

## Schritt 13: Monitoring & Wartung

### 13.1 Logs ansehen

```bash
# Alle Services
docker-compose -f docker-compose.production.yml logs -f

# Nur Production
docker-compose -f docker-compose.production.yml --profile prod logs -f

# Nur PostgreSQL Prod
docker-compose -f docker-compose.production.yml --profile prod logs -f postgres-prod
```

### 13.2 Container-Status

```bash
docker-compose -f docker-compose.production.yml ps
docker stats  # Ressourcen-Verbrauch
```

### 13.3 Datenbank-Statistiken

```bash
docker exec -it pvs-postgres-prod psql -U pvs_user -d pvs_prod -c "
SELECT 
    datname,
    numbackends as connections,
    xact_commit as commits,
    xact_rollback as rollbacks
FROM pg_stat_database 
WHERE datname IN ('pvs_dev', 'pvs_test', 'pvs_prod');
"
```

---

## Schritt 14: Troubleshooting

### Container startet nicht

```bash
# Logs prüfen
docker-compose -f docker-compose.production.yml logs postgres-prod

# Container-Status
docker ps -a | grep postgres-prod

# Neustart
docker-compose -f docker-compose.production.yml restart postgres-prod
```

### Datenbank-Verbindung fehlgeschlagen

```bash
# Ist PostgreSQL bereit?
docker exec pvs-postgres-prod pg_isready -U pvs_user

# Passwort prüfen (aus .env)
grep POSTGRES_PASSWORD_PROD /opt/pvs/.env

# Netzwerk prüfen
docker network inspect pvs_pvs-network
```

### Port bereits belegt

```bash
# Welcher Prozess nutzt den Port?
sudo netstat -tulpn | grep :5435

# Oder Docker-Container
docker ps | grep 5435
```

### Volumes prüfen

```bash
docker volume ls | grep postgres
docker volume inspect pvs_postgres-data-prod
```

---

## Schritt 15: Wiederholbares Setup (für später)

### 15.1 Setup-Dokumentation speichern

Alle wichtigen Informationen dokumentieren:

1. **IP-Adresse**: `188.245.253.179`
2. **Root-Passwort** (falls gesetzt)
3. **SSH-Key Pfad**: `~/.ssh/hetzner_deploy`
4. **.env Passwörter**: In Passwort-Manager
5. **Domain**: `pvs.example.com` (falls vorhanden)

### 15.2 Quick-Setup Script

Wenn du später alles neu aufsetzen musst:

1. Hetzner Server erstellen (Schritt 1)
2. `setup-server.sh` ausführen (Schritt 2)
3. `.env` Datei erstellen mit den Passwörtern (Schritt 3)
4. `docker-compose.production.yml` kopieren (Schritt 4)
5. Container starten (Schritt 5-6)
6. GitHub Secrets aktualisieren (Schritt 8)

### 15.3 Migration von altem Server

Falls du vom alten Setup migrieren musst:

```bash
# 1. Backup vom alten Server
docker exec old-postgres pg_dump -U pvs_user pvs_prod > backup.sql

# 2. Auf neuem Server restore
docker exec -i pvs-postgres-prod psql -U pvs_user -d pvs_prod < backup.sql
```

---

## ✅ Setup-Checkliste

- [ ] Hetzner Server erstellt (IP notiert)
- [ ] SSH-Zugriff funktioniert
- [ ] Docker & Docker Compose installiert
- [ ] `/opt/pvs` Verzeichnis erstellt
- [ ] `.env` Datei erstellt mit allen Passwörtern
- [ ] `.env` Dateirechte: `600` (nur Owner lesbar)
- [ ] `docker-compose.production.yml` kopiert
- [ ] PostgreSQL-Prod Container gestartet
- [ ] PostgreSQL-Health-Check erfolgreich
- [ ] Datenbank-Verbindung getestet
- [ ] GitHub Secrets konfiguriert (alle 7)
- [ ] Firewall aktiviert (nur 22, 80, 443)
- [ ] Backup-Script erstellt
- [ ] Cron-Job für Backups eingerichtet
- [ ] Erste Deployment getestet
- [ ] DNS-Records gesetzt (falls Domain vorhanden)
- [ ] Traefik gestartet (falls Domain vorhanden)
- [ ] SSL-Zertifikat erstellt (automatisch via Traefik)

---

## 📝 Wichtige Befehle (Cheat Sheet)

```bash
# Container starten
docker-compose -f docker-compose.production.yml --profile prod up -d

# Container stoppen
docker-compose -f docker-compose.production.yml --profile prod down

# Logs ansehen
docker-compose -f docker-compose.production.yml logs -f

# Datenbank-Backup
docker exec pvs-postgres-prod pg_dump -U pvs_user pvs_prod > backup.sql

# Datenbank-Restore
docker exec -i pvs-postgres-prod psql -U pvs_user -d pvs_prod < backup.sql

# Passwort ändern (nach Passwort-Änderung in .env)
docker-compose -f docker-compose.production.yml restart postgres-prod

# Alle Container Status
docker-compose -f docker-compose.production.yml ps
```

---

## 🔒 Sicherheits-Checkliste

- [ ] SSH-Key Authentication aktiviert (kein Passwort-Login)
- [ ] Firewall aktiviert (UFW)
- [ ] Starke Passwörter in `.env` (32+ Zeichen, zufällig)
- [ ] `.env` Datei nicht in Git (in `.gitignore`)
- [ ] Docker-Socket nicht öffentlich zugänglich
- [ ] SSL/TLS aktiviert (Traefik + Let's Encrypt)
- [ ] Regelmäßige Backups (täglich)
- [ ] Log-Rotation aktiviert
- [ ] Security Updates installiert
- [ ] Nur benötigte Ports offen (22, 80, 443)

---

**Fertig!** 🎉 Dein Hetzner-Server ist jetzt vollständig konfiguriert und bereit für Deployments.

Bei Problemen: Siehe `docs/deployment/TROUBLESHOOTING.md`

