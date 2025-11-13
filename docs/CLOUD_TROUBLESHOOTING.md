# Cloud Troubleshooting Guide

Häufige Probleme und Lösungen beim Cloud-Deployment.

## Application startet nicht

### Problem: Container startet nicht

```bash
# Prüfe Container-Status
podman ps -a

# Prüfe Logs
podman logs pvs-prod

# Prüfe Environment-Variablen
podman exec pvs-prod env | grep -E 'DATABASE|SMTP|REDIS'
```

### Lösung: Prüfe Environment-Variablen

Stelle sicher, dass alle erforderlichen Environment-Variablen gesetzt sind:
- `SMTP_ENCRYPTION_KEY` (ERFORDERLICH für Cloud)
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT` (wenn Redis verwendet wird)

## Datenbank-Verbindungsfehler

### Problem: "Connection refused" oder "Authentication failed"

```bash
# Prüfe Datenbank-Verbindung
psql -h <host> -p <port> -U <user> -d <database>

# Prüfe Firewall
ufw status
```

### Lösung: Firewall-Regeln anpassen

```bash
# Erlaube Datenbank-Zugriff (nur intern)
ufw allow from 10.0.0.0/8 to any port 5432
```

## Redis-Verbindungsfehler

### Problem: "Connection refused" zu Redis

```bash
# Prüfe Redis-Status
podman ps | grep redis
podman logs pvs-redis

# Teste Redis-Verbindung
redis-cli -h localhost -p 6379 ping
```

### Lösung: Redis-Container starten

```bash
podman-compose -f podman-compose.production.yml --profile cloud up -d redis
```

## Health Check schlägt fehl

### Problem: `/actuator/health` gibt Fehler zurück

```bash
# Prüfe Health-Endpoint
curl http://localhost:8080/actuator/health

# Prüfe detaillierte Health-Informationen
curl http://localhost:8080/actuator/health | jq
```

### Lösung: Prüfe einzelne Health-Indicators

- Database: Prüfe Datenbank-Verbindung
- Redis: Prüfe Redis-Verbindung
- Whisper: Prüfe Whisper-Service (optional)

## Performance-Probleme

### Problem: Langsame Datenbank-Queries

```bash
# Aktiviere Slow-Query-Log (PostgreSQL)
# In PostgreSQL-Konfiguration:
log_min_duration_statement = 1000  # Log queries > 1 second

# Prüfe aktive Queries
psql -c "SELECT * FROM pg_stat_activity WHERE state = 'active';"
```

### Lösung: Indizes prüfen und optimieren

```bash
# Führe ANALYZE aus
psql -d pvs_prod -c "ANALYZE;"

# Prüfe fehlende Indizes
# Siehe V17__optimize_database_performance.sql
```

## Session-Probleme

### Problem: Sessions gehen verloren

```bash
# Prüfe Redis-Session-Storage
redis-cli KEYS "spring:session:pvs:*"

# Prüfe Session-Timeout
grep SESSION_TIMEOUT /opt/pvs/.env
```

### Lösung: Redis-Verbindung prüfen

- Stelle sicher, dass Redis läuft
- Prüfe `REDIS_HOST` und `REDIS_PORT` in `.env`
- Prüfe `REDIS_PASSWORD` (falls gesetzt)

## SMTP-Fehler

### Problem: E-Mails werden nicht versendet

```bash
# Prüfe SMTP-Konfiguration
podman exec pvs-prod env | grep SMTP

# Prüfe Logs für SMTP-Fehler
podman logs pvs-prod | grep -i smtp
```

### Lösung: SMTP-Konfiguration prüfen

- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` müssen gesetzt sein
- Prüfe Firewall-Regeln für SMTP-Port (587, 465)
- Teste SMTP-Verbindung manuell

## Deployment-Fehler

### Problem: GitHub Actions Deployment schlägt fehl

```bash
# Prüfe GitHub Actions Logs
# Gehe zu: GitHub → Actions → Failed Workflow → View logs

# Prüfe SSH-Verbindung manuell
ssh -i ~/.ssh/hetzner_deploy root@<server-ip>
```

### Lösung: SSH-Key und Secrets prüfen

- Stelle sicher, dass `HETZNER_SSH_KEY` in GitHub Secrets gesetzt ist
- Prüfe, dass der öffentliche Key auf dem Server in `~/.ssh/authorized_keys` ist
- Teste SSH-Verbindung manuell

## Disk Space

### Problem: Kein Speicherplatz mehr

```bash
# Prüfe Disk Space
df -h

# Prüfe große Dateien
du -sh /opt/pvs/backups/*
du -sh /var/log/pvs/*
```

### Lösung: Alte Backups und Logs löschen

```bash
# Lösche alte Backups (> 30 Tage)
find /opt/pvs/backups -name "*.sql.gz" -mtime +30 -delete

# Rotiere Logs
logrotate -f /etc/logrotate.d/pvs
```

## Weitere Hilfe

- GitHub Issues: [Cloud-Migration Issues](https://github.com/bbajor/pvs/issues?q=is%3Aissue+is%3Aopen+label%3Acloud-migration)
- Dokumentation: [CLOUD_ENV_VARIABLES.md](./CLOUD_ENV_VARIABLES.md)
- Runbook: [CLOUD_RUNBOOK.md](./CLOUD_RUNBOOK.md)


