# Rollback-Strategie

Dieses Dokument beschreibt die Rollback-Prozesse für verschiedene Szenarien.

## Automatisches Rollback

### Wann wird automatisch zurückgerollt?

1. **Health Check Failure** nach Deployment
   - Render erkennt automatisch Health Check Failures
   - Alte Version wird automatisch reaktiviert

2. **Migration-Fehler (Flyway)**
   - Flyway führt Migrations in Transaktionen aus
   - Bei Fehler: Automatisches Rollback der Migration
   - App bleibt bei alter Version

3. **Application Startup Failure**
   - Render erkennt fehlgeschlagenen Start
   - Vorherige Version wird reaktiviert

## Manuelles Rollback

### Via GitHub Actions (Empfohlen)

1. GitHub Actions → "Rollback Production"
2. "Run workflow"
3. Confirmation: `ROLLBACK` eingeben
4. Rollback wird ausgeführt
5. Health Check wird validiert

### Via Render Dashboard

1. Render Dashboard → Service → "Events"
2. "Rollback to previous deploy" klicken
3. Bestätige Rollback

### Via Render CLI

```bash
render rollback --service <service-id>
```

## Database Migration Rollback

### Flyway Repair (bei korrupten Schemas)

Wenn Flyway Schema History korrupt ist:

```sql
-- Prüfe Flyway Schema History
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;

-- Bei Bedarf: Flyway Repair
UPDATE flyway_schema_history 
SET success = false 
WHERE version = 'V2';
```

**Achtung**: Nur wenn Migration tatsächlich fehlgeschlagen ist!

### Manuelle Migration-Rollback

Wenn eine Migration Probleme verursacht:

1. Erstelle Rollback-Migration: `V3__rollback_v2.sql`
2. Führe Rollback-Migration aus
3. In Flyway Schema History eintragen:

```sql
DELETE FROM flyway_schema_history WHERE version = 'V2';
```

**Besser**: Verwende immer reversible Migrations!

### Beispiel: Reversible Migration

```sql
-- V2__add_email_column.sql
ALTER TABLE user_account ADD COLUMN email VARCHAR(255);

-- V3__rollback_add_email_column.sql (falls nötig)
ALTER TABLE user_account DROP COLUMN IF EXISTS email;
```

## Notfall-Rollback-Prozess

### Production Incident Response

1. **Sofortiges Rollback** (max. 5 Minuten)
   - GitHub Actions: "Rollback Production" → `ROLLBACK`
   - Alternativ: Render Dashboard → Rollback

2. **Communication**
   - Notifiziere Team (falls konfiguriert)
   - Dokumentiere Incident

3. **Post-Mortem**
   - Analysiere Logs
   - Identifiziere Root Cause
   - Fix vorbereiten
   - Deploy Fix zu Test → validiere → Deploy zu Prod

## Präventive Maßnahmen

### Vor Production Deployment

1. ✅ Tests laufen erfolgreich
2. ✅ Migrationen wurden in Test validiert
3. ✅ Health Check funktioniert in Test
4. ✅ Monitoring ist aktiv
5. ✅ Rollback-Prozess ist verstanden

### Deployment Checklist

- [ ] Feature wurde in Test getestet
- [ ] Migrationen wurden in Test erfolgreich ausgeführt
- [ ] Performance wurde validiert
- [ ] Breaking Changes sind dokumentiert
- [ ] Rollback-Strategie ist klar

## Recovery Szenarien

### Szenario 1: Application startet nicht

**Symptom**: Health Check schlägt fehl, App erreichbar

**Lösung**:
1. Automatisches Rollback (Render)
2. Oder manuelles Rollback via GitHub Actions

### Szenario 2: Database Migration schlägt fehl

**Symptom**: App startet, aber DB-Zugriff funktioniert nicht

**Lösung**:
1. Flyway Repair (falls Schema History korrupt)
2. Manuelle Migration-Rollback
3. Application Rollback

### Szenario 3: Performance Issues

**Symptom**: App läuft, aber sehr langsam

**Lösung**:
1. Scale up in Render Dashboard (temporär)
2. Analysiere Logs für Bottlenecks
3. Fix vorbereiten und deployen
4. Scale down nach Fix

### Szenario 4: Datenverlust durch fehlerhafte Migration

**Symptom**: Daten fehlen nach Migration

**Lösung**:
1. **SOFORT**: Stoppe weitere Deployments
2. Database Backup wiederherstellen (falls vorhanden)
3. Application Rollback
4. Migration fixen und in Test validieren
5. Erneut deployen

**Prävention**: Immer Backups vor Migrationen!

## Backup-Strategie

### Render PostgreSQL Backups

Render erstellt automatisch Backups:
- **Starter Plan**: Tägliche Backups (7 Tage Retention)
- Zugriff: Render Dashboard → Database → "Backups"

### Manueller Backup-Export

```bash
# Via Render CLI
render pg:backup <database-name>

# Oder direkt via pg_dump
pg_dump $DATABASE_URL > backup.sql
```

### Backup vor kritischen Migrations

1. Erstelle manuelles Backup
2. Führe Migration in Test aus
3. Validiere Ergebnisse
4. Deploy zu Prod

## Monitoring für Rollback-Entscheidungen

### Key Metrics

1. **Error Rate**: >5% → Rollback in Erwägung ziehen
2. **Response Time**: >2x Baseline → Performance-Problem
3. **Database Connections**: Max Connections erreicht → Scale/Investigate
4. **Memory Usage**: >80% → Memory-Leak oder Scale

### Alerts Setup

Konfiguriere Alerts in Render für:
- Health Check Failures
- High Error Rates (>10%)
- Database Connection Errors

