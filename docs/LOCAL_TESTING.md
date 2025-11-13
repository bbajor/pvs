# Lokale Tests - Cloud-Features

Diese Anleitung beschreibt, wie du die Cloud-Features lokal testen kannst, bevor sie in die Cloud deployed werden.

## Voraussetzungen

- Java 21
- Podman oder Docker
- PostgreSQL (optional, kann via Container laufen)
- Redis (optional, kann via Container laufen)

## 1. Cloud-Profil lokal testen

### Mit Spring Boot direkt

```bash
# Cloud-Profil aktivieren
./gradlew bootRun --args='--spring.profiles.active=cloud'

# Oder mit Production Mode
./gradlew -Pvaadin.productionMode=true vaadinBuildFrontend bootRun --args='--spring.profiles.active=cloud'
```

### Mit Podman-Compose

```bash
# Starte alle Services (PostgreSQL, Redis, Whisper)
podman-compose -f podman-compose.production.yml --profile test up -d

# Prüfe Health Checks
curl http://localhost:8081/actuator/health
```

## 2. Environment-Variablen setzen

Erstelle eine `.env` Datei oder setze die Variablen:

```bash
# Database
export DATABASE_URL=jdbc:postgresql://localhost:5434/pvs_test
export DATABASE_USERNAME=pvs_user
export DATABASE_PASSWORD=dein_passwort

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6380
export REDIS_PASSWORD=

# SMTP (optional)
export SMTP_ENCRYPTION_KEY=dein-32-zeichen-schluessel-hier
export SPRING_MAIL_HOST=smtp.example.com
export SPRING_MAIL_PORT=587
export SPRING_MAIL_USERNAME=deine@email.de
export SPRING_MAIL_PASSWORD=dein_passwort

# AI Services (optional)
export AI_API_KEY=dein-api-key
export WHISPER_REMOTE_ENABLED=true
```

## 3. eGK-Agent API testen

### API-Endpoint prüfen

```bash
# Health Check
curl http://localhost:8081/api/egk/health

# Sollte zurückgeben: "eGK-Agent API is available"
```

### eGK-Daten senden (Beispiel)

```bash
curl -X POST http://localhost:8081/api/egk/read \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer DEIN_TOKEN" \
  -d '{
    "versichertenId": "X123456789",
    "vorname": "Max",
    "nachname": "Mustermann",
    "geburtsdatum": "1980-01-01",
    "geschlecht": "M",
    "adresse": {
      "strasse": "Musterstraße",
      "hausnummer": "123",
      "postleitzahl": "12345",
      "ort": "Berlin",
      "land": "D"
    },
    "versicherungsschutz": {
      "beginn": "2020-01-01",
      "versichertenart": "GKV",
      "kostentraeger": {
        "kostentraegerkennung": "123456789",
        "name": "AOK",
        "laendercode": "D"
      }
    }
  }'
```

### Mit Postman/Insomnia

1. Erstelle neue Request: `POST http://localhost:8081/api/egk/read`
2. Header: `Content-Type: application/json`
3. Body (JSON): Siehe Beispiel oben
4. Authentifizierung: Basic Auth oder Bearer Token (je nach Konfiguration)

## 4. Resilienz-Features testen

### Retry-Logik testen

```bash
# Simuliere temporären Fehler (z.B. Whisper API)
# Die Anwendung sollte automatisch retry durchführen

# Logs prüfen
podman logs pvs-test | grep -i retry
```

### Circuit-Breaker testen

```bash
# Mehrere fehlgeschlagene Requests senden
# Nach 5 Fehlern sollte Circuit Breaker öffnen

# Status prüfen
curl http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state
```

## 5. Session-Failover mit Redis testen

### Redis starten

```bash
# Redis Container starten
podman-compose -f podman-compose.production.yml --profile test up -d redis

# Redis-Verbindung prüfen
podman exec pvs-redis-test redis-cli ping
# Sollte zurückgeben: PONG
```

### Session-Sharing testen

1. **Login durchführen:**
   ```bash
   # Login-Request senden
   curl -X POST http://localhost:8081/login \
     -d "username=test&password=test" \
     -c cookies.txt
   ```

2. **Session in Redis prüfen:**
   ```bash
   # Sessions anzeigen
   podman exec pvs-redis-test redis-cli keys "spring:session:*"
   
   # Session-Details
   podman exec pvs-redis-test redis-cli get "spring:session:SESSION_ID"
   ```

3. **Application neu starten:**
   ```bash
   podman-compose -f podman-compose.production.yml --profile test restart pvs-test
   ```

4. **Session sollte erhalten bleiben:**
   - Cookie sollte weiterhin gültig sein
   - User sollte eingeloggt bleiben

## 6. Load-Testing lokal

### Apache Bench (ab)

```bash
# Installieren (Ubuntu/Debian)
sudo apt-get install apache2-utils

# Load Test durchführen
./scripts/testing/load-test.sh

# Oder manuell
ab -n 1000 -c 10 http://localhost:8081/
```

### Mit k6 (empfohlen)

```bash
# k6 installieren
# https://k6.io/docs/getting-started/installation/

# Load Test Script erstellen
cat > load-test.js <<EOF
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  vus: 10,
  duration: '30s',
};

export default function() {
  let res = http.get('http://localhost:8081/');
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
EOF

# Test ausführen
k6 run load-test.js
```

## 7. Monitoring & Health Checks

### Health Endpoints

```bash
# Application Health
curl http://localhost:8081/actuator/health

# Prometheus Metrics
curl http://localhost:8081/actuator/prometheus

# Circuit Breaker Status
curl http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state

# Retry Statistics
curl http://localhost:8081/actuator/metrics/resilience4j.retry.calls
```

### Logs prüfen

```bash
# Application Logs
podman logs -f pvs-test

# Redis Logs
podman logs -f pvs-redis-test

# PostgreSQL Logs
podman logs -f pvs-postgres-test
```

## 8. PII-Masking testen

### Logs prüfen

```bash
# Logs sollten keine PII enthalten
podman logs pvs-test | grep -i "patient\|email\|name"

# Sollte nur maskierte Daten zeigen:
# "Extracted patient - Name: M***n, Birth: 1980-***-***"
```

## 9. Email-Service testen

### Rate-Limiting testen

```bash
# Mehrere Emails schnell hintereinander senden
# Nach 10 Emails pro Stunde sollte Rate-Limit greifen

# Logs prüfen
podman logs pvs-test | grep -i "rate limit"
```

### Retry-Mechanismus testen

```bash
# Simuliere SMTP-Fehler
# Email-Service sollte automatisch retry durchführen

# Logs prüfen
podman logs pvs-test | grep -i "retry\|email"
```

## 10. Troubleshooting

### Port-Konflikte

```bash
# Prüfe welche Ports belegt sind
netstat -tulpn | grep -E "8081|6380|9001"

# Oder mit ss
ss -tulpn | grep -E "8081|6380|9001"
```

### Container-Probleme

```bash
# Container-Status prüfen
podman ps -a

# Container-Logs
podman logs pvs-test

# Container neu starten
podman-compose -f podman-compose.production.yml --profile test restart
```

### Datenbank-Verbindung

```bash
# PostgreSQL-Verbindung testen
podman exec pvs-postgres-test pg_isready -U pvs_user

# Datenbank-Verbindung von Application
podman exec pvs-test curl http://localhost:8081/actuator/health
```

### Redis-Verbindung

```bash
# Redis-Verbindung testen
podman exec pvs-redis-test redis-cli ping

# Sessions prüfen
podman exec pvs-redis-test redis-cli keys "spring:session:*"
```

## 11. Vollständiger Test-Workflow

```bash
# 1. Alle Services starten
podman-compose -f podman-compose.production.yml --profile test up -d

# 2. Warten bis alle Services bereit sind
sleep 30

# 3. Health Checks
curl http://localhost:8081/actuator/health
podman exec pvs-postgres-test pg_isready
podman exec pvs-redis-test redis-cli ping

# 4. Load Test
./scripts/testing/load-test.sh

# 5. Session-Failover Test
./scripts/testing/session-failover-test.sh

# 6. Logs prüfen
podman logs pvs-test | tail -100

# 7. Cleanup
podman-compose -f podman-compose.production.yml --profile test down
```

## 12. Nächste Schritte

Nach erfolgreichen lokalen Tests:

1. **Merge in dev:**
   ```bash
   git checkout dev
   git merge cloud-ready-and-multi-tenancy
   git push origin dev
   ```

2. **Merge in test:**
   ```bash
   git checkout test
   git merge dev
   git push origin test
   ```

3. **Deployment auf Test-Instanz:**
   - Siehe `docs/SSH_TUNNEL_SETUP.md` für SSH-Tunnel
   - Siehe `docs/CLOUD_DEPLOYMENT.md` für Deployment-Workflow

