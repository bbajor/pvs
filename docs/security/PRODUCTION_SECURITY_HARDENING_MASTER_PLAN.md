# Production Security Hardening - Master Plan

**Status:** 🟢 Active  
**Erstellt:** 2025-10-30  
**Branch-Strategie:** Feature-Branch mit Sub-Branches  

---

## Überblick

Dieses Dokument organisiert die Production Security Hardening Tasks auf mehrere Agenten, um parallele Entwicklung und konfliktfreies Merging zu ermöglichen.

---

## Branch-Strategie

```
main
  └── feature/production-security-hardening (Haupt-Feature-Branch)
       ├── feature/security-infrastructure (Agent 1)
       ├── feature/security-spring-headers (Agent 2)
       ├── feature/security-auth-mfa (Agent 3)
       ├── feature/security-database-secrets (Agent 4)
       └── feature/security-monitoring-backup (Agent 5)
```

### Merge-Reihenfolge

1. **Agent 1** (Infrastructure) → Haupt-Feature-Branch
2. **Agent 2** (Spring Security Headers) → Haupt-Feature-Branch
3. **Agent 3** (MFA + Rate Limiting) → Haupt-Feature-Branch
4. **Agent 4** (Database + Secrets) → Haupt-Feature-Branch
5. **Agent 5** (Monitoring + Backup) → Haupt-Feature-Branch
6. **Haupt-Feature-Branch** → main (nach Testing)

---

## Agent-Aufgaben-Verteilung

### 🐳 Agent 1: Infrastructure & Reverse Proxy
**Branch:** `feature/security-infrastructure`  
**Konfliktbereich:** Docker-Compose, Dockerfile, Nginx/Traefik Configs  
**Priorität:** 🔴 Highest (Basis für andere)

#### Tasks
- [ ] **Reverse-Proxy Setup (Traefik oder Nginx)**
  - Traefik Docker-Compose Service hinzufügen
  - Routing-Regeln für pvs-app konfigurieren
  - Health-Check-Endpoints einrichten
  - Logging für Proxy-Requests
  - Geo-IP-Filtering optional vorbereiten
- [ ] **HTTPS/TLS Verschlüsselung**
  - Let's Encrypt Integration via Traefik
  - Automatische Zertifikats-Erneuerung
  - HTTP → HTTPS Redirect
  - HSTS Headers im Proxy
  - TLS 1.2+ erzwingen
- [ ] **Container Security**
  - Non-Root User in Dockerfiles
  - Multi-Stage Builds optimieren
  - Minimal Base Images (Alpine/Distroless evaluieren)
  - Security Scanning Setup (Trivy)
  - Image-Versionierung
- [ ] **Docker Network Security**
  - Interne Netzwerke für DB/Services
  - Nur notwendige Ports exponieren
  - Network Policies dokumentieren

#### Betroffene Dateien
- `docker-compose.yml` → `docker-compose.production.yml`
- `Dockerfile` (Optimierungen)
- Neue Dateien: 
  - `docker/traefik/traefik.yml`
  - `docker/traefik/docker-compose.traefik.yml`
  - `docs/deployment/REVERSE_PROXY_SETUP.md`
  - `docs/security/TLS_SETUP.md`
  - `docs/security/CONTAINER_SECURITY.md`

#### Abhängigkeiten
- Keine (kann sofort starten)

---

### 🛡️ Agent 2: Spring Security Headers & OWASP
**Branch:** `feature/security-spring-headers`  
**Konfliktbereich:** Spring Security Config, neue Filter-Klassen  
**Priorität:** 🔴 High

#### Tasks
- [ ] **Security Headers Implementation**
  - Content-Security-Policy (CSP)
  - X-Frame-Options (DENY)
  - X-Content-Type-Options (nosniff)
  - Referrer-Policy
  - Permissions-Policy
  - HSTS (ergänzend zu Traefik)
- [ ] **Spring Security Base Configuration**
  - SecurityFilterChain konfigurieren
  - CSRF Protection aktivieren (Vaadin-kompatibel)
  - Secure Cookie Flags (HttpOnly, Secure, SameSite)
  - Session Management härten
- [ ] **OWASP Top 10 Compliance**
  - XSS Protection (CSP + Input Validation)
  - SQL-Injection Protection (bereits via JPA, dokumentieren)
  - Clickjacking Protection
  - MIME-Sniffing Prevention
- [ ] **Spring Boot Security Config**
  - `server.forward-headers-strategy=framework` setzen
  - Actuator Endpoints absichern
  - Error-Handling ohne sensible Infos

#### Betroffene Dateien
- Neue Dateien:
  - `src/main/java/de/bbajor/pvs/security/SecurityHeadersConfiguration.java`
  - `src/main/java/de/bbajor/pvs/security/WebSecurityConfig.java`
  - `src/main/java/de/bbajor/pvs/security/filter/SecurityHeadersFilter.java`
  - `docs/security/SECURITY_HEADERS.md`
- Modifizierte Dateien:
  - `src/main/resources/application-prod.yaml` (neue Security-Properties)

#### Abhängigkeiten
- Keine direkten Code-Abhängigkeiten
- Sollte nach Agent 1 gemergt werden (wegen forward-headers-strategy)

---

### 🔐 Agent 3: MFA & Rate Limiting
**Branch:** `feature/security-auth-mfa`  
**Konfliktbereich:** User Entity, Authentication Services, neue Filter  
**Priorität:** 🟡 Medium-High

#### Tasks
- [ ] **Multi-Factor Authentication (TOTP)**
  - Spring Security TOTP Integration
  - User Entity erweitern (mfaEnabled, mfaSecret, backupCodes)
  - QR-Code-Generierung für Setup
  - TOTP-Validierung im Login-Flow
  - Backup-Codes generieren und hashen
  - Optional: SMS/Email 2FA vorbereiten (Struktur)
- [ ] **MFA UI Components (Vaadin)**
  - MFA-Setup-Dialog
  - MFA-Verification-View
  - Admin-Panel: MFA-Management
  - User-Settings: MFA aktivieren/deaktivieren
- [ ] **Rate Limiting & Brute-Force Protection**
  - Bucket4j oder Resilience4j Integration
  - Login-Attempts-Tracking
  - Account-Lockout nach X Versuchen
  - IP-basiertes Rate Limiting
  - Configurable Thresholds (dev/test/prod)
  - Alerts bei verdächtigen Aktivitäten
- [ ] **Rate Limiting Filter**
  - RateLimitingFilter implementieren
  - Redis/In-Memory Storage für Rate-Limit-Counters
  - Per-User und Per-IP Limiting

#### Betroffene Dateien
- Modifizierte Dateien:
  - `src/main/java/de/bbajor/pvs/entity/User.java` (MFA-Felder)
- Neue Dateien:
  - `src/main/java/de/bbajor/pvs/security/mfa/TotpService.java`
  - `src/main/java/de/bbajor/pvs/security/mfa/MfaAuthenticationProvider.java`
  - `src/main/java/de/bbajor/pvs/security/mfa/BackupCodeService.java`
  - `src/main/java/de/bbajor/pvs/security/ratelimit/RateLimitingFilter.java`
  - `src/main/java/de/bbajor/pvs/security/ratelimit/RateLimitService.java`
  - `src/main/java/de/bbajor/pvs/security/ratelimit/LoginAttemptsService.java`
  - `src/main/java/de/bbajor/pvs/views/security/MfaSetupView.java`
  - `src/main/java/de/bbajor/pvs/views/security/MfaVerificationView.java`
  - `docs/security/TOTP_SETUP.md`
  - `docs/security/HYBRID_2FA_SETUP.md`
  - `docs/security/RATE_LIMITING.md`
- Dependencies:
  - `build.gradle` (Bucket4j, TOTP-Library, QR-Code-Generator)

#### Abhängigkeiten
- Sollte nach Agent 2 gemergt werden (nutzt SecurityConfig)

---

### 🗄️ Agent 4: Database Security & Secrets Management
**Branch:** `feature/security-database-secrets`  
**Konfliktbereich:** application.yaml, application-prod.yaml, DB-Config  
**Priorität:** 🟡 Medium

#### Tasks
- [ ] **Database Security**
  - PostgreSQL SSL/TLS Connection konfigurieren
  - Connection-Pooling mit Limits (HikariCP)
  - Datenbank-Zugriff nur über interne Netzwerke
  - Database-User mit minimalen Rechten
  - Connection-String Security Review
- [ ] **Secrets Management**
  - Alle Credentials aus Code entfernen (Review)
  - Environment-Variablen für alle Secrets
  - `.env.example` für Prod-Deployment erstellen
  - Secrets-Rotation-Dokumentation
  - Optional: HashiCorp Vault Integration vorbereiten
- [ ] **application.yaml Security Hardening**
  - Alle Prod-Secrets durch `${ENV_VAR}` ersetzen
  - Dev/Test-Profile beibehalten (Test-Credentials OK)
  - Prod-Profile: NULL-Toleranz für hardcodierte Secrets
  - Datasource Security Properties
- [ ] **Backup Encryption Setup**
  - Backup-Encryption-Strategie dokumentieren
  - GPG/OpenSSL für Backup-Verschlüsselung

#### Betroffene Dateien
- Modifizierte Dateien:
  - `src/main/resources/application.yaml` (DB-Config)
  - `src/main/resources/application-prod.yaml` (Secrets → Env-Vars)
  - `docker-compose.production.yml` (PostgreSQL SSL)
- Neue Dateien:
  - `.env.example` (Production)
  - `docs/security/DATABASE_SECURITY.md`
  - `docs/security/SECRETS_MANAGEMENT.md`
  - `docs/security/BACKUP_ENCRYPTION.md`

#### Abhängigkeiten
- Sollte nach Agent 1 gemergt werden (PostgreSQL in Docker-Compose)

---

### 📊 Agent 5: Logging, Monitoring & Backup
**Branch:** `feature/security-monitoring-backup`  
**Konfliktbereich:** Logging-Config, neue Scripts, Dependencies  
**Priorität:** 🟢 Medium-Low

#### Tasks
- [ ] **Structured Logging (JSON)**
  - Logback JSON-Encoder konfigurieren
  - Production Logging auf JSON umstellen
  - Dev/Test Logging weiterhin readable
- [ ] **Security Event Logging**
  - Login-Events loggen
  - Failed Authentication Attempts
  - Permission Denials
  - MFA-Events
  - Rate-Limit-Violations
- [ ] **Monitoring Setup**
  - Spring Boot Actuator erweitern
  - Prometheus Metrics exporter
  - Grafana Dashboard-Templates
  - Health-Check-Endpoints
  - Custom Security Metrics
- [ ] **Audit Logging**
  - Audit-Log für sensible Operationen
  - User-Actions tracking
  - Data-Access-Logging (DSGVO-konform)
- [ ] **Backup & Disaster Recovery**
  - Automatische DB-Backup-Scripts
  - Backup-Rotation-Strategie
  - Off-Site-Backup-Konfiguration
  - Disaster-Recovery-Plan dokumentieren
  - Backup-Restoration-Test-Prozess

#### Betroffene Dateien
- Modifizierte Dateien:
  - `src/main/resources/logback-spring.xml` (neu erstellen)
  - `src/main/resources/application-prod.yaml` (Logging-Config)
  - `build.gradle` (Logging-Dependencies)
- Neue Dateien:
  - `src/main/java/de/bbajor/pvs/security/audit/AuditLogger.java`
  - `src/main/java/de/bbajor/pvs/security/audit/SecurityEventLogger.java`
  - `docker/monitoring/prometheus.yml`
  - `docker/monitoring/grafana-dashboard.json`
  - `docker/monitoring/docker-compose.monitoring.yml`
  - `scripts/deployment/backup-database.sh`
  - `scripts/deployment/restore-database.sh`
  - `scripts/deployment/rotate-backups.sh`
  - `docs/security/LOGGING_MONITORING.md`
  - `docs/deployment/BACKUP_DISASTER_RECOVERY.md`

#### Abhängigkeiten
- Kann parallel zu anderen laufen
- Sollte als letztes gemergt werden (nutzt Security-Events von Agent 3)

---

## Merge-Strategie & Konflikt-Minimierung

### Konflikt-Matrix

| Agent | Konflikt-Dateien | Merge-Reihenfolge |
|-------|------------------|-------------------|
| Agent 1 | Docker-Compose, Dockerfile | 1 (zuerst) |
| Agent 2 | Spring Security Config | 2 |
| Agent 3 | User Entity, Security Services | 3 |
| Agent 4 | application.yaml, DB-Config | 4 |
| Agent 5 | Logging-Config, Dependencies | 5 (zuletzt) |

### Kritische Konflikt-Punkte

#### ⚠️ Potentieller Konflikt: `build.gradle`
**Betroffene Agenten:** 2, 3, 5

**Lösung:**
- Agent 2: Fügt Security-Dependencies hinzu
- Agent 3: Fügt MFA/Rate-Limiting-Dependencies hinzu
- Agent 5: Fügt Logging/Monitoring-Dependencies hinzu

**Konflikt-Vermeidung:**
- Jeder Agent dokumentiert seine Dependencies in einem separaten Kommentar-Block
- Beim Mergen: Dependencies manuell zusammenführen
- **Alternativ:** Agent 2 kümmert sich um ALLE Dependencies für alle Agents (koordiniert)

#### ⚠️ Potentieller Konflikt: `application-prod.yaml`
**Betroffene Agenten:** 2, 4, 5

**Lösung:**
- Agent 2: Security-Headers-Properties
- Agent 4: Database & Secrets
- Agent 5: Logging-Config

**Konflikt-Vermeidung:**
- Jeder Agent arbeitet in einem eigenen Konfig-Block
- Klare Sektion-Trennung:
  - `server:` → Agent 2
  - `spring.datasource:` → Agent 4
  - `logging:` → Agent 5

#### ⚠️ Potentieller Konflikt: `docker-compose.production.yml`
**Betroffene Agenten:** 1, 4, 5

**Lösung:**
- Agent 1: Reverse-Proxy, TLS
- Agent 4: PostgreSQL SSL
- Agent 5: Monitoring-Container

**Konflikt-Vermeidung:**
- Agent 1 erstellt die Basis-Datei
- Agent 4/5 erweitern mit neuen Services (keine Überschreibung)

---

## Test-Strategie pro Agent

### Agent 1 (Infrastructure)
- [ ] Traefik startet korrekt
- [ ] HTTPS-Redirect funktioniert
- [ ] Health-Checks grün
- [ ] TLS-Zertifikate werden generiert

### Agent 2 (Security Headers)
- [ ] Alle Security-Headers gesetzt
- [ ] CSRF-Protection funktioniert mit Vaadin
- [ ] Spring Security Tests grün

### Agent 3 (MFA)
- [ ] TOTP-Setup funktioniert
- [ ] QR-Code wird generiert
- [ ] Login mit MFA erfolgreich
- [ ] Rate Limiting greift

### Agent 4 (Database)
- [ ] PostgreSQL SSL-Connection
- [ ] Keine hardcodierten Secrets in Code
- [ ] Prod-Config lädt Env-Vars korrekt

### Agent 5 (Monitoring)
- [ ] JSON-Logging funktioniert
- [ ] Security-Events werden geloggt
- [ ] Prometheus Metrics verfügbar
- [ ] Backup-Scripts funktionieren

---

## Integration Testing (nach allen Merges)

### End-to-End Security Tests
- [ ] HTTPS-Verbindung erfolgreich
- [ ] Security-Headers alle gesetzt
- [ ] Login mit MFA funktioniert
- [ ] Rate Limiting greift
- [ ] Logging funktioniert
- [ ] Monitoring-Dashboard zeigt Daten

### Penetration Testing
- [ ] OWASP ZAP Scan
- [ ] SQL-Injection Tests
- [ ] XSS Tests
- [ ] CSRF Tests
- [ ] Brute-Force Tests

---

## Zeitplan (Orientierung)

| Phase | Agent | Dauer | Start nach |
|-------|-------|-------|------------|
| Phase 1 | Agent 1 | 2-3 Tage | Sofort |
| Phase 2a | Agent 2 | 2-3 Tage | Nach Agent 1 gemergt |
| Phase 2b | Agent 4 | 2 Tage | Parallel zu Agent 2 |
| Phase 3 | Agent 3 | 3-4 Tage | Nach Agent 2 gemergt |
| Phase 4 | Agent 5 | 2-3 Tage | Parallel zu Agent 3 |
| Phase 5 | Integration Testing | 2 Tage | Nach allen gemergt |

**Gesamt:** ca. 2-3 Wochen

---

## Erfolgs-Kriterien

### Infrastructure (Agent 1)
- ✅ Traefik läuft als Reverse-Proxy
- ✅ HTTPS mit Let's Encrypt funktioniert
- ✅ Container laufen als Non-Root
- ✅ Security-Scans sind eingerichtet

### Spring Security (Agent 2)
- ✅ Alle Security-Headers gesetzt
- ✅ CSRF-Protection aktiv
- ✅ OWASP Top 10 Compliance

### Authentication (Agent 3)
- ✅ MFA optional aktivierbar
- ✅ Rate Limiting aktiv
- ✅ Brute-Force Protection funktioniert

### Database (Agent 4)
- ✅ Keine hardcodierten Credentials
- ✅ PostgreSQL SSL aktiv
- ✅ Secrets aus Environment-Variablen

### Monitoring (Agent 5)
- ✅ Structured Logging (JSON)
- ✅ Security-Events werden geloggt
- ✅ Monitoring-Dashboard läuft
- ✅ Backup-Strategie getestet

---

## Kommunikation zwischen Agenten

### Koordinations-Dokument
Dieses Dokument (`PRODUCTION_SECURITY_HARDENING_MASTER_PLAN.md`) dient als zentrale Referenz.

### Bei Konflikten
1. In diesem Dokument nachschlagen
2. Konflikt-Matrix prüfen
3. Bei Unsicherheit: Mit koordinierendem Agent (Agent 1) abstimmen

### Bei API-Änderungen
Wenn ein Agent eine Schnittstelle ändert, die andere nutzen:
- Änderung in diesem Dokument dokumentieren
- Betroffene Agenten benachrichtigen

---

## Dokumentations-Checkliste

Jeder Agent muss folgende Dokumentation erstellen:
- [ ] README.md aktualisieren (falls relevant)
- [ ] Spezifische Security-Docs in `docs/security/`
- [ ] Deployment-Docs in `docs/deployment/` (falls relevant)
- [ ] Inline-Code-Kommentare für komplexe Logik
- [ ] Test-Dokumentation

---

## Review & Sign-Off

Nach jedem Merge in den Haupt-Feature-Branch:
- [ ] Code-Review durchführen
- [ ] Tests müssen grün sein
- [ ] Dokumentation vollständig
- [ ] Keine Merge-Konflikte
- [ ] Build erfolgreich

---

## Notizen

- **Pragmatischer Ansatz:** Nicht alle "Optional"-Features müssen sofort implementiert werden
- **Iterativ:** Features können in späteren Sprints erweitert werden
- **Fokus auf Merge-Konflikt-Freiheit:** Lieber etwas mehr Koordination als später Konflikte lösen
- **Testing ist Pflicht:** Keine Merges ohne funktionierende Tests

---

**Erstellt:** 2025-10-30  
**Version:** 1.0  
**Besitzer:** Security Team  
**Status:** 🟢 Ready to Start
