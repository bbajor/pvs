# Disaster Recovery Plan

## RTO/RPO Definitionen

- **RTO (Recovery Time Objective):** < 1 Stunde
  - Ziel: System innerhalb von 1 Stunde nach Ausfall wiederherstellen
- **RPO (Recovery Point Objective):** < 24 Stunden
  - Ziel: Maximaler Datenverlust von 24 Stunden (tägliche Backups)

## Backup-Strategie

### Automatische Backups

- **Frequenz:** Täglich um 02:00 Uhr
- **Retention:** 30 Tage
- **Speicherort:** `/opt/pvs/backups/`
- **Format:** Komprimierte SQL-Dumps (`.sql.gz`)

### Backup-Konfiguration

```bash
# Crontab-Eintrag
0 2 * * * /opt/pvs/scripts/deployment/backup-postgres.sh
```

### Backup-Verifikation

```bash
# Prüfe letztes Backup
ls -lh /opt/pvs/backups/ | tail -5

# Prüfe Backup-Integrität
gunzip -t /opt/pvs/backups/pvs_prod_YYYYMMDD_HHMMSS.sql.gz
```

## Restore-Prozess

### Schritt-für-Schritt Anleitung

1. **Ausfall identifizieren**
   ```bash
   # Prüfe Application-Status
   curl http://localhost:8080/actuator/health
   
   # Prüfe Datenbank-Status
   podman exec postgres-prod pg_isready
   ```

2. **Backup auswählen**
   ```bash
   # Liste verfügbare Backups
   ls -lh /opt/pvs/backups/
   
   # Wähle das neueste erfolgreiche Backup
   BACKUP_FILE=/opt/pvs/backups/pvs_prod_YYYYMMDD_HHMMSS.sql.gz
   ```

3. **Anwendung stoppen**
   ```bash
   podman-compose -f podman-compose.production.yml --profile prod stop pvs-prod
   ```

4. **Datenbank wiederherstellen**
   ```bash
   ./scripts/deployment/restore-postgres.sh $BACKUP_FILE
   ```

5. **Anwendung starten**
   ```bash
   podman-compose -f podman-compose.production.yml --profile prod up -d pvs-prod
   ```

6. **Verifikation**
   ```bash
   # Warte auf Health-Check
   sleep 60
   
   # Prüfe Health
   curl http://localhost:8080/actuator/health
   
   # Prüfe Logs
   podman logs pvs-prod | tail -50
   ```

7. **Funktionstest**
   - Login testen
   - Kritische Funktionen testen
   - Datenintegrität prüfen

## DR-Test (empfohlen alle 3 Monate)

### Test-Prozedur

1. **Test-Umgebung vorbereiten**
   ```bash
   # Erstelle Test-Datenbank
   createdb -U pvs_user pvs_test_restore
   ```

2. **Backup in Test-Umgebung wiederherstellen**
   ```bash
   # Restore in Test-DB
   gunzip -c /opt/pvs/backups/pvs_prod_YYYYMMDD_HHMMSS.sql.gz | \
     psql -U pvs_user -d pvs_test_restore
   ```

3. **Datenintegrität prüfen**
   ```sql
   -- Prüfe Tabellen-Anzahl
   SELECT COUNT(*) FROM information_schema.tables 
   WHERE table_schema = 'public';
   
   -- Prüfe kritische Daten
   SELECT COUNT(*) FROM patient;
   SELECT COUNT(*) FROM user_account;
   SELECT COUNT(*) FROM institution;
   ```

4. **Ergebnisse dokumentieren**
   - Backup-Datum/Zeit
   - Restore-Dauer
   - Gefundene Probleme
   - Empfehlungen

## Notfall-Kontakte

- **GitHub Issues:** [Cloud-Migration Issues](https://github.com/bbajor/pvs/issues?q=is%3Aissue+is%3Aopen+label%3Acloud-migration)
- **Dokumentation:** [CLOUD_TROUBLESHOOTING.md](./CLOUD_TROUBLESHOOTING.md)
- **Runbook:** [CLOUD_RUNBOOK.md](./CLOUD_RUNBOOK.md)

## Präventive Maßnahmen

- Regelmäßige Backups (automatisch)
- Monitoring und Alerting
- Regelmäßige DR-Tests
- Dokumentation aktuell halten
