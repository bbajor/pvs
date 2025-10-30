# Agent-Handoff-Instructions

**Ziel:** Neue Agents für die Security-Hardening-Tasks briefen

---

## 📋 Übersicht

Das Production Security Hardening Projekt ist in **5 parallele Agents** aufgeteilt. Dieser Guide erklärt, wie du einen neuen Agent für einen Task briefst.

---

## 🎯 Agent-Übersicht

| Agent | Branch | Priorität | Start | Dauer |
|-------|--------|-----------|-------|-------|
| Agent 1 | `feature/security-infrastructure` | 🔴 Highest | Sofort | 2-3 Tage |
| Agent 2 | `feature/security-spring-headers` | 🔴 High | Nach Agent 1 | 2-3 Tage |
| Agent 3 | `feature/security-auth-mfa` | 🟡 Medium-High | Nach Agent 2 | 3-4 Tage |
| Agent 4 | `feature/security-database-secrets` | 🟡 Medium | Nach Agent 1 | 2 Tage |
| Agent 5 | `feature/security-monitoring-backup` | 🟢 Medium-Low | Parallel | 2-3 Tage |

---

## 📝 Handoff-Templates

### Agent 1: Infrastructure & Reverse Proxy

**Branch:** `feature/security-infrastructure`

**Prompt:**
```
Hallo Agent 1! 👋

Ich brief dich für den ersten Teil des Production Security Hardening Projekts: Infrastructure & Reverse Proxy.

# Dein Auftrag
Du bist verantwortlich für:
- Reverse-Proxy Setup (Traefik oder Nginx)
- HTTPS/TLS mit Let's Encrypt
- Container Security (Non-Root User, Image-Scanning)
- Docker Network Security

# Wichtige Dokumente
- Master-Plan: `docs/security/PRODUCTION_SECURITY_HARDENING_MASTER_PLAN.md`
- Dein Task-File: `docs/security/agents/AGENT_1_INFRASTRUCTURE.md`
- Security-Übersicht: `docs/security/README.md`

# Dein Branch
- Erstelle Branch: `feature/security-infrastructure`
- Base-Branch: `main` (oder `feature/production-security-hardening` falls bereits erstellt)

# Wichtige Hinweise
- Du legst die Basis für alle anderen Agents
- Keine Code-Abhängigkeiten (kannst sofort starten)
- Traefik bevorzugt (einfacher mit Docker als Nginx)
- Let's Encrypt: STAGING-Modus für Tests wegen Rate-Limits!
- Dokumentation ist Pflicht (siehe Task-File)

# Definition of Done
- Traefik läuft als Reverse-Proxy
- HTTPS mit Let's Encrypt funktioniert
- Container laufen als Non-Root
- Trivy-Scans integriert
- Tests grün, Dokumentation vollständig

Viel Erfolg! 🚀
```

---

### Agent 2: Spring Security Headers & OWASP

**Branch:** `feature/security-spring-headers`

**Prompt:**
```
Hallo Agent 2! 👋

Ich brief dich für den zweiten Teil des Production Security Hardening Projekts: Spring Security Headers & OWASP.

# Dein Auftrag
Du bist verantwortlich für:
- Security Headers (CSP, X-Frame-Options, HSTS, etc.)
- Spring Security Base Configuration
- OWASP Top 10 Compliance
- CSRF Protection (Vaadin-kompatibel)

# Wichtige Dokumente
- Master-Plan: `docs/security/PRODUCTION_SECURITY_HARDENING_MASTER_PLAN.md`
- Dein Task-File: `docs/security/agents/AGENT_2_SPRING_SECURITY.md`
- Security-Übersicht: `docs/security/README.md`

# Dein Branch
- Erstelle Branch: `feature/security-spring-headers`
- Base-Branch: `feature/security-infrastructure` (nach Agent 1 gemergt)

# Abhängigkeiten
- Warte auf Agent 1 (Traefik + Forward-Headers)
- SecurityFilterChain wird von Agent 3 erweitert (MFA-Filter)

# Wichtige Hinweise
- Vaadin benötigt spezielle CSP-Regeln für inline-styles
- CSRF + Vaadin: Vaadin hat eigenen CSRF-Schutz, Koordination notwendig
- Forward-Headers-Strategy benötigt Traefik X-Forwarded-* Headers
- Security-Headers-Score: A+ anstreben (securityheaders.com)

# Definition of Done
- Alle Security-Headers gesetzt
- CSP funktioniert mit Vaadin
- CSRF-Protection aktiv
- OWASP Top 10 Compliance dokumentiert
- Tests grün, Dokumentation vollständig

Viel Erfolg! 🚀
```

---

### Agent 3: MFA & Rate Limiting

**Branch:** `feature/security-auth-mfa`

**Prompt:**
```
Hallo Agent 3! 👋

Ich brief dich für den dritten Teil des Production Security Hardening Projekts: Multi-Factor Authentication & Rate Limiting.

# Dein Auftrag
Du bist verantwortlich für:
- Multi-Factor Authentication (TOTP mit QR-Code)
- Rate Limiting & Brute-Force Protection
- MFA UI Components (Vaadin)
- Security Event Logging

# Wichtige Dokumente
- Master-Plan: `docs/security/PRODUCTION_SECURITY_HARDENING_MASTER_PLAN.md`
- Dein Task-File: `docs/security/agents/AGENT_3_MFA_RATE_LIMITING.md`
- Security-Übersicht: `docs/security/README.md`

# Dein Branch
- Erstelle Branch: `feature/security-auth-mfa`
- Base-Branch: `feature/security-spring-headers` (nach Agent 2 gemergt)

# Abhängigkeiten
- Warte auf Agent 2 (SecurityFilterChain wird hier erweitert)
- User Entity muss bereits existieren (modifizieren: MFA-Felder hinzufügen)

# Wichtige Hinweise
- TOTP Time-Drift: 30s Window + 1 Period Drift erlauben (total 60s)
- Backup-Codes: Nur einmal verwendbar, dann deaktivieren
- TOTP-Secret verschlüsselt speichern (z.B. via AES)
- Rate-Limiting: Dev-Environment großzügige Limits für Testing
- QR-Code nur einmal anzeigen, dann löschen (Security)

# Definition of Done
- MFA-Setup funktioniert (QR-Code, Verification)
- Login mit MFA erfolgreich
- Rate-Limiting greift bei Überschreitung
- Account-Lockout nach Failed-Logins
- Tests grün, Dokumentation vollständig

Viel Erfolg! 🚀
```

---

### Agent 4: Database Security & Secrets Management

**Branch:** `feature/security-database-secrets`

**Prompt:**
```
Hallo Agent 4! 👋

Ich brief dich für den vierten Teil des Production Security Hardening Projekts: Database Security & Secrets Management.

# Dein Auftrag
Du bist verantwortlich für:
- PostgreSQL SSL/TLS Connection
- Connection-Pooling (HikariCP)
- Secrets Management (Environment-Variablen)
- Keine hardcodierten Credentials im Code

# Wichtige Dokumente
- Master-Plan: `docs/security/PRODUCTION_SECURITY_HARDENING_MASTER_PLAN.md`
- Dein Task-File: `docs/security/agents/AGENT_4_DATABASE_SECRETS.md`
- Security-Übersicht: `docs/security/README.md`

# Dein Branch
- Erstelle Branch: `feature/security-database-secrets`
- Base-Branch: `feature/security-infrastructure` (nach Agent 1 gemergt)

# Abhängigkeiten
- Warte auf Agent 1 (PostgreSQL in docker-compose.production.yml)
- Parallel zu Agent 2/3 möglich

# Wichtige Hinweise
- Code-Review: Suche nach hardcodierten Credentials
- Dev/Test: Test-Credentials erlaubt (dokumentieren)
- Prod: NULL-Toleranz für hardcodierte Secrets
- .env in .gitignore, nur .env.example committen
- PostgreSQL nur intern erreichbar (keine Public-Ports)

# Definition of Done
- PostgreSQL SSL-Connection funktioniert
- Alle Secrets via Environment-Variablen
- Keine hardcodierten Credentials in Code
- .env.example erstellt
- Tests grün, Dokumentation vollständig

Viel Erfolg! 🚀
```

---

### Agent 5: Logging, Monitoring & Backup

**Branch:** `feature/security-monitoring-backup`

**Prompt:**
```
Hallo Agent 5! 👋

Ich brief dich für den fünften Teil des Production Security Hardening Projekts: Logging, Monitoring & Backup.

# Dein Auftrag
Du bist verantwortlich für:
- Structured Logging (JSON-Format)
- Security Event Logging
- Monitoring (Prometheus + Grafana)
- Backup & Disaster Recovery

# Wichtige Dokumente
- Master-Plan: `docs/security/PRODUCTION_SECURITY_HARDENING_MASTER_PLAN.md`
- Dein Task-File: `docs/security/agents/AGENT_5_MONITORING_BACKUP.md`
- Security-Übersicht: `docs/security/README.md`

# Dein Branch
- Erstelle Branch: `feature/security-monitoring-backup`
- Base-Branch: `feature/security-infrastructure` (nach Agent 1 gemergt)

# Abhängigkeiten
- Parallel zu Agent 2/3/4 möglich
- Security-Events von Agent 3 (MFA, Rate-Limiting)

# Wichtige Hinweise
- Secrets in Logs: Niemals Passwörter, Tokens, API-Keys loggen!
- PII in Logs: Personenbezogene Daten nur gehashed oder pseudonymisiert
- Backup-Encryption: Backups IMMER verschlüsseln (DSGVO!)
- Backup-Testing: Regelmäßig Test-Restores durchführen
- Prometheus Retention: Nicht zu lange (max. 15-30 Tage)

# Definition of Done
- Structured Logging (JSON) funktioniert in Prod
- Security-Events werden geloggt
- Prometheus Metrics verfügbar
- Grafana-Dashboard läuft
- Backup-Script funktioniert (inkl. Encryption)
- Tests grün, Dokumentation vollständig

Viel Erfolg! 🚀
```

---

## 🔄 Merge-Reihenfolge

### Phase 1: Infrastructure (Week 1)
1. **Agent 1** → `feature/production-security-hardening`
2. Code-Review & Testing
3. Merge erfolgreich

### Phase 2: Spring Security & Database (Week 2)
4. **Agent 2** → `feature/production-security-hardening`
5. **Agent 4** → `feature/production-security-hardening`
6. Code-Review & Testing
7. Merge erfolgreich

### Phase 3: Authentication & Monitoring (Week 2-3)
8. **Agent 3** → `feature/production-security-hardening`
9. **Agent 5** → `feature/production-security-hardening`
10. Code-Review & Testing
11. Merge erfolgreich

### Phase 4: Integration Testing (Week 3)
12. End-to-End Security Tests
13. Penetration Testing (OWASP ZAP)
14. Documentation Review

### Phase 5: Production Deployment (Week 4)
15. `feature/production-security-hardening` → `main`
16. Production Deployment
17. Post-Deployment Monitoring

---

## 🚨 Bei Problemen

### Merge-Konflikte
- Konflikt-Matrix im Master-Plan prüfen
- Mit koordinierendem Agent (Agent 1) abstimmen
- Dokumentation im Master-Plan aktualisieren

### API-Änderungen
- Änderung im Master-Plan dokumentieren
- Betroffene Agenten benachrichtigen

### Unklare Anforderungen
- Task-File im Detail lesen
- Master-Plan konsultieren
- OWASP/Spring Security Docs prüfen

---

**Erstellt:** 2025-10-30  
**Version:** 1.0
