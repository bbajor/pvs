# Backup & Disaster Recovery

**Production Database Backup-Strategie**

---

## 🎯 Backup-Strategie

### Automated Backups
- **Frequency:** Täglich (via Cron)
- **Retention:** 30 Tage
- **Encryption:** Optional (GPG)
- **Location:** `/backups/prod` (Docker-Volume)

---

## 🚀 Backup-Scripts

### backup-database.sh

**Location:** `scripts/deployment/backup-database.sh`

**Features:**
- PostgreSQL `pg_dump` (Custom Format)
- Gzip-Compression
- Optional GPG-Encryption
- Auto-Cleanup (30 Tage)

**Usage:**
```bash
# Manual Backup
./scripts/deployment/backup-database.sh

# Mit Encryption
BACKUP_GPG_KEY="admin@example.com" ./scripts/deployment/backup-database.sh

# Custom Retention
RETENTION_DAYS=90 ./scripts/deployment/backup-database.sh
```

**Cron-Setup:**
```bash
# Daily at 2 AM
0 2 * * * /app/scripts/deployment/backup-database.sh >> /var/log/backup.log 2>&1
```

---

### restore-database.sh

**Location:** `scripts/deployment/restore-database.sh`

**Features:**
- Auto-Decrypt (GPG)
- Auto-Decompress (Gzip)
- PostgreSQL `pg_restore`
- Interactive Confirmation

**Usage:**
```bash
# List available backups
./scripts/deployment/restore-database.sh

# Restore specific backup
./scripts/deployment/restore-database.sh /backups/prod/pvs_backup_20251030_120000.sql.gz
```

---

## 🧪 Testing

### Test Backup

```bash
# 1. Create Backup
./scripts/deployment/backup-database.sh

# 2. Verify Backup
ls -lh /backups/prod/

# 3. Test Restore (in Test-Env!)
./scripts/deployment/restore-database.sh /backups/prod/pvs_backup_*.sql.gz
```

---

## 📚 Disaster Recovery Plan

### RTO & RPO
- **RTO (Recovery Time Objective):** 1 Stunde
- **RPO (Recovery Point Objective):** 24 Stunden (täglich)

### Recovery-Prozess
1. Identify Failure (Monitoring-Alerts)
2. Stop App-Container
3. Restore Latest Backup
4. Restart App-Container
5. Verify Functionality
6. Document Incident

---

**Erstellt:** 2025-10-30  
**Version:** 1.0 (Agent 5)
