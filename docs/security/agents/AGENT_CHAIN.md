# Agent Chain - Automatische Agent-Übergabe

**Ziel:** Jeder Agent startet automatisch den nächsten Agent nach Abschluss seiner Tasks.

---

## 🔗 Agent-Chain-Struktur

```
Agent 1 (Infrastructure)
    ↓ (nach Abschluss & Merge)
Agent 2 (Spring Security) + Agent 4 (Database) [parallel]
    ↓ (nach Abschluss & Merge)
Agent 3 (MFA) + Agent 5 (Monitoring) [parallel]
    ↓ (nach Abschluss & Merge)
Integration Testing
    ↓
Production Deployment
```

---

## 📋 Agent-Start-Kommandos

### Agent 1 Start (JETZT)

**Branch:** `feature/security-infrastructure`

**Start-Command für @cursor:**
```
@cursor Hallo Agent 1! 👋

Ich brief dich für Production Security Hardening - Teil 1: Infrastructure & Reverse Proxy.

# Deine Aufgabe
Lies das Task-File: docs/security/agents/AGENT_1_INFRASTRUCTURE.md

Arbeite ALLE Tasks in diesem File ab:
✅ Reverse-Proxy Setup (Traefik)
✅ HTTPS/TLS mit Let's Encrypt
✅ Container Security (Non-Root, Image-Scanning)
✅ Docker Network Security

# Branch-Setup
1. Erstelle Branch: feature/security-infrastructure (von main)
2. Arbeite an deinen Tasks
3. Committe regelmäßig
4. Teste alles lokal (Build, Tests, Docker-Compose)

# Nach Abschluss
Wenn alle Tasks erledigt sind:
1. Stelle sicher: Build erfolgreich, Tests grün, Dokumentation vollständig
2. Merge deinen Branch in main (oder feature/production-security-hardening)
3. Starte die NÄCHSTEN AGENTS (parallel):
   - Agent 2 (siehe docs/security/agents/AGENT_CHAIN.md)
   - Agent 4 (siehe docs/security/agents/AGENT_CHAIN.md)

Viel Erfolg! 🚀
```

---

### Agent 2 Start (nach Agent 1)

**Branch:** `feature/security-spring-headers`

**Start-Command für @cursor:**
```
@cursor Hallo Agent 2! 👋

Ich brief dich für Production Security Hardening - Teil 2: Spring Security Headers & OWASP.

# Deine Aufgabe
Lies das Task-File: docs/security/agents/AGENT_2_SPRING_SECURITY.md

Arbeite ALLE Tasks in diesem File ab:
✅ Security Headers (CSP, X-Frame-Options, HSTS)
✅ Spring Security Base Configuration
✅ OWASP Top 10 Compliance
✅ CSRF Protection (Vaadin-kompatibel)

# Branch-Setup
1. Erstelle Branch: feature/security-spring-headers (von current main mit Agent 1)
2. Arbeite an deinen Tasks
3. Committe regelmäßig
4. Teste alles lokal (Build, Tests, Security-Headers)

# Nach Abschluss
Wenn alle Tasks erledigt sind:
1. Stelle sicher: Build erfolgreich, Tests grün, Dokumentation vollständig
2. Merge deinen Branch in main (oder feature/production-security-hardening)
3. Warte bis Agent 4 auch fertig ist
4. Dann starte die NÄCHSTEN AGENTS (parallel):
   - Agent 3 (siehe docs/security/agents/AGENT_CHAIN.md)
   - Agent 5 (siehe docs/security/agents/AGENT_CHAIN.md)

Viel Erfolg! 🚀
```

---

### Agent 3 Start (nach Agent 2)

**Branch:** `feature/security-auth-mfa`

**Start-Command für @cursor:**
```
@cursor Hallo Agent 3! 👋

Ich brief dich für Production Security Hardening - Teil 3: Multi-Factor Authentication & Rate Limiting.

# Deine Aufgabe
Lies das Task-File: docs/security/agents/AGENT_3_MFA_RATE_LIMITING.md

Arbeite ALLE Tasks in diesem File ab:
✅ Multi-Factor Authentication (TOTP mit QR-Code)
✅ MFA UI Components (Vaadin)
✅ Rate Limiting & Brute-Force Protection
✅ Security Event Logging

# Branch-Setup
1. Erstelle Branch: feature/security-auth-mfa (von current main mit Agent 1+2)
2. Arbeite an deinen Tasks
3. Committe regelmäßig
4. Teste alles lokal (Build, Tests, MFA-Flow)

# Nach Abschluss
Wenn alle Tasks erledigt sind:
1. Stelle sicher: Build erfolgreich, Tests grün, Dokumentation vollständig
2. Merge deinen Branch in main (oder feature/production-security-hardening)
3. Warte bis ALLE anderen Agents fertig sind (2, 4, 5)
4. Dann starte Integration Testing (siehe docs/security/agents/AGENT_CHAIN.md)

Viel Erfolg! 🚀
```

---

### Agent 4 Start (nach Agent 1, parallel zu Agent 2)

**Branch:** `feature/security-database-secrets`

**Start-Command für @cursor:**
```
@cursor Hallo Agent 4! 👋

Ich brief dich für Production Security Hardening - Teil 4: Database Security & Secrets Management.

# Deine Aufgabe
Lies das Task-File: docs/security/agents/AGENT_4_DATABASE_SECRETS.md

Arbeite ALLE Tasks in diesem File ab:
✅ PostgreSQL SSL/TLS Connection
✅ Connection-Pooling (HikariCP)
✅ Secrets Management (Environment-Variablen)
✅ Backup Encryption

# Branch-Setup
1. Erstelle Branch: feature/security-database-secrets (von current main mit Agent 1)
2. Arbeite an deinen Tasks
3. Committe regelmäßig
4. Teste alles lokal (Build, Tests, DB-Connection)

# Nach Abschluss
Wenn alle Tasks erledigt sind:
1. Stelle sicher: Build erfolgreich, Tests grün, Dokumentation vollständig
2. Merge deinen Branch in main (oder feature/production-security-hardening)
3. Warte bis Agent 2 auch fertig ist
4. Dann können Agent 3 und Agent 5 starten (siehe docs/security/agents/AGENT_CHAIN.md)

Viel Erfolg! 🚀
```

---

### Agent 5 Start (nach Agent 2/4, parallel zu Agent 3)

**Branch:** `feature/security-monitoring-backup`

**Start-Command für @cursor:**
```
@cursor Hallo Agent 5! 👋

Ich brief dich für Production Security Hardening - Teil 5: Logging, Monitoring & Backup.

# Deine Aufgabe
Lies das Task-File: docs/security/agents/AGENT_5_MONITORING_BACKUP.md

Arbeite ALLE Tasks in diesem File ab:
✅ Structured Logging (JSON-Format)
✅ Security Event Logging
✅ Monitoring (Prometheus + Grafana)
✅ Backup & Disaster Recovery

# Branch-Setup
1. Erstelle Branch: feature/security-monitoring-backup (von current main mit Agent 1+2+4)
2. Arbeite an deinen Tasks
3. Committe regelmäßig
4. Teste alles lokal (Build, Tests, Logging, Backup-Scripts)

# Nach Abschluss
Wenn alle Tasks erledigt sind:
1. Stelle sicher: Build erfolgreich, Tests grün, Dokumentation vollständig
2. Merge deinen Branch in main (oder feature/production-security-hardening)
3. Warte bis ALLE anderen Agents fertig sind (2, 3, 4)
4. Dann starte Integration Testing (siehe docs/security/agents/AGENT_CHAIN.md)

Viel Erfolg! 🚀
```

---

## 🧪 Integration Testing (nach allen Agents)

**Start-Command für @cursor:**
```
@cursor Hallo Integration-Tester! 👋

Alle Security-Hardening-Agents haben ihre Arbeit abgeschlossen. Zeit für Integration Testing!

# Deine Aufgabe
1. Lies: docs/security/PRODUCTION_SECURITY_HARDENING_MASTER_PLAN.md (Abschnitt "Integration Testing")

# Tests durchführen
✅ End-to-End Security Tests
  - HTTPS-Verbindung erfolgreich
  - Security-Headers alle gesetzt
  - Login mit MFA funktioniert
  - Rate Limiting greift
  - Logging funktioniert
  - Monitoring-Dashboard zeigt Daten

✅ Penetration Testing
  - OWASP ZAP Scan
  - SQL-Injection Tests
  - XSS Tests
  - CSRF Tests
  - Brute-Force Tests

✅ Performance Testing
  - Load-Testing mit Rate-Limiting
  - MFA-Flow unter Last
  - Backup-Restore-Zeit

# Nach Abschluss
Wenn alle Tests erfolgreich:
1. Dokumentation final reviewen
2. Production-Deployment vorbereiten
3. Go-Live-Checkliste abarbeiten

Viel Erfolg! 🚀
```

---

## 📊 Progress-Tracking

### Status-Übersicht

| Agent | Branch | Status | Merged? | Next Action |
|-------|--------|--------|---------|-------------|
| Agent 1 | `feature/security-infrastructure` | ⏳ Starting | ❌ | Start Now |
| Agent 2 | `feature/security-spring-headers` | ⏸️ Waiting | ❌ | Wait Agent 1 |
| Agent 3 | `feature/security-auth-mfa` | ⏸️ Waiting | ❌ | Wait Agent 2 |
| Agent 4 | `feature/security-database-secrets` | ⏸️ Waiting | ❌ | Wait Agent 1 |
| Agent 5 | `feature/security-monitoring-backup` | ⏸️ Waiting | ❌ | Wait Agent 2+4 |
| Integration | - | ⏸️ Waiting | ❌ | Wait All |

---

## 🚨 Wichtige Regeln

### Für jeden Agent

1. **IMMER das Task-File lesen** (`docs/security/agents/AGENT_X_*.md`)
2. **IMMER lokal testen** bevor mergen (Build, Tests, Linter)
3. **IMMER Dokumentation erstellen** (siehe Task-File)
4. **IMMER den nächsten Agent briefen** nach Abschluss

### Bei Problemen

- **Merge-Konflikte:** Konflikt-Matrix im Master-Plan prüfen
- **Unklare Anforderungen:** Task-File + Master-Plan + OWASP-Docs konsultieren
- **API-Änderungen:** Im Master-Plan dokumentieren

---

## 🎯 Erfolgs-Kriterien

### Pro Agent
- ✅ Alle Tasks abgeschlossen
- ✅ Build erfolgreich
- ✅ Tests grün
- ✅ Dokumentation vollständig
- ✅ Branch gemergt

### Gesamtprojekt
- ✅ Alle 5 Agents fertig
- ✅ Integration-Tests erfolgreich
- ✅ Security-Audit bestanden
- ✅ Production-ready

---

**Erstellt:** 2025-10-30  
**Version:** 1.0  
**Status:** 🟢 Ready to Start
