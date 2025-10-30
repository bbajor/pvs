# On-Premise Deployment

Anleitung für Kunden, die PVS lokal (On-Premise) hosten möchten.

## Voraussetzungen

- Docker & Docker Compose installiert
- Mind. 4GB RAM verfügbar
- Mind. 20GB freier Speicherplatz
- Port 8080 verfügbar (oder ändere in docker-compose.yml)

## Quick Start

1. Klone Repository oder kopiere Deployment-Dateien:
   ```bash
   git clone <repository-url>
   cd pvs
   ```

2. Konfiguriere Environment:
   ```bash
   cp .env.example .env
   # Editiere .env mit lokalen Einstellungen
   ```

3. Starte Services:
   ```bash
   docker-compose up -d
   ```

4. Warte auf Startup (ca. 2-3 Minuten):
   ```bash
   docker-compose logs -f pvs-app
   ```

5. Öffne Browser: `http://localhost:8080`

## Docker Compose Setup

Die `docker-compose.yml` enthält:

- **pvs-app**: Hauptanwendung (Spring Boot)
- **pvs-whisper**: Whisper AI Service (optional)
- **pvs-db**: PostgreSQL Datenbank (optional, kann extern sein)

### Konfiguration

```yaml
# docker-compose.yml
services:
  pvs-app:
    environment:
      - SPRING_PROFILES_ACTIVE=prod  # oder 'dev' für Entwicklung
      - DATABASE_URL=postgresql://pvs_user:password@pvs-db:5432/pvs
      - AI_WHISPER_LOCAL_ENABLED=true  # Whisper aktivieren
```

## Whisper AI Service

### Aktivieren

Setze in `.env` oder docker-compose.yml:
```yaml
AI_WHISPER_LOCAL_ENABLED=true
```

Whisper Service wird automatisch gestartet.

### Deaktivieren

Für Kunden ohne Whisper-Anforderung:
```yaml
AI_WHISPER_LOCAL_ENABLED=false
```

Whisper Service wird nicht gestartet, App funktioniert ohne AI-Features.

## Database Setup

### Option 1: Docker PostgreSQL (einfach)

PostgreSQL läuft als Docker Container:
```yaml
pvs-db:
  image: postgres:15
  environment:
    - POSTGRES_DB=pvs
    - POSTGRES_USER=pvs_user
    - POSTGRES_PASSWORD=change_me
  volumes:
    - pvs-db-data:/var/lib/postgresql/data
```

### Option 2: Externe PostgreSQL

1. Erstelle externe PostgreSQL-Datenbank
2. Setze `DATABASE_URL` in docker-compose.yml:
   ```yaml
   DATABASE_URL=postgresql://user:pass@host:5432/dbname
   ```
3. Entferne `pvs-db` Service aus docker-compose.yml

### Migrationen

Bei erstem Start werden Flyway-Migrationen automatisch ausgeführt.

**Manuelle Migration:**
```bash
docker-compose exec pvs-app \
  java -jar app.jar --spring.flyway.migrate=true
```

## Backup-Strategie

### Automatische Backups

```bash
# Backup-Script (z.B. in cron)
docker-compose exec pvs-db \
  pg_dump -U pvs_user pvs > /backups/pvs-$(date +%Y%m%d).sql
```

### Restore

```bash
docker-compose exec -T pvs-db \
  psql -U pvs_user pvs < /backups/pvs-20250128.sql
```

## Updates

### Update-Prozess

1. Stoppe Services:
   ```bash
   docker-compose down
   ```

2. Backup erstellen:
   ```bash
   docker-compose exec pvs-db \
     pg_dump -U pvs_user pvs > backup.sql
   ```

3. Update Images:
   ```bash
   git pull  # Falls Code-Änderungen
   docker-compose pull
   docker-compose build
   ```

4. Starte Services:
   ```bash
   docker-compose up -d
   ```

5. Prüfe Logs:
   ```bash
   docker-compose logs -f pvs-app
   ```

## Troubleshooting

### App startet nicht

```bash
# Prüfe Logs
docker-compose logs pvs-app

# Prüfe Health Check
curl http://localhost:8080/actuator/health
```

### Database Connection Fehler

```bash
# Prüfe DB Status
docker-compose ps pvs-db

# Teste Connection
docker-compose exec pvs-db \
  psql -U pvs_user -d pvs -c "SELECT version();"
```

### Whisper Service Fehler

```bash
# Prüfe Whisper Logs
docker-compose logs pvs-whisper

# Teste Whisper Health
curl http://localhost:9000/health
```

### Port bereits belegt

Ändere Port-Mapping in docker-compose.yml:
```yaml
ports:
  - "9090:8080"  # Externer Port: Interner Port
```

## Production Considerations

### Resource Limits

```yaml
services:
  pvs-app:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          memory: 1G
```

### Persistente Volumes

```yaml
volumes:
  pvs-db-data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /path/to/persistent/storage
```

### Reverse Proxy (Nginx)

Für HTTPS und Domain-Setup:

```nginx
server {
    listen 443 ssl;
    server_name pvs.example.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Support

Bei Problemen:
1. Prüfe Logs: `docker-compose logs`
2. Prüfe Health Endpoint: `/actuator/health`
3. Kontaktiere Support mit Logs

