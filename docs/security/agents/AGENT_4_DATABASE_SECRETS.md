# Agent 4: Database Security & Secrets Management

**Branch:** `feature/security-database-secrets`  
**Priorität:** 🟡 Medium  
**Start:** Nach Agent 1 gemergt (parallel zu Agent 2 möglich)  
**Geschätzte Dauer:** 2 Tage  

---

## 🎯 Mission

Database Security (PostgreSQL SSL, Connection-Pooling), Secrets Management (Environment-Variablen, keine hardcodierten Credentials).

---

## 📋 Tasks

### 1. Database Security

#### PostgreSQL SSL/TLS Connection
- [ ] **SSL-Modus konfigurieren**
  - PostgreSQL SSL in docker-compose.production.yml
  - SSL-Zertifikate für PostgreSQL
  - Spring Boot Datasource SSL-Config
  - `ssl=true&sslmode=require` in JDBC-URL

- [ ] **Connection-String Security**
  - Keine Credentials in JDBC-URL
  - Username/Password via Environment-Variablen
  - SSL-Parameter konfigurieren

#### Connection-Pooling (HikariCP)
- [ ] **Pool-Limits konfigurieren**
  - `maximum-pool-size` (z.B. 10 für Prod)
  - `minimum-idle` (z.B. 5)
  - `connection-timeout` (z.B. 30000ms)
  - `idle-timeout` (z.B. 600000ms)
  - `max-lifetime` (z.B. 1800000ms)

- [ ] **Pool-Monitoring**
  - HikariCP Metrics exponieren
  - Pool-Health-Check

#### Database-User Security
- [ ] **Minimal Permissions**
  - Separater DB-User für Applikation (nicht `postgres`)
  - GRANT nur auf notwendige Tables
  - Kein `SUPERUSER`, kein `CREATEDB`
  - Dokumentation: Welche Permissions benötigt?

#### Network Security
- [ ] **Interne Netzwerke**
  - PostgreSQL nur über Docker-Internal-Network erreichbar
  - Kein Public-Zugriff auf Port 5432
  - Firewall-Regeln dokumentieren

---

### 2. Secrets Management

#### Environment-Variablen
- [ ] **Alle Secrets aus Code entfernen**
  - Code-Review: Suche nach hardcodierten Credentials
  - Passwörter, API-Keys, Tokens, etc.
  - Dev/Test: Test-Credentials erlaubt (dokumentieren)
  - Prod: NULL-Toleranz für hardcodierte Secrets

- [ ] **Environment-Variablen definieren**
  - `DATABASE_URL` oder `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`
  - `DATABASE_USERNAME`
  - `DATABASE_PASSWORD`
  - `AI_API_KEY` (Aleph Alpha)
  - `SPRING_PROFILES_ACTIVE`
  - Weitere nach Bedarf

- [ ] **.env.example für Production erstellen**
  - Template mit Platzhaltern
  - Dokumentation: Welche Variablen erforderlich?
  - Deployment-Guide: Wie Env-Vars setzen?

#### application.yaml Hardening
- [ ] **application-prod.yaml: Secrets → Env-Vars**
  - Alle Datasource-Credentials via `${DATABASE_PASSWORD}`
  - API-Keys via `${AI_API_KEY:}`
  - Keine Default-Werte für Prod-Secrets
  - Fallback-Werte nur für Dev/Test

- [ ] **application-dev.yaml: Test-Credentials OK**
  - H2-In-Memory oder Test-PostgreSQL
  - Test-User mit Passwort "123" (für Dev OK)
  - Dokumentieren: Dev != Prod

- [ ] **application-test.yaml: Test-Credentials OK**
  - Test-Datasource
  - Test-User für Integration-Tests

#### Secrets-Rotation
- [ ] **Rotation-Strategie dokumentieren**
  - Wie oft rotieren? (z.B. alle 90 Tage)
  - Prozess für Password-Rotation
  - Downtime vermeiden (Rolling-Update)

#### Optional: Secrets-Management-Tools
- [ ] **HashiCorp Vault Integration vorbereiten**
  - Vault-Client-Dependency
  - Spring Cloud Vault Config
  - Dokumentation: Vault-Setup
  - Noch nicht aktivieren (opt-in für später)

---

### 3. Backup Encryption

- [ ] **Backup-Encryption-Strategie**
  - GPG oder OpenSSL für Verschlüsselung
  - Public-Key-Encryption
  - Key-Management dokumentieren

- [ ] **Backup-Storage Security**
  - Backups auf separatem Storage
  - Encrypted-at-Rest
  - Access-Control für Backups

---

## 📁 Betroffene Dateien

### Zu modifizieren
- `src/main/resources/application.yaml` - DB-Config
- `src/main/resources/application-prod.yaml` - Secrets → Env-Vars
- `src/main/resources/application-dev.yaml` - Dev-Config dokumentieren
- `docker-compose.production.yml` - PostgreSQL SSL

### Zu erstellen
- `.env.example` - Template für Prod-Environment
- `docs/security/DATABASE_SECURITY.md` - DB-Security-Guide
- `docs/security/SECRETS_MANAGEMENT.md` - Secrets-Management-Guide
- `docs/security/BACKUP_ENCRYPTION.md` - Backup-Encryption-Guide
- `scripts/deployment/generate-db-certs.sh` - PostgreSQL SSL-Zertifikate (optional)

---

## 🧪 Testing

### Lokales Testing
- [ ] PostgreSQL SSL-Connection funktioniert
- [ ] Connection-Pooling konfiguriert
- [ ] Environment-Variablen werden geladen
- [ ] Keine hardcodierten Secrets in Logs

### Integration Tests
- [ ] Datasource mit Env-Vars funktioniert
- [ ] Connection-Pool-Limits werden eingehalten
- [ ] SSL-Connection erfolgreich

### Security Tests
- [ ] Secrets nicht in Git-Historie
- [ ] Secrets nicht in Logs
- [ ] PostgreSQL nicht von extern erreichbar

---

## 🔗 Abhängigkeiten

### Code-Abhängigkeiten
- **Agent 1:** PostgreSQL in docker-compose.production.yml (von Agent 1 erstellt)

### Dependencies (build.gradle)
```gradle
// PostgreSQL Driver (bereits vorhanden)
runtimeOnly 'org.postgresql:postgresql'

// Optional: HashiCorp Vault (später)
// implementation 'org.springframework.cloud:spring-cloud-starter-vault-config'
```

---

## 📚 Dokumentation

### docs/security/DATABASE_SECURITY.md
- PostgreSQL SSL-Setup
- Connection-Pooling-Konfiguration
- Database-User-Permissions
- Network-Security
- Monitoring

### docs/security/SECRETS_MANAGEMENT.md
- Environment-Variablen-Guide
- .env.example-Usage
- Secrets-Rotation-Prozess
- Best Practices
- HashiCorp Vault (optional)

### docs/security/BACKUP_ENCRYPTION.md
- Backup-Encryption-Strategie
- GPG-Setup
- Key-Management
- Restore-Prozess

---

## 🎓 Hilfreiche Ressourcen

- [PostgreSQL SSL Docs](https://www.postgresql.org/docs/current/ssl-tcp.html)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [HashiCorp Vault](https://www.vaultproject.io/)
- [12-Factor App: Config](https://12factor.net/config)

---

## ✅ Definition of Done

- [ ] PostgreSQL SSL-Connection funktioniert
- [ ] Connection-Pooling konfiguriert
- [ ] Alle Secrets via Environment-Variablen
- [ ] Keine hardcodierten Credentials in Code
- [ ] `.env.example` erstellt
- [ ] Prod-Config lädt Env-Vars korrekt
- [ ] Dev/Test-Config dokumentiert
- [ ] PostgreSQL nur intern erreichbar
- [ ] Backup-Encryption-Strategie dokumentiert
- [ ] Alle Tests grün
- [ ] Dokumentation vollständig
- [ ] Build erfolgreich
- [ ] Keine Linter-Errors

---

## 🚨 Wichtige Hinweise

1. **Environment-Variablen in Docker:** Via `.env`-File oder `docker-compose` `environment:`
2. **PostgreSQL SSL:** Self-Signed Certs für Dev, Let's Encrypt für Prod (optional)
3. **Secrets in Logs:** Logger konfigurieren, um Secrets nicht zu loggen
4. **Git-Secrets:** `.env` in `.gitignore`, nur `.env.example` committen
5. **Dev vs. Prod:** Test-Credentials in Dev OK, aber klar dokumentieren
6. **Application.yaml:** Default-Werte mit `${VAR:default}` nur für Dev, nicht für Prod-Secrets

---

## 🤝 Koordination mit anderen Agenten

### Agent 1 (Infrastructure)
- PostgreSQL SSL-Config in docker-compose.production.yml
- Network-Security für DB

### Agent 3 (MFA)
- Keine direkten Code-Konflikte
- User Entity wird von Agent 3 modifiziert, aber keine DB-Connection-Konflikte

### Agent 5 (Monitoring)
- Connection-Pool-Metrics werden hier definiert
- Logging von Secrets verhindern (hier)

---

## 🔍 Code-Review-Checkliste

### Secrets-Review
- [ ] Alle `password`, `passwd`, `pwd` in Code prüfen
- [ ] Alle `apiKey`, `api_key`, `secret`, `token` prüfen
- [ ] Alle `jdbc:` Connection-Strings prüfen
- [ ] `.env` in `.gitignore`
- [ ] Keine Private Keys in Code
- [ ] Keine AWS/GCP/Azure Keys

### Environment-Profile-Review
- [ ] Prod-Profile: Nur `${ENV_VAR}`, keine Defaults
- [ ] Dev-Profile: Test-Credentials dokumentiert
- [ ] Test-Profile: Test-Credentials dokumentiert

---

**Erstellt:** 2025-10-30  
**Status:** 🟡 Waiting for Agent 1  
**Nächster Agent:** Agent 3 + Agent 5 (nach Agent 2 UND Agent 4)

---

## 🔗 Nach Abschluss: Koordiniere mit Agent 2

Wenn du ALLE Tasks abgeschlossen hast:
- Warte bis Agent 2 (Spring Security) auch fertig ist
- Dann können Agent 3 und Agent 5 starten
- Siehe docs/security/agents/AGENT_CHAIN.md für Details
