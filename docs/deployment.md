# Deployment-Dokumentation für PVS

Diese umfassende Dokumentation beschreibt alle Deployment-Optionen und Prozesse für das Praxis-Verwaltungs-System (PVS).

## 📋 Inhaltsverzeichnis

1. [Übersicht](#übersicht)
2. [Branch-Strategie und Workflows](#branch-strategie-und-workflows)
3. [Lokale Entwicklung](#lokale-entwicklung)
4. [Deployment-Optionen](#deployment-optionen)
   - [Hetzner Cloud (Self-Hosted)](#hetzner-cloud-self-hosted)
   - [Render.com (Managed)](#rendercom-managed)
   - [Railway.app (Alternative)](#railwayapp-alternative)
   - [On-Premise](#on-premise)
5. [Datenbank-Architektur](#datenbank-architektur)
6. [CI/CD und Build-Optimierung](#cicd-und-build-optimierung)
7. [Monitoring und Wartung](#monitoring-und-wartung)
8. [Rollback und Backup](#rollback-und-backup)
9. [Sicherheit und Compliance](#sicherheit-und-compliance)
10. [Troubleshooting](#troubleshooting)

---

## Übersicht

PVS unterstützt mehrere Deployment-Methoden für verschiedene Anwendungsfälle:

| Deployment-Typ | Zielumgebung | Kosten/Monat | Aufwand |
|---------------|-------------|--------------|---------|
| **Lokale Entwicklung** | Entwickler-Maschine | 0€ | Niedrig |
| **Hetzner Cloud** | Self-Hosted (VPS) | ~5€ | Mittel |
| **Render.com** | Managed Cloud | ~26€ | Niedrig |
| **Railway.app** | Managed Cloud | ~30€ | Niedrig |
| **On-Premise** | Kunden-Server | 0€ (nur Hardware) | Hoch |

### Deployment-Stages

Die Anwendung wird in drei Umgebungen deployed:

- **Dev**: Entwicklungs-Umgebung für lokale Entwicklung
- **Test**: Staging-Umgebung für Integrationstests auf Server
- **Prod**: Produktions-Umgebung für Live-Betrieb

---

## Branch-Strategie und Workflows

### Branch-Struktur

```
feature/* → dev → test → master
```

#### Branches

- **`dev`**: Entwicklungs-Branch
  - Läuft nur lokal auf Entwickler-Maschinen
  - Schnelles Testing mit H2 In-Memory: `./gradlew bootRun`
  - Realistisches Testing mit PostgreSQL Container: `docker-compose.dev.yml`
  - Auto-Build & Push Docker Image bei Push zu GitHub
  - **Keine** Server-Deployment (Ressourcen & Sicherheit)

- **`test`**: Staging-Branch
  - Verwendet PostgreSQL mit persistenter Datenbank auf Hetzner
  - Auto-CI & Build bei Push
  - **Manuelles Deployment** über GitHub Actions
  - Nur über VPN erreichbar auf Hetzner Server

- **`master`**: Production-ready Code
  - Nur stabile Releases nach ausgiebigem Testing
  - Verwendet PostgreSQL Production-Datenbank
  - Manuelles Deployment über GitHub Actions
  - Öffentlich erreichbar über Traefik/HTTPS

### Workflow-Prozess

1. **Feature-Entwicklung**:
   ```bash
   git checkout dev
   # Änderungen machen...
   git push origin dev
   # → GitHub Actions: Build & Push Image (ghcr.io/bbajor/pvs:dev-latest)
   ```

2. **Staging-Test**:
   ```bash
   git checkout test
   git merge dev
   git push origin test
   # → GitHub Actions: CI & Build
   # → Manuelles Deployment zu Test-Server
   ```

3. **Production**:
   ```bash
   git checkout master
   git merge test
   git push origin master
   # → Manuelles Deployment über GitHub Actions
   ```

---

## Lokale Entwicklung

### Option 1: H2 In-Memory (Schnell)

**Wann nutzen:**
- Schnelle Feature-Entwicklung
- Einfache Logik-Tests
- Keine Docker-Installation benötigt

**Setup:**
```bash
./gradlew bootRun
# App läuft auf http://localhost:8130
```

**Eigenschaften:**
- H2 In-Memory Datenbank (keine Persistenz)
- Schneller Start
- Daten gehen beim Neustart verloren

### Option 2: PostgreSQL Container (Realistisch)

**Wann nutzen:**
- Datenbank-Features testen
- Flyway-Migrationen testen
- Realistische Datenstrukturen benötigt

**Setup:**
```bash
# Environment-Datei erstellen
cp docker-compose.dev.env.example docker-compose.dev.env
# Bearbeite docker-compose.dev.env (setze Passwörter)

# Container starten
docker-compose -f docker-compose.dev.yml --env-file docker-compose.dev.env up -d

# App läuft auf http://localhost:8130
```

**Services:**
- **PostgreSQL**: Port `127.0.0.1:5434` (nur lokal)
- **PVS App**: Port `127.0.0.1:8130`
- **Whisper AI**: Port `127.0.0.1:9000` (optional)

### Automatisches Dev-Deployment (Optional)

#### Option A: Scheduled Task (Windows)

Mit PowerShell-Script `scripts/local/auto-update-dev.ps1`:

1. **Scheduled Task erstellen:**
   - Windows-Taste → "Aufgabengesteuerung"
   - Neue Aufgabe erstellen:
     - **Name**: `PVS Dev Auto-Update`
     - **Trigger**: Wiederholung alle 5 Minuten
     - **Aktion**: `powershell.exe -ExecutionPolicy Bypass -File "D:\workspace\pvs\scripts\local\auto-update-dev.ps1"`
     - **Starten in**: `D:\workspace\pvs`

2. **Funktionsweise:**
   - Push zu `dev` → GitHub Actions baut Image
   - Scheduled Task läuft alle 5 Minuten
   - Script pulled neues Image (falls verfügbar)
   - Vergleicht Image-IDs
   - Wenn neues Image: `docker compose up -d` → Deployment

#### Option B: Selbst-hosted GitHub Actions Runner

**Setup (Linux/macOS):**
```bash
cd ~
mkdir actions-runner && cd actions-runner
curl -o actions-runner.tar.gz -L https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz
tar xzf actions-runner.tar.gz

# Token holen: GitHub → Settings → Actions → Runners → New runner
./config.sh --url https://github.com/bbajor/pvs --token <TOKEN>

# Als Service installieren
sudo ./svc.sh install
sudo ./svc.sh start
```

**Setup (Windows):**
Siehe `docs/deployment/WINDOWS_RUNNER_SETUP.md` für vollständige Anleitung.

**Vorteile:**
- ✅ Sofortiges Deployment nach Push
- ✅ Keine Wartezeit durch Polling
- ✅ Sicher (kein öffentlicher SSH nötig)

---

## Deployment-Optionen

### Hetzner Cloud (Self-Hosted)

**Kosten: ~5€/Monat für alles!**

Die günstigste Option: Ein Hetzner VPS (CX21, 2 vCPU, 4GB RAM) hostet alle drei Deployment-Stages mit Docker Compose.

#### Voraussetzungen

1. Hetzner Cloud Account: [hetzner.com/cloud](https://www.hetzner.com/cloud)
2. GitHub Repository
3. Domain (optional, für SSL)

#### Quick Start (10 Minuten)

**Schritt 1: Hetzner VPS erstellen**

1. Gehe zu [hetzner.com/cloud](https://www.hetzner.com/cloud)
2. "Create Server" klicken
3. **Settings:**
   - Location: Nürnberg oder Falkenstein
   - Image: Ubuntu 22.04
   - Type: **CX21** (2 vCPU, 4GB RAM, 40GB SSD) - **5€/Monat**
   - SSH Key: Hinzufügen
4. "Create & Buy now" → Server wird erstellt
5. **IP-Adresse** notieren!

**Schritt 2: Server Setup**

```bash
ssh root@<DEINE_IP>

# Docker installieren
curl -fsSL https://get.docker.com -o get-docker.sh && sudo sh get-docker.sh

# Docker Compose installieren
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# User zu Docker-Gruppe hinzufügen
sudo usermod -aG docker $USER
newgrp docker

# Projekt-Verzeichnis erstellen
sudo mkdir -p /opt/pvs && sudo chown $USER:$USER /opt/pvs
cd /opt/pvs
```

**Schritt 3: Environment-Datei erstellen**

```bash
cat > .env <<EOF
POSTGRES_DB_DEV=pvs_dev
POSTGRES_USER_DEV=pvs_user
POSTGRES_PASSWORD_DEV=$(openssl rand -base64 32)

POSTGRES_DB_TEST=pvs_test
POSTGRES_USER_TEST=pvs_user
POSTGRES_PASSWORD_TEST=$(openssl rand -base64 32)

POSTGRES_DB_PROD=pvs_prod
POSTGRES_USER_PROD=pvs_user
POSTGRES_PASSWORD_PROD=$(openssl rand -base64 32)  # SICHER AUFBEWAHREN!

DOCKER_REGISTRY=ghcr.io
DOCKER_IMAGE=bbajor/pvs
LETSENCRYPT_EMAIL=deine@email.de
EOF

# Dateirechte schützen
chmod 600 .env
```

**Schritt 4: Docker Compose File kopieren**

Von deinem lokalen Rechner:
```bash
scp docker-compose.production.yml root@<DEINE_IP>:/opt/pvs/
```

**Schritt 5: Datenbank initialisieren**

```bash
cd /opt/pvs

# PostgreSQL starten
docker-compose -f docker-compose.production.yml --profile prod up -d postgres-prod

# Warten bis DB bereit ist
sleep 15

# Health Check
docker exec pvs-postgres-prod pg_isready -U pvs_user
```

**Schritt 6: GitHub Secrets konfigurieren**

GitHub Repository → Settings → Secrets and variables → Actions → New repository secret

Erstelle folgende Secrets:

```
HETZNER_HOST=<DEINE_IP>
HETZNER_USER=root
HETZNER_SSH_KEY=<Inhalt von ~/.ssh/hetzner_deploy>
PROD_DB_HOST=localhost
PROD_DB_NAME=pvs_prod
PROD_DB_USER=pvs_user
PROD_DB_PASSWORD=<Aus /opt/pvs/.env kopieren - POSTGRES_PASSWORD_PROD>
```

**SSH Key generieren:**
```bash
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/hetzner_deploy -N ""
cat ~/.ssh/hetzner_deploy.pub | ssh root@<DEINE_IP> "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

**Schritt 7: Erste Deployment testen**

1. GitHub Actions → "Deploy to Dev (Hetzner)" → Run workflow → Branch: `dev`
2. Warte auf erfolgreiche Ausführung
3. Auf Server prüfen:
```bash
docker-compose -f docker-compose.production.yml --profile dev ps
docker-compose -f docker-compose.production.yml --profile dev logs pvs-dev
curl http://localhost:8080/actuator/health
```

#### Domain & SSL (Optional)

1. **DNS A Records setzen:**
   ```
   dev.pvs.example.com  → <HETZNER_IP>
   test.pvs.example.com → <HETZNER_IP>
   pvs.example.com      → <HETZNER_IP>
   ```

2. **docker-compose.production.yml anpassen:**
   - Ersetze `pvs.example.com` mit deiner Domain

3. **Traefik starten:**
   ```bash
   docker-compose -f docker-compose.production.yml up -d traefik
   ```
   SSL wird automatisch von Let's Encrypt eingerichtet!

#### Vorteile

- ✅ Sehr günstig: 5€/Monat für alles
- ✅ Volle Kontrolle: Self-Hosted
- ✅ DSGVO-konform: Server in Deutschland
- ✅ Skalierbar: Kann später auf mehrere Server erweitert werden

#### Nachteile

- ⚠️ Mehr Setup-Aufwand
- ⚠️ Wartung: Updates, Backups, Monitoring selbst verwalten
- ⚠️ Single Point of Failure: Ein Server für alles

**Vollständige Anleitung:** Siehe `docs/deployment/HETZNER_COMPLETE_SETUP.md`

---

### Render.com (Managed)

**Kosten: ~26€/Monat**

Render.com ist eine Managed Cloud-Plattform mit automatischen Deployments.

#### Übersicht

- **Dev**: Free Tier (0€/Monat)
- **Test**: Starter Plan (7$/Monat) + PostgreSQL Starter (7$/Monat) = ~13€/Monat
- **Prod**: Starter Plan (7$/Monat) + PostgreSQL Starter (7$/Monat) = ~13€/Monat

**Gesamtkosten**: ~26€/Monat

#### Voraussetzungen

1. Render.com Account: [render.com](https://render.com)
2. GitHub Account mit Repository-Zugriff
3. Render API Key (Account Settings → API Keys)

#### Initial Setup

**Schritt 1: Render API Key erstellen**

1. Render Dashboard → Account Settings → API Keys
2. "Create API Key"
3. Speichere den Key sicher

**Schritt 2: GitHub Secrets konfigurieren**

GitHub Repository → Settings → Secrets and variables → Actions → New repository secret

**Erforderliche Secrets:**
- `RENDER_API_KEY`: Dein Render API Key
- `RENDER_DEV_SERVICE_ID`: Service ID für Dev (nach Setup in Render Dashboard)
- `RENDER_TEST_SERVICE_ID`: Service ID für Test
- `RENDER_PROD_SERVICE_ID`: Service ID für Prod
- `RENDER_TEST_URL`: Vollständige URL des Test-Services
- `RENDER_PROD_URL`: Vollständige URL des Prod-Services

**Schritt 3: Render Services erstellen**

**Via render.yaml (Empfohlen):**

1. Render Dashboard → "New" → "Blueprint"
2. Verbinde dein GitHub Repository
3. Render erkennt automatisch `render.yaml` und erstellt alle Services

**Manuell:**

1. **Dev Service:**
   - "New" → "Web Service"
   - Repository verbinden
   - Name: `pvs-dev`
   - Branch: `develop`
   - Plan: `Free`
   - Build Command: `./gradlew bootJar --no-daemon`
   - Start Command: `java -jar build/libs/*.jar`
   - Environment: `SPRING_PROFILES_ACTIVE=dev`

2. **Test Service:**
   - "New" → "Web Service"
   - Name: `pvs-test`
   - Branch: `main`
   - Plan: `Starter ($7/month)`
   - PostgreSQL Database hinzufügen: `pvs-test-db`
   - Environment: `SPRING_PROFILES_ACTIVE=test`

3. **Prod Service:**
   - "New" → "Web Service"
   - Name: `pvs-prod`
   - Branch: `main`
   - Plan: `Starter ($7/month)`
   - PostgreSQL Database hinzufügen: `pvs-prod-db`
   - Environment: `SPRING_PROFILES_ACTIVE=prod`
   - **Auto-Deploy**: Disabled (manuelles Deployment)

#### Deployment-Prozess

**Dev Deployment:**
- **Automatisch** bei jedem Push auf `develop` Branch

**Test Deployment:**
- **Automatisch** bei jedem Push auf `main` Branch
- Flyway Migration Validation
- Health Check Verification

**Production Deployment:**
- **Manuell** via GitHub Actions:
  1. GitHub Actions → "Deploy to Production (Render)"
  2. "Run workflow"
  3. Wähle Branch (`main`)
  4. Deployment läuft automatisch
  5. Health Check wird validiert

#### Monitoring

- **Health Checks:**
  - Dev: `https://pvs-dev.onrender.com/actuator/health`
  - Test: `https://pvs-test.onrender.com/actuator/health`
  - Prod: `https://pvs-prod.onrender.com/actuator/health`

- **Logs:** Render Dashboard → Service → "Logs"

**Vollständige Anleitung:** Siehe `docs/deployment/README.md` (Render-spezifisch)

---

### Railway.app (Alternative)

**Kosten: ~30$/Monat (~27€)**

Alternative zu Render.com mit ähnlicher Funktionalität.

#### Setup

1. Railway Account: [railway.app](https://railway.app)
2. GitHub Integration aktivieren
3. Services erstellen (3 Environments + PostgreSQL)
4. Railway Token für GitHub Actions generieren

**Vollständige Anleitung:** Siehe `docs/deployment/RAILWAY_SETUP.md`

#### Vorteile

- ✅ Kostenlose Build-Pipelines
- ✅ GitHub Integration
- ✅ EU-Server verfügbar
- ✅ Docker-Support

---

### On-Premise

Für Kunden, die die Anwendung lokal hosten möchten.

#### Voraussetzungen

- Docker & Docker Compose installiert
- Mind. 4GB RAM verfügbar
- Mind. 20GB freier Speicherplatz
- Port 8080 verfügbar

#### Quick Start

```bash
# Klone Repository
git clone <repository-url>
cd pvs

# Konfiguriere Environment
cp .env.example .env
# Editiere .env mit lokalen Einstellungen

# Starte Services
docker-compose up -d

# Warte auf Startup (ca. 2-3 Minuten)
docker-compose logs -f pvs-app

# Öffne Browser: http://localhost:8080
```

#### Whisper AI Service (Optional)

**Aktivieren:**
```yaml
# In docker-compose.yml oder .env:
AI_WHISPER_LOCAL_ENABLED=true
```

**Deaktivieren:**
```yaml
AI_WHISPER_LOCAL_ENABLED=false
```

#### Update-Prozess

1. Stoppe Services: `docker-compose down`
2. Backup erstellen: `docker-compose exec pvs-db pg_dump -U pvs_user pvs > backup.sql`
3. Update Images: `git pull && docker-compose pull && docker-compose build`
4. Starte Services: `docker-compose up -d`

**Vollständige Anleitung:** Siehe `docs/deployment/ON_PREMISE.md`

---

## Datenbank-Architektur

### Separate Container für Dev/Test/Prod

**Warum separate Container?**

1. **Sicherheit**: Keine Datenvermischung zwischen Environments
2. **Flexibilität**: Prod kann mehr Ressourcen bekommen
3. **Testbarkeit**: Test-DB kann einfach zurückgesetzt werden
4. **Wartbarkeit**: Alles in docker-compose.yml dokumentiert

### Container-Konfiguration

```yaml
# Separate Container pro Environment
postgres-dev:    Port 5433, Volume: postgres-data-dev
postgres-test:   Port 5434, Volume: postgres-data-test  
postgres-prod:   Port 5435, Volume: postgres-data-prod (4GB RAM, 2 CPUs)
```

### Resource-Limits

- **Production**: 4GB RAM, 2 CPUs (Performance)
- **Test**: 1GB RAM, 0.5 CPUs (Sparsam)
- **Dev**: 1GB RAM, 0.5 CPUs (Sparsam)

### Migrationen (Flyway)

Die Anwendung nutzt Flyway für Database-Versionierung:

- **Dev**: Flyway disabled (H2 in-memory, create-drop)
- **Test/Prod**: Flyway enabled, migrations werden automatisch ausgeführt

**Neue Migration erstellen:**

1. Erstelle SQL-Datei: `src/main/resources/db/migration/V2__your_migration_name.sql`
2. Migration wird automatisch beim nächsten Deployment ausgeführt
3. Beachte: Migrations müssen rückwärtskompatibel sein (oder Rollback vorbereiten)

**Migration-Syntax:**
- `V1__initial.sql` → Version 1, Beschreibung
- `V2__add_user_table.sql` → Version 2, Beschreibung
- Versionsnummer muss sequenziell sein

**Vollständige Dokumentation:** Siehe `docs/deployment/DATABASE_ARCHITECTURE.md`

---

## CI/CD und Build-Optimierung

### GitHub Actions Workflows

Die Codebase nutzt mehrere GitHub Actions Workflows:

- **Dev Branch CI & Build**: Automatischer Build bei Push zu `dev`
- **Test Branch CI & Build**: Automatischer Build bei Push zu `test`
- **Deploy to Dev (Hetzner)**: Deployment zu Hetzner Dev-Environment
- **Deploy to Test (Hetzner)**: Deployment zu Hetzner Test-Environment
- **Deploy Production (Hetzner)**: Manuelles Deployment zu Production

### Build-Optimierungen

**Implementierte Optimierungen:**

1. **Skip-Build bei non-code Änderungen**: Nur bei Java/Gradle/Config-Änderungen wird gebaut
2. **Tests nur einmal ausführen**: `./gradlew build -x test` + `./gradlew test` separat
3. **Gradle Build-Cache**: `--build-cache` und `--parallel` aktiviert
4. **Docker Layer-Caching**: GitHub Actions Cache für Docker-Images
5. **BootJar Cache-Optimierung**: Inkrementelle Builds

**Erwartete Build-Zeiten:**

- **Vorher:**
  - Code-Änderung: ~15-20 Minuten
  - Non-code Änderung: ~15-20 Minuten (unnötig!)

- **Nachher:**
  - Code-Änderung: ~8-12 Minuten (mit Cache: ~5-8 Minuten)
  - Non-code Änderung: ~1-2 Minuten (nur Workflow, kein Build)

**Vollständige Dokumentation:** Siehe `docs/deployment/BUILD_OPTIMIERUNG.md`

---

## Monitoring und Wartung

### Health Checks

- **Dev**: `http://localhost:8080/actuator/health` (lokal) oder `https://pvs-dev.onrender.com/actuator/health`
- **Test**: `https://pvs-test.onrender.com/actuator/health` oder Hetzner Server
- **Prod**: `https://pvs-prod.onrender.com/actuator/health` oder öffentliche Domain

**Erwartete Antwort:**
```json
{"status":"UP"}
```

### Logs

**Hetzner Server:**
```bash
# Alle Services
docker-compose -f docker-compose.production.yml logs -f

# Spezifischer Service
docker-compose -f docker-compose.production.yml logs -f pvs-prod
```

**Render.com:**
- Render Dashboard → Service → "Logs"

### Metrics

- Spring Actuator: `/actuator/metrics`
- Flyway Status: `/actuator/flyway` (nur Test/Prod)

### Container-Status

```bash
# Auf Hetzner Server
docker-compose -f docker-compose.production.yml ps
docker stats  # Ressourcen-Verbrauch
```

### Backup-Strategie

**Automatische Backups (Hetzner):**

```bash
# Backup-Script (in Cron)
0 2 * * * docker exec pvs-postgres-prod pg_dump -U pvs_user pvs_prod | gzip > /opt/pvs/backups/pvs_prod_$(date +\%Y\%m\%d).sql.gz
```

**Render.com:**
- Automatische tägliche Backups (7 Tage Retention)
- Zugriff: Render Dashboard → Database → "Backups"

**Manuelles Backup:**
```bash
# Via pg_dump
docker exec pvs-postgres-prod pg_dump -U pvs_user pvs_prod > backup.sql

# Restore
docker exec -i pvs-postgres-prod psql -U pvs_user -d pvs_prod < backup.sql
```

---

## Rollback und Backup

### Automatisches Rollback

**Wann wird automatisch zurückgerollt?**

1. **Health Check Failure** nach Deployment
   - Render erkennt automatisch Health Check Failures
   - Alte Version wird automatisch reaktiviert

2. **Migration-Fehler (Flyway)**
   - Flyway führt Migrations in Transaktionen aus
   - Bei Fehler: Automatisches Rollback der Migration
   - App bleibt bei alter Version

3. **Application Startup Failure**
   - Render erkennt fehlgeschlagenen Start
   - Vorherige Version wird reaktiviert

### Manuelles Rollback

**Via GitHub Actions (Empfohlen):**

1. GitHub Actions → "Rollback Production"
2. "Run workflow"
3. Confirmation: `ROLLBACK` eingeben
4. Rollback wird ausgeführt
5. Health Check wird validiert

**Via Render Dashboard:**
1. Render Dashboard → Service → "Events"
2. "Rollback to previous deploy" klicken

**Via Hetzner Server:**
```bash
cd /opt/pvs
docker-compose -f docker-compose.production.yml stop pvs-prod
docker tag ghcr.io/bbajor/pvs:prod-backup-YYYYMMDD-HHMMSS ghcr.io/bbajor/pvs:prod-latest
docker-compose -f docker-compose.production.yml up -d pvs-prod
```

### Database Migration Rollback

Wenn eine Migration Probleme verursacht:

1. Erstelle Rollback-Migration: `V3__rollback_v2.sql`
2. Führe Rollback-Migration aus
3. In Flyway Schema History eintragen

**Besser**: Verwende immer reversible Migrations!

**Vollständige Dokumentation:** Siehe `docs/deployment/ROLLBACK.md`

---

## Sicherheit und Compliance

### DSGVO-Compliance

**Datenverarbeitung in der EU:**
- ✅ Render.com EU-Rechenzentren (Frankfurt, Ireland)
- ✅ Hetzner Server in Deutschland (Nürnberg, Falkenstein)
- ✅ Railway.app EU-Server verfügbar

**Ende-zu-Ende Verschlüsselung:**
- ✅ HTTPS ist Standard (automatische SSL-Zertifikate)
- ✅ Database Connections: PostgreSQL unterstützt SSL
- ✅ API-Kommunikation über HTTPS

**Datenminimierung:**
- ✅ Production Logging: `WARN` Level nur
- ✅ Keine sensiblen Daten in Logs
- ✅ Log Retention: 30 Tage (Render)

**Zugriffskontrolle:**
- ✅ Spring Security aktiviert
- ✅ User-basierte Zugriffsrechte
- ✅ Passwörter: BCrypt-Hashing
- ✅ Alle Secrets in GitHub Secrets / Environment Variables

**Firewall (Hetzner):**
```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp  # SSH
sudo ufw allow 80/tcp  # HTTP
sudo ufw allow 443/tcp # HTTPS
sudo ufw enable
```

### Sicherheits-Checkliste

- [ ] SSH-Key Authentication aktiviert (kein Passwort-Login)
- [ ] Firewall aktiviert (nur 22, 80, 443)
- [ ] Starke Passwörter in `.env` (32+ Zeichen, zufällig)
- [ ] `.env` Datei nicht in Git (in `.gitignore`)
- [ ] `.env` Dateirechte: `600` (nur Owner lesbar)
- [ ] PostgreSQL Ports nur auf `127.0.0.1` gebunden
- [ ] Dev/Test Container nicht öffentlich erreichbar
- [ ] Nur Prod über Traefik/HTTPS öffentlich
- [ ] SSL/TLS aktiviert (Traefik + Let's Encrypt)
- [ ] Regelmäßige Backups eingerichtet

**Vollständige Dokumentation:** Siehe `docs/deployment/DSGVO.md`

---

## Troubleshooting

### Deployment schlägt fehl

**1. Prüfe GitHub Actions Logs**
- GitHub Repository → Actions → Workflow → Logs prüfen

**2. Prüfe Render/Server Logs**
```bash
# Hetzner Server
docker-compose -f docker-compose.production.yml logs pvs-prod

# Render
Render Dashboard → Service → "Logs"
```

**3. Validiere Build lokal**
```bash
./gradlew bootJar --no-daemon
```

**4. Prüfe Environment Variables**
- GitHub Secrets prüfen
- Render Dashboard → Service → Environment
- Hetzner Server: `.env` Datei prüfen

### Database Connection Fehler

**1. Prüfe DATABASE_URL Environment Variable**
```bash
# Auf Hetzner Server
docker exec pvs-prod env | grep DATABASE
```

**2. Prüfe PostgreSQL Service Status**
```bash
docker-compose -f docker-compose.production.yml ps postgres-prod
docker exec pvs-postgres-prod pg_isready -U pvs_user
```

**3. Validiere Connection String Format**
- Format: `jdbc:postgresql://host:port/dbname`

### Migration Fehler

**1. Prüfe Flyway Logs**
- Endpoint: `/actuator/flyway` (Test/Prod)
- Render/Server Logs prüfen

**2. Prüfe Migration SQL Syntax**
```bash
# Lokal testen
./gradlew flywayMigrate --info
```

**3. Bei Baseline-Problemen**
```bash
# Lokale DB testen
flyway baseline
```

### Application startet nicht

**1. Prüfe Java Memory Settings**
```bash
# JAVA_OPTS Environment Variable prüfen
echo $JAVA_OPTS
```

**2. Prüfe Port-Konfiguration**
```bash
# PORT Environment Variable
echo $PORT  # Sollte 8080 sein
```

**3. Prüfe Spring Profile**
```bash
# SPRING_PROFILES_ACTIVE prüfen
echo $SPRING_PROFILES_ACTIVE
```

**4. Prüfe Logs für Stack Traces**
```bash
docker-compose -f docker-compose.production.yml logs pvs-prod | tail -100
```

### Port bereits belegt

```bash
# Prüfe welcher Prozess den Port nutzt
sudo netstat -tulpn | grep :8080
# Oder
docker ps | grep :8080
```

### Private Docker Images

Falls das Docker Image privat ist, musst du dich authentifizieren:

**GitHub Personal Access Token:**

1. GitHub → Settings → Developer settings → Personal access tokens
2. Neuen Token erstellen mit `read:packages` Scope
3. Docker Login:
```bash
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
```

**Vollständige Anleitung:** Siehe `docs/deployment/DOCKER_PRIVATE_IMAGES.md`

---

## Zusammenfassung

### Empfehlungen

**Für Budget-Optimierung (<10€/Monat):**
- ✅ **Hetzner Cloud + GitHub Actions** (5€/Monat)
- Volle Kontrolle, mehr Setup

**Für Einfachheit + Preis (~26€/Monat):**
- ✅ **Render.com** (26€/Monat)
- Automatische Deployments, wenig Konfiguration

**Für lokale Entwicklung:**
- ✅ **H2 In-Memory** für schnelle Tests
- ✅ **PostgreSQL Container** für realistische Tests
- ✅ **Scheduled Task oder Runner** für automatisches Dev-Deployment

### Wichtige Dateien

- `docker-compose.production.yml`: Production-Setup für Hetzner
- `docker-compose.dev.yml`: Lokale Entwicklung
- `render.yaml`: Render.com Konfiguration
- `Dockerfile`: Multi-Stage Build für optimierte Images
- `.env` / `docker-compose.dev.env`: Environment-Variablen (nicht in Git!)

### Nächste Schritte

1. **Wähle Deployment-Option** basierend auf Budget und Anforderungen
2. **Führe Setup durch** gemäß entsprechender Anleitung
3. **Teste Deployment** mit Dev-Environment
4. **Konfiguriere Backups** und Monitoring
5. **Sicherheits-Checkliste** durchgehen

### Support

Bei Problemen:
1. Prüfe Troubleshooting-Sektion
2. Prüfe spezifische Deployment-Dokumentationen in `docs/deployment/`
3. Prüfe GitHub Actions Logs
4. Prüfe Server/Render Logs

**Viel Erfolg beim Deployment!** 🚀

