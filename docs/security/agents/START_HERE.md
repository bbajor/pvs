# 🚀 START HERE - Production Security Hardening

**Los geht's mit Agent 1!**

---

## 📋 Quick Start

### Schritt 1: Brief Agent 1

Kopiere den folgenden Prompt und sende ihn an @cursor:

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
   - Agent 2 (siehe docs/security/agents/AGENT_1_INFRASTRUCTURE.md am Ende)
   - Agent 4 (siehe docs/security/agents/AGENT_1_INFRASTRUCTURE.md am Ende)

Viel Erfolg! 🚀
```

---

## 📚 Wichtige Dokumente

### Für alle Agents
- **[Master-Plan](../PRODUCTION_SECURITY_HARDENING_MASTER_PLAN.md)** - Gesamtübersicht
- **[Agent Chain](./AGENT_CHAIN.md)** - Wer startet wen?
- **[Security README](../README.md)** - Security-Übersicht

### Agent-spezifische Task-Files
1. [Agent 1: Infrastructure](./AGENT_1_INFRASTRUCTURE.md) - **START HIER!**
2. [Agent 2: Spring Security](./AGENT_2_SPRING_SECURITY.md)
3. [Agent 3: MFA & Rate Limiting](./AGENT_3_MFA_RATE_LIMITING.md)
4. [Agent 4: Database & Secrets](./AGENT_4_DATABASE_SECRETS.md)
5. [Agent 5: Monitoring & Backup](./AGENT_5_MONITORING_BACKUP.md)

---

## 🔗 Agent-Kette

```
Agent 1 (Infrastructure) ← START HIER
    ↓
Agent 2 (Spring Security) + Agent 4 (Database) [parallel]
    ↓
Agent 3 (MFA) + Agent 5 (Monitoring) [parallel]
    ↓
Integration Testing
    ↓
Production Deployment
```

---

## ⏱️ Zeitplan

| Phase | Agent(s) | Dauer | Start |
|-------|----------|-------|-------|
| Phase 1 | Agent 1 | 2-3 Tage | Sofort |
| Phase 2 | Agent 2 + Agent 4 | 2-3 Tage | Nach Agent 1 |
| Phase 3 | Agent 3 + Agent 5 | 3-4 Tage | Nach Phase 2 |
| Phase 4 | Integration Testing | 2 Tage | Nach Phase 3 |

**Gesamt:** ca. 2-3 Wochen

---

## ✅ Erfolgs-Kriterien

### Pro Agent
- ✅ Alle Tasks abgeschlossen
- ✅ Build erfolgreich
- ✅ Tests grün
- ✅ Dokumentation vollständig
- ✅ Branch gemergt
- ✅ Nächster Agent gebrieft

### Gesamtprojekt
- ✅ Alle 5 Agents fertig
- ✅ Integration-Tests erfolgreich
- ✅ Security-Audit bestanden
- ✅ Production-ready

---

## 🚨 Wichtige Regeln

1. **IMMER das Task-File lesen** vor dem Start
2. **IMMER lokal testen** bevor mergen
3. **IMMER Dokumentation erstellen**
4. **IMMER den nächsten Agent briefen** nach Abschluss

---

## 📊 Progress-Tracking

Du kannst den Status im [Agent Chain Dokument](./AGENT_CHAIN.md) tracken.

---

**LOS GEHT'S! 🚀**

Kopiere den Prompt oben und sende ihn an @cursor um Agent 1 zu starten!
