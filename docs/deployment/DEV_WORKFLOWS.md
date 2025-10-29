# Development Workflows

Das Projekt unterstützt verschiedene Development-Modi je nach Anforderung.

## 🚀 Entwicklungs-Optionen

### 1. Schnelles Testing mit H2 (Gradle bootRun)

**Wann nutzen:**
- Schnelle Feature-Entwicklung
- Einfache Logik-Tests
- Keine Docker-Installation benötigt

**Setup:**
```bash
# Einfach Gradle starten
./gradlew bootRun

# App läuft auf http://localhost:8080
```

**Eigenschaften:**
- H2 In-Memory Datenbank (keine Persistenz)
- Schneller Start
- Keine Docker-Container nötig
- Daten gehen beim Neustart verloren

### 2. Realistisches Testing mit PostgreSQL (Lokal)

**Wann nutzen:**
- Datenbank-Features testen
- Flyway-Migrationen testen
- Realistische Datenstrukturen benötigt

**Setup:**
```bash
# Environment-Datei erstellen
cp docker-compose.dev.env.example docker-compose.dev.env
nano docker-compose.dev.env  # Passwort setzen

# Container starten
docker-compose -f docker-compose.dev.yml --env-file docker-compose.dev.env up -d

# App läuft auf http://localhost:8080
```

**Eigenschaften:**
- PostgreSQL Container (persistente Daten)
- Daten bleiben nach Neustart erhalten
- Realistische DB-Struktur
- Separate Netzwerk-Isolation

**Verwendung des automatisch gebauten Images:**
```bash
# Nach Push zu dev Branch läuft GitHub Actions
# Image ist verfügbar als: ghcr.io/bbajor/pvs:dev-latest

# Lokal nutzen (anstatt build):
docker-compose -f docker-compose.dev.yml pull
```

### 3. Test-Umgebung auf Hetzner Server

**Wann nutzen:**
- Vor Production-Freigabe
- Integration-Tests mit echter Infrastruktur
- Oberflächen-Tests

**Workflow:**
```bash
# 1. Entwickle auf dev Branch
git checkout dev
# ... Änderungen ...

# 2. Push zu dev → GitHub Actions baut Image
git push origin dev

# 3. Merge zu test Branch
git checkout test
git merge dev
git push origin test

# 4. GitHub Actions läuft (CI & Build)
#    → Image: ghcr.io/bbajor/pvs:test-latest

# 5. Manuelles Deployment (nur wenn CI erfolgreich!)
#    → GitHub Actions → "Deploy Test to Hetzner (Manual)"
#    → Run workflow → Bestätigen

# 6. Via VPN verbinden und testen
#    → http://<hetzner-ip>:8080 (nur über VPN erreichbar)
```

**Eigenschaften:**
- PostgreSQL Container auf Hetzner Server
- Nur über VPN erreichbar
- Persistente Daten
- Realistische Server-Umgebung
- Manuelles Deployment (vollständige Kontrolle)

### 4. Production-Umgebung auf Hetzner Server

**Wann nutzen:**
- Finale Freigabe nach erfolgreichen Tests
- Echte Produktions-Daten

**Workflow:**
```bash
# 1. Test-Umgebung vollständig getestet
# 2. Merge test → master
git checkout master
git merge test
git push origin master

# 3. Manuelles Production-Deployment
#    → GitHub Actions → "Deploy Production to Hetzner"
#    → Run workflow → Bestätigen
```

**Eigenschaften:**
- PostgreSQL Production-Datenbank
- Öffentlich erreichbar über Traefik/HTTPS
- Persistente Daten
- Backup-Strategie aktiv

## 📋 Workflow-Übersicht

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Lokal: H2 (schnell)                                      │
│    ./gradlew bootRun                                        │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Lokal: PostgreSQL Container                             │
│    docker-compose.dev.yml                                  │
│    (Image wird bei dev-Push automatisch gebaut)            │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. dev Branch → Push                                        │
│    → GitHub Actions: Build & Push Image                     │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. test Branch → Merge dev                                  │
│    → GitHub Actions: CI & Build                            │
│    → Manuelles Deployment (GitHub Actions)                 │
│    → Test auf Hetzner (nur VPN)                            │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. master Branch → Merge test                               │
│    → Manuelles Production-Deployment                        │
│    → Production auf Hetzner (öffentlich)                   │
└─────────────────────────────────────────────────────────────┘
```

## 🛠️ Praktische Beispiele

### Schnell Feature testen

```bash
# Einfach starten
./gradlew bootRun

# Browser öffnet automatisch
# http://localhost:8080
```

### Datenbank-Feature entwickeln

```bash
# PostgreSQL Container starten
docker-compose -f docker-compose.dev.yml up -d postgres-dev

# App lokal starten (nutzt Container-DB)
export DATABASE_URL=jdbc:postgresql://localhost:5432/pvs_dev
export DATABASE_USERNAME=pvs_user
export DATABASE_PASSWORD=<aus docker-compose.dev.env>
./gradlew bootRun
```

### Für CI vorbereiten

```bash
# Alles lokal testen
./gradlew build test

# Dann pushen
git push origin dev
# → GitHub Actions baut Image automatisch
```

### Test-Umgebung deployen

1. **In GitHub:** Actions → "Test Branch CI & Build"
2. **Prüfe:** Alle Tests grün?
3. **Wenn ja:** Actions → "Deploy Test to Hetzner (Manual)"
4. **Bestätige:** "Ja, deployen zu Test-Umgebung"
5. **Warte:** Deployment läuft
6. **Teste:** Via VPN → http://<hetzner-ip>:8080

## ⚙️ Environment-Variablen

### H2 (Standard für gradle bootRun)

Keine Variablen nötig - H2 ist Standard in `application-dev.yaml`

### PostgreSQL (Lokal)

**Via docker-compose.dev.env:**
```bash
POSTGRES_DB_DEV=pvs_dev
POSTGRES_USER_DEV=pvs_user
POSTGRES_PASSWORD_DEV=dein_passwort
```

**Oder direkt:**
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/pvs_dev
export DATABASE_USERNAME=pvs_user
export DATABASE_PASSWORD=dein_passwort
./gradlew bootRun
```

## 🔍 Troubleshooting

### Port 8080 bereits belegt

```bash
# Anderen Prozess finden
lsof -i :8080  # Mac/Linux
netstat -ano | findstr :8080  # Windows

# Oder anderen Port nutzen
SPRING_SERVER_PORT=8081 ./gradlew bootRun
```

### PostgreSQL Container startet nicht

```bash
# Logs prüfen
docker-compose -f docker-compose.dev.yml logs postgres-dev

# Container neu starten
docker-compose -f docker-compose.dev.yml restart postgres-dev
```

### Image für dev nicht verfügbar

```bash
# Nach dev-Push sollte GitHub Actions das Image bauen
# Prüfe in GitHub: Actions → "Dev Branch CI & Build"

# Manuell pullen (falls öffentlich):
docker pull ghcr.io/bbajor/pvs:dev-latest

# Oder lokal bauen:
docker-compose -f docker-compose.dev.yml build
```

