# Agent 5: Logging, Monitoring & Backup

**Branch:** `feature/security-monitoring-backup`  
**Priorität:** 🟢 Medium-Low  
**Start:** Parallel zu Agent 3/4 möglich  
**Geschätzte Dauer:** 2-3 Tage  

---

## 🎯 Mission

Structured Logging (JSON), Security Event Logging, Monitoring (Prometheus + Grafana), Backup & Disaster Recovery.

---

## 📋 Tasks

### 1. Structured Logging (JSON)

#### Logback JSON-Encoder
- [ ] **Logback-Spring.xml erstellen**
  - JSON-Encoder für Production
  - Readable-Logging für Dev/Test
  - Profile-spezifische Konfiguration
  - File-Appender + Console-Appender

- [ ] **JSON-Format konfigurieren**
  - Timestamp (ISO-8601)
  - Log-Level
  - Logger-Name
  - Message
  - Thread
  - MDC (Mapped Diagnostic Context)
  - Exception-Stacktrace

- [ ] **Production Logging**
  - JSON-Format für Log-Aggregation
  - File-Rotation (z.B. täglich, max 30 Tage)
  - Compression (gzip)

- [ ] **Dev/Test Logging**
  - Readable Console-Output
  - Colored-Output (optional)
  - Debug-Level für Dev

---

### 2. Security Event Logging

#### Security-Event-Logger Service
- [ ] **SecurityEventLogger implementieren**
  - Structured-Logging für Security-Events
  - Event-Types definieren (Enum)
    - LOGIN_SUCCESS
    - LOGIN_FAILED
    - MFA_SETUP
    - MFA_VERIFICATION_SUCCESS
    - MFA_VERIFICATION_FAILED
    - RATE_LIMIT_VIOLATION
    - ACCOUNT_LOCKOUT
    - PASSWORD_CHANGE
    - PERMISSION_DENIED
    - etc.

- [ ] **Event-Context**
  - User-ID
  - Username
  - IP-Address
  - User-Agent
  - Timestamp
  - Event-Details

#### Audit-Logging
- [ ] **AuditLogger implementieren**
  - Audit-Trail für sensible Operationen
  - User-Actions tracking
  - Data-Access-Logging (DSGVO-konform)
  - Immutable Logs (append-only)

- [ ] **Audit-Events**
  - PATIENT_DATA_ACCESS
  - PATIENT_DATA_MODIFY
  - USER_CREATED
  - USER_DELETED
  - ROLE_CHANGED
  - etc.

---

### 3. Monitoring Setup

#### Spring Boot Actuator erweitern
- [ ] **Custom Metrics**
  - Security-Metrics (Login-Success-Rate, Failed-Logins)
  - MFA-Metrics (MFA-Setup-Rate, MFA-Usage)
  - Rate-Limit-Metrics (Violations-Count)
  - Application-Metrics (Request-Count, Response-Time)

- [ ] **Health-Check-Endpoints**
  - Database-Health
  - External-Services-Health (Whisper, Aleph Alpha)
  - Disk-Space-Health
  - Custom Health-Indicators

#### Prometheus Integration
- [ ] **Prometheus Metrics Exporter**
  - Micrometer-Prometheus-Registry
  - Metrics-Endpoint: `/actuator/prometheus`
  - Custom-Metrics registrieren

- [ ] **Prometheus Docker-Compose**
  - Prometheus-Service in docker-compose.monitoring.yml
  - Scrape-Config für pvs-app
  - Retention-Policy (z.B. 15 Tage)

#### Grafana Dashboard
- [ ] **Grafana Setup**
  - Grafana-Service in docker-compose.monitoring.yml
  - Datasource: Prometheus
  - Dashboard-Template erstellen

- [ ] **Dashboard-Panels**
  - Security-Dashboard
    - Login-Success/Failed
    - MFA-Events
    - Rate-Limit-Violations
    - Account-Lockouts
  - Application-Dashboard
    - Request-Rate
    - Response-Time
    - Error-Rate
    - JVM-Metrics (Memory, GC)
  - Infrastructure-Dashboard
    - CPU-Usage
    - Memory-Usage
    - Disk-Usage
    - Network-IO

---

### 4. Alerts & Notifications

- [ ] **Alert-Rules definieren**
  - Excessive Failed-Logins
  - Brute-Force-Attempts
  - Rate-Limit-Violations
  - High Error-Rate
  - Service-Down

- [ ] **Alert-Mechanismus**
  - Grafana Alerting
  - Webhook für Notifications
  - Optional: E-Mail, Slack, PagerDuty

---

### 5. Backup & Disaster Recovery

#### Database-Backup
- [ ] **Backup-Script: backup-database.sh**
  - PostgreSQL `pg_dump`
  - Automated Backups (via Cron oder Systemd Timer)
  - Backup-Schedule (täglich)
  - Backup-Location (Off-Site)

- [ ] **Backup-Encryption**
  - GPG-Encryption nach Backup
  - Public-Key-Encryption
  - Encrypted-Backups Storage

- [ ] **Backup-Rotation**
  - Retention-Policy (z.B. 30 Tage täglich, 12 Monate monatlich)
  - Alte Backups automatisch löschen
  - Script: rotate-backups.sh

#### Backup-Restoration
- [ ] **Restore-Script: restore-database.sh**
  - PostgreSQL `pg_restore`
  - Decrypt Backup
  - Restore-Prozess dokumentieren
  - Test-Restore durchführen

#### Disaster-Recovery-Plan
- [ ] **DR-Plan dokumentieren**
  - RTO (Recovery Time Objective)
  - RPO (Recovery Point Objective)
  - Restore-Prozess Schritt-für-Schritt
  - Kontakt-Informationen
  - Checkliste für DR-Szenario

- [ ] **DR-Test**
  - Test-Restore durchführen
  - Downtime messen
  - Prozess validieren
  - Lessons Learned dokumentieren

---

## 📁 Betroffene Dateien

### Zu modifizieren
- `src/main/resources/application-prod.yaml` - Logging-Config
- `build.gradle` - Logging- und Monitoring-Dependencies

### Zu erstellen
**Config:**
- `src/main/resources/logback-spring.xml` - Logback-Konfiguration

**Services:**
- `src/main/java/de/bbajor/pvs/security/audit/AuditLogger.java`
- `src/main/java/de/bbajor/pvs/security/audit/SecurityEventLogger.java`
- `src/main/java/de/bbajor/pvs/security/audit/SecurityEvent.java` (Enum)
- `src/main/java/de/bbajor/pvs/monitoring/CustomMetrics.java`
- `src/main/java/de/bbajor/pvs/monitoring/CustomHealthIndicator.java`

**Docker:**
- `docker/monitoring/prometheus.yml` - Prometheus-Config
- `docker/monitoring/grafana-dashboard.json` - Grafana-Dashboard
- `docker/monitoring/docker-compose.monitoring.yml` - Monitoring-Services

**Scripts:**
- `scripts/deployment/backup-database.sh` - Backup-Script
- `scripts/deployment/restore-database.sh` - Restore-Script
- `scripts/deployment/rotate-backups.sh` - Rotation-Script
- `scripts/deployment/test-backup.sh` - Backup-Test-Script

**Docs:**
- `docs/security/LOGGING_MONITORING.md` - Logging & Monitoring Guide
- `docs/deployment/BACKUP_DISASTER_RECOVERY.md` - Backup & DR Guide

---

## 🧪 Testing

### Unit Tests
- [ ] SecurityEventLogger loggt korrekt
- [ ] AuditLogger erstellt Audit-Trail
- [ ] Custom Metrics werden registriert

### Integration Tests
- [ ] JSON-Logging funktioniert in Prod-Profil
- [ ] Prometheus Metrics verfügbar
- [ ] Security-Events erscheinen in Logs

### Backup Tests
- [ ] Backup-Script funktioniert
- [ ] Backup-Encryption funktioniert
- [ ] Restore-Script funktioniert
- [ ] Restored DB ist funktionsfähig

---

## 🔗 Abhängigkeiten

### Code-Abhängigkeiten
- **Agent 3:** Security-Events (MFA, Rate-Limiting) werden hier geloggt

### Dependencies (build.gradle)
```gradle
// JSON-Logging
implementation 'net.logstash.logback:logstash-logback-encoder:7.4'

// Prometheus Metrics
implementation 'io.micrometer:micrometer-registry-prometheus'

// Spring Boot Actuator (bereits vorhanden?)
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

---

## 📚 Dokumentation

### docs/security/LOGGING_MONITORING.md
- Logging-Strategie (JSON vs. Readable)
- Security-Event-Logging
- Audit-Logging
- Prometheus-Setup
- Grafana-Dashboard
- Alert-Rules
- Troubleshooting

### docs/deployment/BACKUP_DISASTER_RECOVERY.md
- Backup-Strategie
- Backup-Schedule
- Backup-Encryption
- Restore-Prozess
- Disaster-Recovery-Plan
- Testing-Backup-Restore

---

## 🎓 Hilfreiche Ressourcen

- [Logback Docs](https://logback.qos.ch/documentation.html)
- [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus Docs](https://prometheus.io/docs/)
- [Grafana Docs](https://grafana.com/docs/)
- [PostgreSQL Backup Docs](https://www.postgresql.org/docs/current/backup.html)

---

## ✅ Definition of Done

- [ ] Structured Logging (JSON) funktioniert in Prod
- [ ] Security-Events werden geloggt
- [ ] Audit-Logs für sensible Operationen
- [ ] Prometheus Metrics verfügbar
- [ ] Grafana-Dashboard läuft und zeigt Daten
- [ ] Custom Security-Metrics werden erfasst
- [ ] Backup-Script funktioniert (inkl. Encryption)
- [ ] Restore-Script funktioniert
- [ ] Backup-Rotation funktioniert
- [ ] DR-Plan dokumentiert und getestet
- [ ] Alle Tests grün
- [ ] Dokumentation vollständig
- [ ] Build erfolgreich
- [ ] Keine Linter-Errors

---

## 🚨 Wichtige Hinweise

1. **Secrets in Logs:** Niemals Passwörter, Tokens, API-Keys loggen!
2. **PII in Logs:** Personenbezogene Daten nur gehashed oder pseudonymisiert
3. **Log-Rotation:** Logs regelmäßig rotieren, um Disk-Space zu sparen
4. **Backup-Encryption:** Backups IMMER verschlüsseln (DSGVO!)
5. **Backup-Testing:** Regelmäßig Test-Restores durchführen
6. **Prometheus Retention:** Nicht zu lange (max. 15-30 Tage)
7. **Grafana Security:** Dashboard-Access mit Auth schützen

---

## 🤝 Koordination mit anderen Agenten

### Agent 1 (Infrastructure)
- Monitoring-Services in docker-compose.monitoring.yml
- Traefik-Routing für Grafana (optional)

### Agent 2 (Spring Security)
- Security-Events hier geloggt
- Actuator-Security (von Agent 2 konfiguriert)

### Agent 3 (MFA + Rate Limiting)
- Security-Events (MFA, Rate-Limit) hier geloggt
- Custom Metrics für MFA/Rate-Limiting

### Agent 4 (Database)
- Connection-Pool-Metrics
- Database-Backup-Scripts

---

## 📊 Custom Metrics Beispiele

### Security-Metrics
- `security.login.success.count` - Erfolgreiche Logins
- `security.login.failed.count` - Fehlgeschlagene Logins
- `security.mfa.setup.count` - MFA-Setups
- `security.mfa.verification.success.count` - MFA-Verifications erfolgreich
- `security.mfa.verification.failed.count` - MFA-Verifications fehlgeschlagen
- `security.ratelimit.violation.count` - Rate-Limit-Violations
- `security.account.lockout.count` - Account-Lockouts

### Application-Metrics
- `http.requests.total` - Total HTTP Requests
- `http.requests.duration` - Request-Duration
- `http.requests.errors.total` - HTTP-Errors

---

**Erstellt:** 2025-10-30  
**Status:** 🟢 Ready to Start (parallel möglich)  
**Nächster Agent:** Integration Testing (wenn ALLE Agents fertig)

---

## 🔗 Nach Abschluss: Integration Testing

Wenn du ALLE Tasks abgeschlossen hast UND alle anderen Agents (2, 3, 4) auch fertig sind:

### Starte Integration Testing
```
@cursor Hallo Integration-Tester! 👋

Alle Security-Hardening-Agents haben ihre Arbeit abgeschlossen. Zeit für Integration Testing!

# Deine Aufgabe
1. Lies: docs/security/PRODUCTION_SECURITY_HARDENING_MASTER_PLAN.md (Abschnitt "Integration Testing")
2. Führe End-to-End Security Tests durch
3. OWASP ZAP Penetration Testing
4. Performance Testing

# Nach Abschluss
Production-Deployment vorbereiten und Go-Live-Checkliste abarbeiten.

Viel Erfolg! 🚀
```
