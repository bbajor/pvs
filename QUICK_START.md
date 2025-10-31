# Quick Start - Lokales Deployment

**Schnellster Weg zum Testen der Applikation**

---

## 🚀 Option 1: Ohne Whisper (Empfohlen für Testing)

```bash
# Starte nur die App (ohne AI-Features)
docker compose -f docker-compose.local.yml up --build

# Oder im Hintergrund
docker compose -f docker-compose.local.yml up -d --build

# Logs anschauen
docker compose -f docker-compose.local.yml logs -f pvs-app
```

**Zugriff:** http://localhost:8080

---

## 🚀 Option 2: Mit Whisper AI (Vollständig)

```bash
# Starte App + Whisper AI-Service
docker compose up --build

# Logs
docker compose logs -f
```

**Hinweis:** Whisper benötigt ~2-3 Minuten zum Starten (Model-Download)

---

## 🚀 Option 3: Nur mit Gradle (ohne Docker)

```bash
# Starte mit embedded H2-Database
./gradlew bootRun

# Oder mit gebautem JAR
./gradlew bootJar
java -jar build/libs/pvs-app-1.0-SNAPSHOT.jar
```

---

## 🧪 Testen nach Start

```bash
# Health-Check
curl http://localhost:8080/actuator/health

# Security Headers
curl -I http://localhost:8080

# Browser öffnen
open http://localhost:8080
```

---

## 🛑 Stoppen

```bash
# docker-compose.local.yml
docker compose -f docker-compose.local.yml down

# docker-compose.yml (mit Whisper)
docker compose down

# Gradle
# CTRL+C
```

---

## ⚡ Bei Problemen

### Problem: Container startet nicht

```bash
# Logs anschauen
docker compose -f docker-compose.local.yml logs pvs-app

# Container neu bauen
docker compose -f docker-compose.local.yml build --no-cache

# Fresh Start
docker compose -f docker-compose.local.yml down -v
docker compose -f docker-compose.local.yml up --build
```

### Problem: Port 8080 belegt

```bash
# Anderen Port verwenden
docker compose -f docker-compose.local.yml up

# Dann in docker-compose.local.yml ändern:
# ports:
#   - "8081:8080"
```

### Problem: Whisper startet nicht

```bash
# Nutze docker-compose.local.yml (ohne Whisper)
docker compose -f docker-compose.local.yml up
```

---

## 📚 Weitere Infos

- Vollständige Anleitung: [LOCAL_DEPLOYMENT_GUIDE.md](./LOCAL_DEPLOYMENT_GUIDE.md)
- Security Features: [docs/security/](./docs/security/)

---

**Viel Erfolg! 🚀**
