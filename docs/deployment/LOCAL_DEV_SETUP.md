# Lokales Development Setup

Die Dev-Stage läuft ausschließlich lokal auf deiner Entwickler-Maschine mit PostgreSQL in Docker Containern.

## 🚀 Quick Start

### Voraussetzungen

- Docker & Docker Compose installiert
- Git Repository geklont

### Setup

**1. Environment-Datei erstellen:**

```bash
# Kopiere Beispiel-Datei
cp docker-compose.dev.env.example docker-compose.dev.env

# Bearbeite die Datei (setze Passwörter)
nano docker-compose.dev.env
# Oder auf Windows: notepad docker-compose.dev.env
```

**2. Container starten:**

```bash
# Starte alle Dev-Services (PostgreSQL + PVS App + Whisper)
docker-compose -f docker-compose.dev.yml --env-file docker-compose.dev.env up -d

# Prüfe Status
docker-compose -f docker-compose.dev.yml ps

# Logs ansehen
docker-compose -f docker-compose.dev.yml logs -f
```

**3. Applikation aufrufen:**

```
http://localhost:8130
```

**4. Container stoppen:**

```bash
docker-compose -f docker-compose.dev.yml down

# Mit Volumes löschen (Datenbank-Daten werden gelöscht!)
docker-compose -f docker-compose.dev.yml down -v
```

## 📋 Services

### PostgreSQL (postgres-dev)

- **Port**: `127.0.0.1:5432` (nur lokal erreichbar)
- **Database**: `pvs_dev` (oder aus `docker-compose.dev.env`)
- **User**: `pvs_user` (oder aus `docker-compose.dev.env`)
- **Password**: Aus `docker-compose.dev.env`

**Verbindung testen:**
```bash
docker exec pvs-postgres-dev-local psql -U pvs_user -d pvs_dev -c "SELECT version();"
```

### PVS Application (pvs-app-dev)

- **Port**: `127.0.0.1:8130` (nur lokal erreichbar, Port 8080 wird von OpenWebUI verwendet)
- **Profile**: `dev`
- **Health Check**: `http://localhost:8130/actuator/health`

### Whisper AI (optional)

- **Port**: `127.0.0.1:9000` (nur lokal erreichbar)
- Wird automatisch gestartet falls benötigt

## 🔧 Konfiguration

### Datenbank-Credentials ändern

```bash
# Öffne Environment-Datei
nano docker-compose.dev.env

# Ändere:
# POSTGRES_PASSWORD_DEV=neues_passwort

# Restarte Container
docker-compose -f docker-compose.dev.yml restart postgres-dev
```

### Datenbank-Reset

```bash
# Container stoppen und Volumes löschen
docker-compose -f docker-compose.dev.yml down -v

# Neu starten
docker-compose -f docker-compose.dev.yml --env-file docker-compose.dev.env up -d
```

### Datenbank-Backup

```bash
# Backup erstellen
docker exec pvs-postgres-dev-local pg_dump -U pvs_user pvs_dev > backup_$(date +%Y%m%d).sql

# Restore
docker exec -i pvs-postgres-dev-local psql -U pvs_user -d pvs_dev < backup_20240101.sql
```

## 🔍 Troubleshooting

### Container startet nicht

```bash
# Prüfe Logs
docker-compose -f docker-compose.dev.yml logs pvs-app-dev

# Prüfe ob Port bereits belegt
netstat -an | grep 8130  # Linux/Mac
netstat -ano | findstr :8130  # Windows
```

### Datenbank-Verbindung fehlgeschlagen

```bash
# Prüfe ob PostgreSQL läuft
docker-compose -f docker-compose.dev.yml ps postgres-dev

# Prüfe Logs
docker-compose -f docker-compose.dev.yml logs postgres-dev

# Teste Verbindung
docker exec pvs-postgres-dev-local pg_isready -U pvs_user
```

### Port bereits belegt

Falls Port 8130 oder 5432 bereits belegt ist, passe die Ports in `docker-compose.dev.yml` an:

```yaml
services:
  postgres-dev:
    ports:
      - "127.0.0.1:5433:5432"  # Ändere zu 5433
  
  pvs-app-dev:
    ports:
      - "127.0.0.1:8131:8130"  # Ändere zu 8131
    environment:
      PORT: 8131  # Interner Port auch ändern
```

## 🔒 Sicherheit

- ✅ Alle Ports nur auf `127.0.0.1` gebunden (nur lokal erreichbar)
- ✅ Keine öffentliche Erreichbarkeit
- ✅ Environment-Dateien sollten in `.gitignore` sein
- ⚠️  Dev-Passwörter sind weniger sicher (für lokale Entwicklung OK)

## 📝 Nächste Schritte

1. **Feature entwickeln** auf `dev` Branch
2. **Lokal testen** mit docker-compose.dev.yml
3. **Push zu dev** → GitHub Actions führt CI aus (kein Deployment)
4. **Merge zu test** → Deployment zu Hetzner Server (mit PostgreSQL)

## ⚡ Tipps

**Hot-Reload für Development:**

Statt Container zu nutzen, starte direkt in IDE:

```bash
# Nur PostgreSQL Container starten
docker-compose -f docker-compose.dev.yml up -d postgres-dev

# App lokal in IDE starten (nutzt Container-DB)
# Setze in Run Configuration:
# DATABASE_URL=jdbc:postgresql://localhost:5432/pvs_dev
```

**Separate Datenbanken pro Feature:**

```bash
# Feature-Branch spezifische DB
docker-compose -f docker-compose.dev.yml \
  -e POSTGRES_DB_DEV=pvs_dev_feature-xyz \
  up -d postgres-dev
```

