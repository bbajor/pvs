# Cloud Runbook - Häufige Operationen

Schnellreferenz für häufige Operationen im Cloud-Deployment.

## Deployment

### Neues Deployment starten

```bash
# Via GitHub Actions
# GitHub → Actions → "Cloud Deployment (Hetzner)" → Run workflow

# Manuell auf Server
cd /opt/pvs
./scripts/deployment/deploy-hetzner.sh prod <image-tag>
```

### Rollback durchführen

```bash
# Liste verfügbare Images
podman images | grep pvs

# Tagge altes Image
podman tag ghcr.io/bbajor/pvs:prod-backup-YYYYMMDD-HHMMSS \
           ghcr.io/bbajor/pvs:prod-latest

# Starte Container neu
podman-compose -f podman-compose.production.yml --profile prod restart pvs-prod
```

## Backups

### Backup erstellen

```bash
# Manuell
/opt/pvs/scripts/deployment/backup-postgres.sh

# Automatisch (via Cron)
# Läuft täglich um 02:00 Uhr
# Crontab: 0 2 * * * /opt/pvs/scripts/deployment/backup-postgres.sh
```

### Backup wiederherstellen

```bash
# Liste Backups
ls -lh /opt/pvs/backups/

# Restore
/opt/pvs/scripts/deployment/restore-postgres.sh \
  /opt/pvs/backups/pvs_prod_20240101_020000.sql.gz
```

### Disaster Recovery

**RTO (Recovery Time Objective):** < 1 Stunde
**RPO (Recovery Point Objective):** < 24 Stunden (tägliche Backups)

**DR-Prozess:**
1. Identifiziere das letzte erfolgreiche Backup
2. Stoppe die Anwendung
3. Führe Restore aus: `./scripts/deployment/restore-postgres.sh <backup-file>`
4. Starte die Anwendung neu
5. Prüfe Health-Checks und Logs
6. Teste kritische Funktionen

**DR-Test (empfohlen alle 3 Monate):**
1. Erstelle Test-Backup
2. Führe Restore in Test-Umgebung durch
3. Validiere Datenintegrität
4. Dokumentiere Ergebnisse

## Monitoring

### Health Check

```bash
# Application Health
curl http://localhost:8080/actuator/health

# Prometheus Metrics
curl http://localhost:8080/actuator/prometheus
```

### Logs anzeigen

```bash
# Application Logs (Container)
podman logs -f pvs-prod

# Log-Dateien
tail -f /var/log/pvs/application.log

# Logs mit Filter
podman logs pvs-prod | grep ERROR
```

## Datenbank

### Datenbank-Verbindung testen

```bash
# Via psql
psql -h <host> -p <port> -U <user> -d <database>

# Via Container
podman exec -it postgres-prod psql -U pvs_user -d pvs_prod
```

### Datenbank-Statistiken

```bash
# Aktive Verbindungen
psql -d pvs_prod -c "SELECT count(*) FROM pg_stat_activity;"

# Große Tabellen
psql -d pvs_prod -c "
  SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
  FROM pg_tables
  WHERE schemaname = 'public'
  ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
  LIMIT 10;
"
```

## Redis

### Redis-Status prüfen

```bash
# Ping
redis-cli -h localhost -p 6379 ping

# Info
redis-cli -h localhost -p 6379 INFO

# Sessions anzeigen
redis-cli -h localhost -p 6379 KEYS "spring:session:pvs:*"
```

### Redis zurücksetzen

```bash
# Alle Sessions löschen (VORSICHT!)
redis-cli -h localhost -p 6379 FLUSHDB
```

## Container-Management

### Container neu starten

```bash
# Einzelner Container
podman restart pvs-prod

# Alle Container (Profile)
podman-compose -f podman-compose.production.yml --profile prod restart
```

### Container-Logs

```bash
# Live-Logs
podman logs -f pvs-prod

# Letzte 100 Zeilen
podman logs --tail 100 pvs-prod

# Logs seit Zeitpunkt
podman logs --since 1h pvs-prod
```

## Updates

### System-Updates

```bash
# Ubuntu/Debian
apt-get update
apt-get upgrade -y

# Podman-Images aktualisieren
podman-compose -f podman-compose.production.yml --profile prod pull
```

## Notfall-Prozeduren

### Application nicht erreichbar

1. Prüfe Container-Status: `podman ps`
2. Prüfe Logs: `podman logs pvs-prod`
3. Prüfe Health: `curl http://localhost:8080/actuator/health`
4. Container neu starten: `podman restart pvs-prod`

### Datenbank-Probleme

1. Prüfe Datenbank-Verbindung
2. Prüfe Disk Space: `df -h`
3. Prüfe PostgreSQL-Logs: `podman logs postgres-prod`
4. Bei Bedarf: Restore aus Backup

### Performance-Probleme

1. Prüfe Container-Ressourcen: `podman stats`
2. Prüfe Datenbank-Performance: Slow-Query-Log
3. Prüfe Application-Metrics: `/actuator/prometheus`
4. Skaliere bei Bedarf: Mehrere Instanzen + Load Balancer

## Kontakte

- **GitHub Issues**: [Cloud-Migration Issues](https://github.com/bbajor/pvs/issues?q=is%3Aissue+is%3Aopen+label%3Acloud-migration)
- **Dokumentation**: [docs/](./)
- **Troubleshooting**: [CLOUD_TROUBLESHOOTING.md](./CLOUD_TROUBLESHOOTING.md)


