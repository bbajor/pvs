# Agent Task Files

**Übersicht der Agent-spezifischen Task-Files für Production Security Hardening**

---

## 📋 Agent-Übersicht

| Agent | Branch | Priorität | Dauer | Status |
|-------|--------|-----------|-------|--------|
| [Agent 1: Infrastructure](./AGENT_1_INFRASTRUCTURE.md) | `feature/security-infrastructure` | 🔴 Highest | 2-3 Tage | ⏳ Ready |
| [Agent 2: Spring Security](./AGENT_2_SPRING_SECURITY.md) | `feature/security-spring-headers` | 🔴 High | 2-3 Tage | ⏸️ Waiting |
| [Agent 3: MFA & Rate Limiting](./AGENT_3_MFA_RATE_LIMITING.md) | `feature/security-auth-mfa` | 🟡 Medium-High | 3-4 Tage | ⏸️ Waiting |
| [Agent 4: Database & Secrets](./AGENT_4_DATABASE_SECRETS.md) | `feature/security-database-secrets` | 🟡 Medium | 2 Tage | ⏸️ Waiting |
| [Agent 5: Monitoring & Backup](./AGENT_5_MONITORING_BACKUP.md) | `feature/security-monitoring-backup` | 🟢 Medium-Low | 2-3 Tage | ⏳ Ready |

---

## 🚀 Quick Start

### 1. Lies den Master-Plan
[Production Security Hardening Master Plan](../PRODUCTION_SECURITY_HARDENING_MASTER_PLAN.md)

### 2. Wähle deinen Agent
Lies das entsprechende Agent-Task-File:
- [Agent 1: Infrastructure](./AGENT_1_INFRASTRUCTURE.md)
- [Agent 2: Spring Security](./AGENT_2_SPRING_SECURITY.md)
- [Agent 3: MFA & Rate Limiting](./AGENT_3_MFA_RATE_LIMITING.md)
- [Agent 4: Database & Secrets](./AGENT_4_DATABASE_SECRETS.md)
- [Agent 5: Monitoring & Backup](./AGENT_5_MONITORING_BACKUP.md)

### 3. Erstelle deinen Branch
```bash
git checkout -b feature/security-[dein-bereich]
```

### 4. Arbeite an deinen Tasks
Siehe Task-File für Details.

### 5. Merge in Haupt-Feature-Branch
```bash
git checkout feature/production-security-hardening
git merge feature/security-[dein-bereich]
```

---

## 📚 Handoff-Instructions

Siehe [Agent Handoff Instructions](./AGENT_HANDOFF_INSTRUCTIONS.md) für vollständige Briefing-Templates.

---

## 🔄 Dependencies

### Merge-Reihenfolge
```
Agent 1 (Infrastructure)
  ↓
Agent 2 (Spring Security) + Agent 4 (Database)
  ↓
Agent 3 (MFA) + Agent 5 (Monitoring)
  ↓
Integration Testing
  ↓
Production Deployment
```

### Code-Dependencies
- **Agent 2** benötigt **Agent 1** (Traefik Forward-Headers)
- **Agent 3** benötigt **Agent 2** (SecurityFilterChain)
- **Agent 4** benötigt **Agent 1** (PostgreSQL in Docker-Compose)
- **Agent 5** benötigt **Agent 3** (Security-Events)

---

## ✅ Definition of Done (alle Agents)

### Pro Agent
- [ ] Alle Tasks aus Task-File abgeschlossen
- [ ] Tests grün (Unit + Integration)
- [ ] Dokumentation vollständig
- [ ] Build erfolgreich (`./gradlew build`)
- [ ] Keine Linter-Errors
- [ ] Code-Review durchgeführt

### Gesamtprojekt
- [ ] Alle Agents gemergt
- [ ] Integration-Tests grün
- [ ] End-to-End Security-Tests erfolgreich
- [ ] Penetration-Testing durchgeführt
- [ ] Dokumentation vollständig und konsistent
- [ ] Production-Deployment erfolgreich

---

## 🚨 Wichtige Hinweise

### Merge-Konflikte vermeiden
- Jeder Agent arbeitet in seinem eigenen Bereich
- Konflikt-Matrix im Master-Plan beachten
- Bei Unsicherheit: Mit koordinierendem Agent (Agent 1) abstimmen

### Kommunikation
- Änderungen an Shared-Files dokumentieren
- API-Änderungen im Master-Plan dokumentieren
- Betroffene Agents benachrichtigen

### Testing
- Lokal testen BEVOR mergen
- Build muss grün sein
- Security-Tests durchführen

---

**Erstellt:** 2025-10-30  
**Version:** 1.0
