# Deployment Fix - Schnelle Lösung

**Das Problem:** Die App wartet auf den Whisper-Service, der lange braucht zum Starten.

**Die Lösung:** Nutze `docker-compose.local.yml` ohne Whisper-Dependency!

---

## ✅ Lösung 1: Vereinfachtes Docker-Compose

```bash
# Stoppe alles
docker compose down

# Starte mit vereinfachtem Compose (ohne Whisper)
docker compose -f docker-compose.local.yml up --build

# Im Hintergrund
docker compose -f docker-compose.local.yml up -d --build
```

**Zugriff:** http://localhost:8080

---

## ✅ Lösung 2: Original docker-compose.yml mit fix

Das originale `docker-compose.yml` wartet auf Whisper. Du kannst entweder:

### A) Whisper-Dependency entfernen (temporär)

```bash
# docker-compose.yml editieren
# Kommentiere "depends_on:" aus:

# depends_on:
#   whisper:
#     condition: service_healthy
```

### B) Warte bis Whisper healthy ist (~3 Minuten)

```bash
# Starte alles
docker compose up

# Warte 2-3 Minuten
# Whisper muss Model herunterladen (~1.5 GB)

# Logs anschauen
docker compose logs -f whisper
```

---

## ✅ Lösung 3: Direkter Start (ohne Docker)

```bash
# Gradle starten (nutzt H2 in-memory DB)
./gradlew bootRun

# Zugriff
open http://localhost:8080
```

---

## 🧪 Nach erfolgreichem Start

```bash
# Health-Check
curl http://localhost:8080/actuator/health

# Expected:
# {"status":"UP"}

# Security Headers prüfen
curl -I http://localhost:8080 | grep -E "Content-Security|X-Frame"

# Expected:
# Content-Security-Policy: ...
# X-Frame-Options: DENY
```

---

## 🚨 Bei weiteren Problemen

### Vollständige Logs anschauen

```bash
# Alle Container-Logs
docker compose -f docker-compose.local.yml logs

# Nur pvs-app
docker compose -f docker-compose.local.yml logs pvs-app

# Follow Mode
docker compose -f docker-compose.local.yml logs -f pvs-app
```

### Fresh Start

```bash
# Alles löschen und neu starten
docker compose -f docker-compose.local.yml down -v
docker compose -f docker-compose.local.yml build --no-cache
docker compose -f docker-compose.local.yml up
```

---

## 📝 Empfehlung

**Für lokales Testing/Development:**
→ Nutze `docker-compose.local.yml` (OHNE Whisper)

**Für vollständige Features (mit AI):**
→ Nutze `docker-compose.yml` (MIT Whisper, aber warte 3 Min)

**Für Production-Testing:**
→ Nutze `docker-compose.production.yml --profile prod`

---

**Versuche jetzt:**

```bash
docker compose -f docker-compose.local.yml up --build
```

Und schau dir die Logs an! 🚀
