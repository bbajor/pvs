# Whisper Docker Container

Dieser Docker-Container stellt einen Faster-Whisper-Service für die PVS-Anwendung bereit.

## Verwendung

### Automatischer Start (empfohlen)

Die Anwendung kann den Container automatisch starten, wenn `ai.whisper.local.auto-install=true` und `ai.whisper.local.use-docker=true` in der `application.yaml` gesetzt sind.

### Manueller Start

```bash
cd docker/whisper
docker-compose up -d --build
```

### Container stoppen

```bash
cd docker/whisper
docker-compose down
```

### Logs anzeigen

```bash
docker logs -f pvs-whisper
```

### Health Check

Der Container bietet einen Health-Check-Endpoint:
```bash
curl http://localhost:9000/health
```

## Konfiguration

Die Konfiguration erfolgt über Umgebungsvariablen:
- `PORT`: Port auf dem der Service läuft (Standard: 9000)

## Troubleshooting

### Container startet nicht
- Prüfen Sie die Docker-Logs: `docker logs pvs-whisper`
- Stellen Sie sicher, dass Port 9000 nicht bereits belegt ist
- Prüfen Sie Docker-Installation: `docker --version`

### Service antwortet nicht
- Prüfen Sie, ob der Container läuft: `docker ps | grep pvs-whisper`
- Prüfen Sie die Health-Check URL: `curl http://localhost:9000/health`
- Prüfen Sie die Logs auf Fehler

