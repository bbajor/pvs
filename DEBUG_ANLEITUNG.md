# Debug-Anleitung für Docker-Compose Startup-Fehler

## 📋 Logs sammeln

### Option 1: Alle Logs in Datei speichern

```bash
# Im /workspace Verzeichnis
docker compose logs > logs.txt

# Oder nur App-Logs
docker compose logs pvs-app > app-logs.txt

# Oder nur Whisper-Logs
docker compose logs whisper > whisper-logs.txt
```

### Option 2: Logs direkt anzeigen

```bash
# Letzte 100 Zeilen der App
docker compose logs --tail=100 pvs-app

# Live-Logs verfolgen
docker compose logs -f pvs-app
```

### Option 3: Fehler filtern

```bash
# Nur Fehler anzeigen
docker compose logs pvs-app 2>&1 | grep -i "error\|exception\|failed\|caused by"

# In Datei speichern
docker compose logs pvs-app 2>&1 | grep -i "error\|exception\|failed\|caused by" > errors.txt
```

---

## 🔍 Häufige Fehler-Pattern

### 1. ApplicationContextException: Failed to start bean 'webServerStartStop'

**Mögliche Ursachen:**
- Port 8080 bereits belegt
- Bean-Konfigurationsfehler
- Fehlende Dependencies
- Whisper-Dependency-Timeout

**Debug-Schritte:**

```bash
# 1. Prüfe ob Port belegt ist
lsof -i :8080
# Oder auf Windows:
netstat -ano | findstr :8080

# 2. Schau nach dem ROOT CAUSE in Logs
docker compose logs pvs-app | grep -A 10 "Caused by"

# 3. Prüfe Bean-Errors
docker compose logs pvs-app | grep -i "bean.*error\|autowired\|required.*bean"

# 4. Prüfe Whisper-Status
docker compose ps whisper
docker compose logs whisper | tail -50
```

### 2. Whisper startet nicht / bleibt unhealthy

**Symptom:**
```
pvs-app    | depends_on condition service_healthy failed
whisper    | Unhealthy
```

**Lösung:**

```bash
# Prüfe Whisper-Logs
docker compose logs whisper

# Häufige Probleme:
# - Model-Download dauert zu lange (2-3 Min beim ersten Mal)
# - Nicht genug RAM (mind. 2GB für Whisper)
# - Python-Dependencies fehlen

# Temporärer Fix: depends_on auskommentieren
# In docker-compose.yml:
#   depends_on:
#     whisper:
#       condition: service_healthy
```

### 3. Bean creation exception

**Symptom:**
```
Error creating bean with name '...'
```

**Debug:**

```bash
# Zeige alle Bean-Errors
docker compose logs pvs-app | grep "Error creating bean"

# Zeige Dependency-Chain
docker compose logs pvs-app | grep -A 5 "required a bean"
```

### 4. Port already in use

**Symptom:**
```
Port 8080 is already in use
```

**Lösung:**

```bash
# Finde Prozess auf Port 8080
lsof -i :8080

# Beende Prozess
kill -9 <PID>

# Oder ändere Port in docker-compose.yml
ports:
  - "8081:8080"
```

---

## 🛠️ Erweiterte Debug-Optionen

### Container-Zustand prüfen

```bash
# Status aller Container
docker compose ps

# Detaillierte Infos
docker inspect pvs-app

# Prozesse im Container
docker exec pvs-app ps aux
```

### In Container einloggen

```bash
# Shell öffnen
docker exec -it pvs-app sh

# Environment prüfen
docker exec pvs-app env | grep SPRING

# Java-Prozess prüfen
docker exec pvs-app ps aux | grep java
```

### Spring Boot Debug-Modus

```bash
# Starte mit Debug-Logging
# In docker-compose.yml ergänzen:
environment:
  - SPRING_PROFILES_ACTIVE=dev
  - LOGGING_LEVEL_ROOT=DEBUG
  - LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=DEBUG
  - LOGGING_LEVEL_DE_BBAJOR_PVS=TRACE

# Dann neu starten
docker compose down
docker compose up
```

### Startup-Condition prüfen

```bash
# Spring Condition Evaluation Report
# Starte App mit:
environment:
  - DEBUG=true

# Dann Logs nach "CONDITIONS EVALUATION REPORT" durchsuchen
```

---

## 📤 Logs für Analyse bereitstellen

Wenn du Hilfe brauchst, sammle diese Infos:

```bash
# 1. Volle App-Logs
docker compose logs pvs-app > app-logs.txt

# 2. Whisper-Logs
docker compose logs whisper > whisper-logs.txt

# 3. Container-Status
docker compose ps > container-status.txt

# 4. Docker-Compose Config
docker compose config > docker-config.txt

# 5. Nur Fehler
docker compose logs 2>&1 | grep -i "error\|exception\|failed" > errors-only.txt
```

Dann kannst du mir die Dateien zeigen oder hier im Chat teilen.

---

## 🎯 Quick Fixes zum Ausprobieren

### Fix 1: Whisper-Dependency entfernen

```bash
# docker-compose.yml editieren:
services:
  pvs-app:
    # depends_on:      # ← AUSKOMMENTIEREN
    #   whisper:       # ← AUSKOMMENTIEREN
    #     condition: service_healthy  # ← AUSKOMMENTIEREN

# Neu starten
docker compose down
docker compose up --build pvs-app
```

### Fix 2: Fresh Start

```bash
# Alles löschen und neu bauen
docker compose down -v
docker system prune -f
docker compose build --no-cache
docker compose up
```

### Fix 3: Nur Gradle lokal starten

```bash
# Ohne Docker
./gradlew bootRun

# Mit spezifischem Profil
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Fix 4: Memory erhöhen

```bash
# In docker-compose.yml:
services:
  pvs-app:
    deploy:
      resources:
        limits:
          memory: 4G
        reservations:
          memory: 2G
```

---

## 💡 Nächste Schritte

1. **Logs sammeln** (siehe oben)
2. **Nach Fehler-Pattern suchen** (grep "Caused by", "Error", "Exception")
3. **Mir die relevanten Logs zeigen** (gerne hier in den Chat pasten)
4. **Ich analysiere dann den konkreten Fehler** und wir fixen ihn gemeinsam! 🚀

---

Sobald du die Logs hast, zeig sie mir und wir schauen uns den genauen Fehler an!
