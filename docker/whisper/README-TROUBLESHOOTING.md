# Whisper Docker - Troubleshooting und Netzwerk-Konfiguration

## Problem: Container startet, aber Server antwortet nicht

### Ursache
Wenn die Anwendung via `bootRun` (Gradle) gestartet wird, läuft sie auf dem Host-System, während der Whisper-Container in Docker läuft. Dies kann zu Netzwerkproblemen führen.

### Lösung

1. **Docker Engine prüfen**: Die Anwendung prüft jetzt automatisch, ob die Docker Engine läuft.

2. **Host-Zugriff**: Der Container ist über `localhost:9000` erreichbar, da der Port gemappt ist.

3. **Verzögerte Bereitschaft**: Beim ersten Start kann der Container länger brauchen, um das Whisper-Modell herunterzuladen. Die Timeout-Zeit wurde auf 2 Minuten erhöht.

### Fehlerbehebung

1. **Container-Logs prüfen**:
   ```bash
   docker logs pvs-whisper
   ```

2. **Container-Status prüfen**:
   ```bash
   docker ps -a | grep pvs-whisper
   docker inspect pvs-whisper
   ```

3. **Health-Check prüfen**:
   ```bash
   docker inspect --format='{{json .State.Health}}' pvs-whisper
   ```

4. **Manueller Health-Check**:
   ```bash
   curl http://localhost:9000/health
   ```

### Docker Desktop vs. Docker Engine

- **Docker Desktop**: Vollständige Docker-Installation mit GUI (empfohlen für Windows/Mac)
- **Docker Engine**: Nur der Docker-Daemon (für Linux oder Servers)

Die Anwendung prüft automatisch, ob die Docker Engine läuft und gibt eine klare Fehlermeldung aus, wenn Docker nicht verfügbar ist.

