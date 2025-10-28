# Railway.app Setup Guide

## Voraussetzungen

1. Railway Account: [railway.app](https://railway.app)
2. GitHub Integration aktivieren

## Setup für 3 Stages

### 1. Dev Environment

1. Railway Dashboard → "New Project"
2. "Deploy from GitHub repo"
3. Repository auswählen
4. Service erstellen:
   - **Name**: `pvs-dev`
   - **Branch**: `develop` (oder erstelle Branch)
   - **Variables**:
     ```
     SPRING_PROFILES_ACTIVE=dev
     AI_WHISPER_LOCAL_ENABLED=false
     ```
5. Plan: **Hobby** (5$/Monat, oder Free Tier wenn <5$ Credit reicht)

### 2. Test Environment

1. "New Service" in gleichem/different Project
2. **Name**: `pvs-test`
3. **Branch**: `main`
4. **Add PostgreSQL**:
   - Railway Dashboard → Service → "+ New" → "Database" → "PostgreSQL"
   - Name: `pvs-test-db`
5. **Variables**:
   ```
   SPRING_PROFILES_ACTIVE=test
   AI_WHISPER_LOCAL_ENABLED=false
   DATABASE_URL=${{Postgres.pvs-test-db.DATABASE_URL}}
   ```
   (DATABASE_URL wird automatisch von Railway gesetzt)
6. Plan: **Hobby** (5$/Monat)

### 3. Production Environment

1. "New Service" 
2. **Name**: `pvs-prod`
3. **Branch**: `main`
4. **Add PostgreSQL**: `pvs-prod-db`
5. **Variables**:
   ```
   SPRING_PROFILES_ACTIVE=prod
   AI_WHISPER_LOCAL_ENABLED=false
   DATABASE_URL=${{Postgres.pvs-prod-db.DATABASE_URL}}
   ```
6. Plan: **Starter** (20$/Monat)
7. **Auto-Deploy**: DEAKTIVIEREN (manuelles Deployment)

### 4. GitHub Actions anpassen

GitHub Actions müssen Railway statt Render nutzen:

```yaml
# .github/workflows/deploy-test.yml (Beispiel)
- name: Deploy to Railway
  uses: bervProject/railway-deploy@v0.2.3
  with:
    railway_token: ${{ secrets.RAILWAY_TOKEN }}
    service: pvs-test
```

## Kosten

- **Dev**: Hobby (5$/Monat) oder Free Tier
- **Test**: Hobby (5$/Monat) + PostgreSQL (5$/Monat)
- **Prod**: Starter (20$/Monat) + PostgreSQL (5$/Monat)
- **Gesamt: ~30-40$/Monat**

## Railway Token für GitHub Actions

1. Railway Dashboard → Account Settings → "Tokens"
2. "Create Token"
3. Als GitHub Secret speichern: `RAILWAY_TOKEN`

## Deployment

### Automatisch:
- Push auf `develop` → Dev Deployment
- Push auf `main` → Test Deployment

### Manuell (Prod):
- Railway Dashboard → Service → "Deploy"

