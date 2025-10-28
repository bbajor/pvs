# Docker Setup für PVS (Praxisverwaltungssystem)

Dieses Verzeichnis enthält die Docker-Konfigurationen für die verschiedenen Komponenten der Anwendung.

## Dateien

- `Dockerfile` - Haupt-Container für die Spring Boot Anwendung (im Root-Verzeichnis)
- `docker/whisper/Dockerfile` - Container für Faster-Whisper AI-Service
- `docker-compose.yml` - Vollständige Stack-Konfiguration (App + Whisper)
- `docker-compose.dev.yml` - Nur Whisper-Service für lokale Entwicklung

## Verwendung

### Option 1: Vollständiger Stack (Anwendung + Whisper)

Startet sowohl die Hauptanwendung als auch den Whisper-Service:

```bash
docker compose up --build
```

Die Anwendung ist dann unter `http://localhost:8080` erreichbar.

**Vorteile:**
- Einzelner Befehl startet alles
- Container kommunizieren über internes Docker-Netzwerk
- Einfaches Deployment

**Nachteile:**
- Spring Dashboard in der IDE nicht verfügbar
- Hot-Reload während Entwicklung umständlich
- Logs über Docker müssen abgerufen werden

### Option 2: Nur Whisper als Container (Empfohlen für Entwicklung)

Whisper läuft als Container, die App lokal:

```bash
# Whisper starten
docker compose -f docker-compose.dev.yml up --build -d

# App lokal starten (z.B. via Gradle bootRun)
./gradlew bootRun
```

**Vorteile:**
- Vollständige IDE-Integration mit Spring Dashboard
- Hot-Reload funktioniert normal
- Separate Debugging-Möglichkeiten für App und Whisper
- Einfacher Wechsel zwischen Container und lokalem Whisper

**Nachteile:**
- Zwei separate Befehle zum Starten
- App muss Whisper über `localhost:9000` erreichen (Port-Mapping)

### Option 3: Alles lokal (ohne Docker)

Wenn Docker nicht gewünscht ist, kann Whisper auch direkt mit Python gestartet werden (siehe `src/main/resources/ai/whisper_server.py`).

## Konfiguration

Die Netzwerk-Konfiguration unterscheidet sich je nach Setup:

### Vollständiger Stack (`docker-compose.yml`)
```yaml
# App verwendet Service-Name für interne Kommunikation
AI_WHISPER_LOCAL_HOST=whisper
```

### Entwicklungs-Setup (App lokal)
```yaml
# application.yaml
ai:
  whisper:
    local:
      host: localhost  # Erreicht Container über Port-Mapping
      port: 9000
```

## Troubleshooting

### Container startet, aber Server antwortet nicht

1. **Logs prüfen:**
   ```bash
   docker logs pvs-whisper
   ```

2. **Health-Check prüfen:**
   ```bash
   docker inspect pvs-whisper | grep -A 10 Health
   ```

3. **Manueller Test:**
   ```bash
   curl http://localhost:9000/health
   ```

### Netzwerk-Probleme

Wenn die App den Whisper-Container nicht erreicht:

1. **Port-Mapping prüfen:**
   ```bash
   docker ps | grep whisper
   # Sollte zeigen: 0.0.0.0:9000->9000/tcp
   ```

2. **Container-Netzwerk prüfen:**
   ```bash
   docker network ls
   docker network inspect pvs-network
   ```

Weitere Details in `docker/whisper/README-TROUBLESHOOTING.md`.

## Entwicklungs-Workflow

**Empfohlen für tägliche Entwicklung:**
1. Starte Whisper-Container: `docker compose -f docker-compose.dev.yml up -d`
2. Starte App via IDE/Gradle: `./gradlew bootRun`
3. Entwickle normal weiter
4. Whisper stoppen (optional): `docker compose -f docker-compose.dev.yml down`

**Für Produktion/Testing:**
1. Vollständigen Stack starten: `docker compose up --build`
2. Alles läuft in Containern, reproduzierbar

