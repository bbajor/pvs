# Deployment-Dokumentation für PVS

Diese Dokumentation beschreibt den kompletten Deployment-Prozess für die PVS-Anwendung auf Render.com mit 3-Stage-Setup (Dev/Test/Prod).

## Übersicht

Die Anwendung wird in drei Umgebungen deployed:

- **Dev**: Automatisches Deployment bei Push auf `develop` Branch (Free Tier)
- **Test**: Automatisches Deployment bei Push auf `main` Branch (Starter Plan)
- **Prod**: Manuelles Deployment nur nach expliziter Freigabe (Starter Plan)

## Kostenstruktur

- **Dev**: Free Tier (0€/Monat)
- **Test**: Starter Plan (7$/Monat) + PostgreSQL Starter (7$/Monat) = ~13€/Monat
- **Prod**: Starter Plan (7$/Monat) + PostgreSQL Starter (7$/Monat) = ~13€/Monat

**Gesamtkosten**: ~26€/Monat

## Voraussetzungen

1. Render.com Account ([render.com](https://render.com))
2. GitHub Account mit Repository-Zugriff
3. Render API Key (erstellt in Render Dashboard → Account Settings → API Keys)

## Initial Setup

### 1. Render.com Account einrichten

1. Gehe zu [render.com](https://render.com) und erstelle einen Account
2. Verbinde dein GitHub Repository
3. Erstelle einen API Key:
   - Account Settings → API Keys
   - "Create API Key"
   - Speichere den Key sicher

### 2. GitHub Secrets konfigurieren

Füge folgende Secrets in deinem GitHub Repository hinzu (Settings → Secrets and variables → Actions):

**Erforderliche Secrets:**
- `RENDER_API_KEY`: Dein Render API Key
- `RENDER_DEV_SERVICE_ID`: Service ID für Dev-Environment (wird nach erstem Setup in Render Dashboard angezeigt)
- `RENDER_TEST_SERVICE_ID`: Service ID für Test-Environment
- `RENDER_PROD_SERVICE_ID`: Service ID für Prod-Environment
- `RENDER_TEST_URL`: Vollständige URL des Test-Services (z.B. `pvs-test.onrender.com`)
- `RENDER_PROD_URL`: Vollständige URL des Prod-Services (z.B. `pvs-prod.onrender.com`)

**Optional - für Flyway Validation in CI/CD:**
- `TEST_DB_HOST`: PostgreSQL Host für Test-DB
- `TEST_DB_NAME`: PostgreSQL Datenbankname
- `TEST_DB_USER`: PostgreSQL Username
- `TEST_DB_PASSWORD`: PostgreSQL Password
- `PROD_DB_HOST`: PostgreSQL Host für Prod-DB
- `PROD_DB_NAME`: PostgreSQL Datenbankname
- `PROD_DB_USER`: PostgreSQL Username
- `PROD_DB_PASSWORD`: PostgreSQL Password

### 3. Render Services erstellen

**Option A: Via render.yaml (Empfohlen)**

1. Render Dashboard → "New" → "Blueprint"
2. Verbinde dein GitHub Repository
3. Render erkennt automatisch `render.yaml` und erstellt alle Services

**Option B: Manuell**

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
   - Repository verbinden
   - Name: `pvs-test`
   - Branch: `main`
   - Plan: `Starter ($7/month)`
   - PostgreSQL Database hinzufügen: `pvs-test-db`
   - Environment: `SPRING_PROFILES_ACTIVE=test`

3. **Prod Service:**
   - "New" → "Web Service"
   - Repository verbinden
   - Name: `pvs-prod`
   - Branch: `main`
   - Plan: `Starter ($7/month)`
   - PostgreSQL Database hinzufügen: `pvs-prod-db`
   - Environment: `SPRING_PROFILES_ACTIVE=prod`
   - **Auto-Deploy**: Disabled (manuelles Deployment)

4. **PostgreSQL Databases:**
   - "New" → "PostgreSQL"
   - Name: `pvs-test-db`, Plan: `Starter`
   - Name: `pvs-prod-db`, Plan: `Starter`
   - Verbinde mit entsprechenden Services

## Deployment-Prozess

### Dev Deployment

**Automatisch** bei jedem Push auf `develop` Branch:
1. Build & Tests laufen
2. Deployment zu Render Dev-Service

### Test Deployment

**Automatisch** bei jedem Push auf `main` Branch:
1. Build & Tests laufen
2. Flyway Migration Validation
3. Deployment zu Render Test-Service
4. Health Check Verification

### Production Deployment

**Manuell** via GitHub Actions:

1. Gehe zu GitHub Actions → "Deploy to Production (Render)"
2. Klicke "Run workflow"
3. Wähle Branch (`main`)
4. (Optional) Skip tests (nur im Notfall)
5. Warte auf Approval (falls konfiguriert)
6. Deployment läuft automatisch
7. Health Check wird validiert

**Alternativ via Render Dashboard:**
- Test/Prod Service → "Manual Deploy" → Branch auswählen

## Database Migrations

Die Anwendung nutzt Flyway für Database-Versionierung:

- **Dev**: Flyway disabled (H2 in-memory, create-drop)
- **Test/Prod**: Flyway enabled, migrations werden automatisch ausgeführt

**Neue Migration erstellen:**

1. Erstelle SQL-Datei: `src/main/resources/db/migration/V2__your_migration_name.sql`
2. Migration wird automatisch beim nächsten Deployment ausgeführt
3. Beachte: Migrations müssen rückwärtskompatibel sein (oder Rollback vorbereiten)

**Migration-Syntax:**
- `V1__initial.sql`` → Version 1, Beschreibung
- `V2__add_user_table.sql` → Version 2, Beschreibung
- Versionsnummer muss sequenziell sein

## Monitoring

### Health Checks

- Dev: `https://pvs-dev.onrender.com/actuator/health`
- Test: `https://pvs-test.onrender.com/actuator/health`
- Prod: `https://pvs-prod.onrender.com/actuator/health`

### Logs

- Render Dashboard → Service → "Logs"
- Oder via Render CLI: `render logs -s <service-id>`

### Metrics

- Spring Actuator: `/actuator/metrics`
- Flyway Status: `/actuator/flyway` (nur Test/Prod)

## Troubleshooting

### Deployment schlägt fehl

1. Prüfe GitHub Actions Logs
2. Prüfe Render Service Logs
3. Validere Build lokal: `./gradlew bootJar`
4. Prüfe Environment Variables in Render Dashboard

### Database Connection Fehler

1. Prüfe DATABASE_URL Environment Variable
2. Prüfe PostgreSQL Service Status
3. Validere Connection String Format: `postgresql://user:pass@host:port/dbname`

### Migration Fehler

1. Prüfe Flyway Logs: `/actuator/flyway`
2. Prüfe Migration SQL Syntax
3. Bei Baseline-Problemen: `flyway baseline` auf lokaler DB testen
4. Siehe [ROLLBACK.md](ROLLBACK.md) für Rollback-Prozess

### Application startet nicht

1. Prüfe Java Memory Settings (JAVA_OPTS)
2. Prüfe Port-Konfiguration (PORT=8080)
3. Prüfe Spring Profile (SPRING_PROFILES_ACTIVE)
4. Prüfe Logs für Stack Traces

## On-Premise Deployment

Für Kunden, die die Anwendung lokal hosten möchten:

Siehe [docs/deployment/ON_PREMISE.md](ON_PREMISE.md) für Docker-Compose Setup.

