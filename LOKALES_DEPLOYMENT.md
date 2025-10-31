# Lokales Deployment - Production Security Features testen

**So testest du die Production-Security-Features lokal**

---

## 🎯 Ziel

Teste alle Security-Features lokal mit Docker-Compose (inkl. Whisper AI).

**Wichtig:** Der Container läuft mit:
- ✅ **Spring-Profil:** `dev` (H2-Database, DevSecurityConfig)
- ✅ **Vaadin:** Production Mode (precompiled frontend aus Dockerfile)
- ✅ **Grund:** Ermöglicht schnelles lokales Testen OHNE ständiges Frontend-Neu-Kompilieren

---

## 🚀 Deployment-Schritte

### 1. Repository & Branch

```bash
# Branch auschecken
git checkout feature/security-infrastructure

# Aktuellsten Stand pullen
git pull
```

### 2. Docker-Compose starten

```bash
# ALLE Services starten (App + Whisper)
docker compose up --build

# Oder im Hintergrund
docker compose up -d --build
```

**Hinweis:** Beim ersten Start:
- ⏳ **Whisper-Download:** ~1.5 GB (dauert 2-3 Minuten)
- ⏳ **App-Build:** ~3-5 Minuten (Multi-Stage Build)
- ⏳ **Gesamt:** ~5-8 Minuten

### 3. Logs verfolgen

```bash
# Alle Logs
docker compose logs -f

# Nur App
docker compose logs -f pvs-app

# Nur Whisper
docker compose logs -f whisper
```

**Warte bis du siehst:**
```
pvs-app    | Started Application in X.XXX seconds
whisper    | INFO:     Uvicorn running on http://0.0.0.0:9000
```

### 4. Health-Check

```bash
# App Health
curl http://localhost:8080/actuator/health

# Expected:
# {"status":"UP"}

# Whisper Health
curl http://localhost:9000/health

# Expected:
# {"status":"healthy"}
```

### 5. Browser öffnen

```bash
open http://localhost:8080

# Oder manuell
# Browser: http://localhost:8080
```

---

## 🧪 Security-Features testen

### 1. Security Headers prüfen

```bash
curl -I http://localhost:8080

# Expected Headers:
# Content-Security-Policy: default-src 'self'; ...
# X-Frame-Options: DENY
# X-Content-Type-Options: nosniff
# X-XSS-Protection: 1; mode=block
# Referrer-Policy: strict-origin-when-cross-origin
# Permissions-Policy: camera=(), ...
```

### 2. Rate Limiting testen

```bash
# 150 Requests in Folge (sollte ab ~100 fehlschlagen)
for i in {1..150}; do
  curl -s -o /dev/null -w "%{http_code} " http://localhost:8080/
done

# Expected: 
# 200 200 200 ... (100x) ... 429 429 429 (50x)
# HTTP 429 = Too Many Requests (Rate Limit greift)
```

### 3. Prometheus Metrics

```bash
# Metrics abrufen
curl http://localhost:8080/actuator/prometheus | head -30

# Expected:
# # HELP jvm_memory_used_bytes
# # TYPE jvm_memory_used_bytes gauge
# ...
# hikaricp_connections_active
# http_server_requests_seconds_count
```

### 4. Structured Logging (JSON)

```bash
# App-Logs anschauen
docker compose logs pvs-app | tail -20

# Dev-Profil: Readable format
# Expected:
# 2025-10-30 12:00:00.000 [main] INFO  d.b.p.Application - Started Application
```

### 5. Database Connection (H2 in Dev)

```bash
# App-Logs nach DB-Connection suchen
docker compose logs pvs-app | grep -i "h2\|database\|hikari"

# Expected:
# HikariPool-1 - Starting...
# HikariPool-1 - Start completed
```

---

## 🔧 Bei Problemen

### Problem: Whisper startet nicht / bleibt unhealthy

**Symptom:**
```
whisper    | ERROR: Model download failed
```

**Lösung:**
```bash
# Option 1: Warte länger (Model-Download dauert)
docker compose logs -f whisper

# Option 2: Whisper-Dependency temporär entfernen
# Editiere docker-compose.yml:
# pvs-app:
#   # depends_on:  # AUSKOMMENTIEREN
#   #   whisper:
#   #     condition: service_healthy

# Dann neu starten
docker compose down
docker compose up --build
```

### Problem: ApplicationContextException

**Symptom:**
```
Failed to start bean 'webServerStartStop'
java.lang.IllegalStateException: Failed to determine project directory for dev mode
```

**Ursache:** Vaadin im Development Mode (braucht Gradle-Projektstruktur im Container)

**Lösung:** ✅ **BEREITS BEHOBEN** durch `VAADIN_PRODUCTION_MODE=true` in `docker-compose.yml`

Wenn das Problem trotzdem auftritt:
```bash
# Prüfe Environment-Variable im Container
docker exec pvs-app env | grep VAADIN

# Expected:
# VAADIN_PRODUCTION_MODE=true

# Wenn fehlt, in docker-compose.yml ergänzen:
# environment:
#   - VAADIN_PRODUCTION_MODE=true
```

### Problem: Port 8080 bereits belegt

**Lösung:**
```bash
# Prüfe was auf Port 8080 läuft
lsof -i :8080

# Oder ändere Port in docker-compose.yml:
# ports:
#   - "8081:8080"
```

### Problem: Out of Memory

**Lösung:**
```bash
# Docker-Memory erhöhen
# Docker Desktop → Settings → Resources → Memory → 8 GB

# Oder in docker-compose.yml:
# deploy:
#   resources:
#     limits:
#       memory: 4G
```

---

## 🔍 Debugging

### Vollständige Logs speichern

```bash
# Alle Logs in Datei
docker compose logs > full-logs.txt

# Dann durchsuchen
grep -i "error\|exception\|failed" full-logs.txt
```

### In Container einloggen

```bash
# Shell im laufenden Container
docker exec -it pvs-app sh

# Java-Prozess prüfen
ps aux | grep java

# Environment-Variablen prüfen
env | grep SPRING
```

### Fresh Start

```bash
# Alles löschen und neu bauen
docker compose down -v
docker compose build --no-cache
docker compose up
```

---

## ✅ Erfolgs-Kriterien

Nach erfolgreichem Start:

- ✅ `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
- ✅ Security Headers gesetzt (curl -I)
- ✅ Browser zeigt Login-Page
- ✅ Keine Errors in Logs
- ✅ Whisper läuft (optional)

---

## 📚 Debugging-Ressourcen

### Log-Locations im Container

```bash
# Spring Boot Logs
docker exec pvs-app cat /tmp/spring.log

# Security-Events
docker exec pvs-app cat /tmp/security-events.log
```

### Bean-Conflicts debuggen

```bash
# Starte mit Debug-Flag
docker compose run pvs-app java -jar app.jar --debug

# Oder in docker-compose.yml:
# environment:
#   - SPRING_PROFILES_ACTIVE=dev
#   - LOGGING_LEVEL_ROOT=DEBUG
```

---

## 💡 Tipps

1. **Geduld mit Whisper:** Beim ersten Start dauert Model-Download 2-3 Minuten
2. **Logs verfolgen:** `docker compose logs -f` zeigt Live-Logs
3. **Fresh Start:** Bei Problemen `docker compose down -v` und neu
4. **Dev-Profil:** Im docker-compose.yml ist `SPRING_PROFILES_ACTIVE=dev` (H2-Database)

---

## 🎯 Next Steps nach erfolgreichem Start

1. **Login testen** (Dev-User: siehe SampleUsers.java)
2. **Security Headers validieren**
3. **Rate Limiting testen**
4. **Metrics prüfen**
5. **Logs analysieren**

---

**Bei Problemen:** Sende mir die Logs von `docker compose logs pvs-app`! 🚀
