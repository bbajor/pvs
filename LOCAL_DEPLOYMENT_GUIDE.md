# Local Deployment Guide

**So deployest du die PVS-Applikation lokal**

---

## 🚀 Quick Start (Dev-Modus)

### 1. Voraussetzungen prüfen

```bash
# Docker & Docker-Compose installiert?
docker --version
docker compose version

# Java 21 installiert?
java -version

# Gradle (optional, gradlew ist enthalten)
./gradlew --version
```

---

## 📦 Option 1: Docker-Compose (Empfohlen)

### Schritt 1: Environment-Variablen

```bash
# .env wurde bereits erstellt (siehe .env im Root)
cat .env

# Optional: Anpassen
nano .env
```

### Schritt 2: Container bauen und starten

```bash
# PostgreSQL + Whisper + App starten
docker compose up -d

# Oder nur App (ohne Whisper)
docker compose up -d postgres pvs-app

# Logs verfolgen
docker compose logs -f pvs-app
```

### Schritt 3: Zugriff

```bash
# App ist erreichbar unter:
# http://localhost:8080

# Health-Check
curl http://localhost:8080/actuator/health

# PostgreSQL (nur localhost)
psql -h localhost -p 5433 -U pvs_user -d pvs_dev
```

### Stoppen

```bash
# Alle Container stoppen
docker compose down

# Container + Volumes löschen (Fresh Start)
docker compose down -v
```

---

## 🔨 Option 2: Direkter Start (ohne Docker)

### Schritt 1: PostgreSQL starten

```bash
# Entweder Docker-PostgreSQL
docker run -d \
  --name pvs-postgres-dev \
  -e POSTGRES_DB=pvs_dev \
  -e POSTGRES_USER=pvs_user \
  -e POSTGRES_PASSWORD=dev_password_123 \
  -p 5432:5432 \
  postgres:15-alpine

# Oder lokale PostgreSQL-Installation
```

### Schritt 2: Application Properties anpassen

```bash
# src/main/resources/application.yaml
# Setze spring.profiles.active: dev
```

### Schritt 3: Gradle Build & Start

```bash
# Build
./gradlew clean build

# Starten (Dev-Modus)
./gradlew bootRun

# Oder mit JAR
java -jar build/libs/pvs-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### Schritt 4: Zugriff

```bash
# App läuft auf
http://localhost:8080
```

---

## 🔒 Option 3: Production-Like (mit Traefik)

### Schritt 1: Production-Compose

```bash
# Kopiere .env.production.example
cp .env.production.example .env

# Editiere .env
nano .env
```

### Schritt 2: Starte Production-Stack

```bash
# PostgreSQL + Traefik + App
docker compose -f docker-compose.production.yml --profile prod up -d

# Logs
docker compose -f docker-compose.production.yml logs -f
```

### Schritt 3: Zugriff

```bash
# Traefik
http://localhost:80  # Redirect zu HTTPS (Self-Signed Cert für lokal)

# Health-Check
curl http://localhost:8080/actuator/health
```

---

## 🧪 Testing nach Deployment

### Health-Check

```bash
# Basic Health
curl http://localhost:8080/actuator/health

# Expected:
# {"status":"UP"}
```

### Security Headers testen

```bash
# Security Headers prüfen
curl -I http://localhost:8080

# Expected Headers:
# Content-Security-Policy
# X-Frame-Options: DENY
# X-Content-Type-Options: nosniff
```

### Database Connection

```bash
# PostgreSQL Connection testen
docker exec -it pvs-postgres-dev psql -U pvs_user -d pvs_dev -c "SELECT 1"

# Expected:
# ?column?
# ----------
#         1
```

### Logs prüfen

```bash
# App-Logs
docker compose logs pvs-app | tail -50

# PostgreSQL-Logs
docker compose logs postgres | tail -20

# Security-Events (wenn in Production)
docker exec pvs-prod cat /tmp/security-events.log
```

---

## 🔧 Troubleshooting

### Problem: Port 8080 bereits belegt

```bash
# Ändere Port in .env
PORT=8081

# Oder stoppe andere App
lsof -i :8080
kill <PID>
```

### Problem: PostgreSQL Connection Failed

```bash
# Prüfe ob PostgreSQL läuft
docker ps | grep postgres

# Prüfe Logs
docker logs pvs-postgres-dev

# Restart
docker restart pvs-postgres-dev
```

### Problem: Build-Fehler

```bash
# Clean Build
./gradlew clean build --no-daemon

# Ohne Tests
./gradlew build -x test --no-daemon

# Dependency-Refresh
./gradlew build --refresh-dependencies
```

### Problem: Docker-Build langsam

```bash
# Multi-Stage-Build ist optimiert, aber beim ersten Mal langsam
# Nutze Build-Cache
docker compose build --parallel

# Oder: Nur geänderte Services bauen
docker compose build pvs-app
```

---

## 📊 Verfügbare Endpoints

### Application

| Endpoint | Beschreibung |
|----------|--------------|
| `http://localhost:8080` | Hauptapplikation (Vaadin UI) |
| `http://localhost:8080/actuator/health` | Health-Check |
| `http://localhost:8080/actuator/prometheus` | Prometheus Metrics |
| `http://localhost:8080/api/ai/**` | AI-Endpoints (wenn enabled) |

### Database

| Service | Port | Credentials |
|---------|------|-------------|
| PostgreSQL Dev | 5433 | pvs_user / dev_password_123 |
| PostgreSQL Test | 5434 | pvs_user / test_password_123 |
| PostgreSQL Prod | - | Nur intern (kein Port) |

---

## 🎯 Next Steps nach Deployment

### 1. Login testen

```bash
# Öffne Browser
open http://localhost:8080

# Dev-Login (falls SampleUsers aktiv):
# Username: admin
# Password: 123
```

### 2. MFA testen (wenn implementiert)

```bash
# MFA-Setup aufrufen
# QR-Code in Google Authenticator scannen
# Code eingeben
```

### 3. Monitoring testen

```bash
# Prometheus Metrics
curl http://localhost:8080/actuator/prometheus | head -20

# Security-Events
docker logs pvs-app | grep SECURITY_EVENT
```

---

## 🔄 Neustart nach Änderungen

### Code-Änderungen

```bash
# 1. Rebuild
./gradlew build -x test

# 2. Docker-Image neu bauen
docker compose build pvs-app

# 3. Container neu starten
docker compose up -d pvs-app
```

### Config-Änderungen (.env, application.yaml)

```bash
# Nur Container neu starten (kein Rebuild nötig)
docker compose restart pvs-app
```

---

## 📚 Weitere Docs

- [Production Security Setup](./docs/security/PRODUCTION_SECURITY_FINAL_SUMMARY.md)
- [Reverse Proxy Setup](./docs/deployment/REVERSE_PROXY_SETUP.md)
- [Database Security](./docs/security/DATABASE_SECURITY.md)

---

**Viel Erfolg beim Deployment! 🚀**

Bei Problemen: Logs prüfen mit `docker compose logs -f pvs-app`
