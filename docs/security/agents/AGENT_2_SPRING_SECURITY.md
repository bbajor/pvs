# Agent 2: Spring Security Headers & OWASP

**Branch:** `feature/security-spring-headers`  
**Priorität:** 🔴 High  
**Start:** Nach Agent 1 gemergt  
**Geschätzte Dauer:** 2-3 Tage  

---

## 🎯 Mission

Spring Security Core-Konfiguration, Security Headers, OWASP Top 10 Compliance.

---

## 📋 Tasks

### 1. Security Headers Implementation

- [ ] **Content-Security-Policy (CSP)**
  - CSP-Header konfigurieren
  - Vaadin-spezifische CSP-Regeln
  - `script-src`, `style-src`, `img-src`, etc.
  - Inline-Scripts hashen (wenn nötig)
  - CSP-Report-URI (optional)

- [ ] **X-Frame-Options**
  - `DENY` für Clickjacking-Schutz
  - Alternative: `frame-ancestors` in CSP

- [ ] **X-Content-Type-Options**
  - `nosniff` setzen
  - MIME-Sniffing verhindern

- [ ] **Referrer-Policy**
  - `strict-origin-when-cross-origin` oder `no-referrer`
  - Privacy-Protection

- [ ] **Permissions-Policy**
  - Ungenutzte Browser-Features deaktivieren
  - `camera=(), microphone=(), geolocation=()`, etc.

- [ ] **HSTS (ergänzend zu Traefik)**
  - In Spring Boot konfigurieren als Fallback
  - Koordination mit Traefik-HSTS

---

### 2. Spring Security Base Configuration

- [ ] **SecurityFilterChain erstellen**
  - Modern Spring Security (6.x) Konfiguration
  - Lambda-DSL verwenden
  - Keine deprecated Methods

- [ ] **CSRF Protection**
  - CSRF-Protection aktivieren
  - Vaadin-Kompatibilität sicherstellen
  - `CsrfRequestDataValueProcessor` für Vaadin
  - CSRF-Token in Forms

- [ ] **Secure Cookie Flags**
  - `HttpOnly` Flag
  - `Secure` Flag (nur HTTPS)
  - `SameSite=Strict` oder `Lax`
  - Cookie-Security in application.yaml

- [ ] **Session Management**
  - Session-Fixation Protection
  - Session-Timeout konfigurieren
  - Concurrent Session Control
  - Session-Cookie-Name anpassen

- [ ] **Actuator Security**
  - Actuator-Endpoints absichern
  - Health-Check public
  - Metrics nur für Admins
  - Management-Port separieren (optional)

---

### 3. OWASP Top 10 Compliance

- [ ] **A01: Broken Access Control**
  - Spring Security Authorization prüfen
  - `@PreAuthorize` auf sensiblen Methoden
  - Role-based Access Control testen

- [ ] **A02: Cryptographic Failures**
  - BCryptPasswordEncoder für Passwörter (bereits vorhanden?)
  - HTTPS erzwingen
  - Sensible Daten nicht loggen

- [ ] **A03: Injection (SQL)**
  - JPA/Hibernate verwendet (OK)
  - Keine nativen Queries mit String-Concatenation
  - Code-Review für SQL-Injection-Risiken

- [ ] **A04: Insecure Design**
  - Security-By-Design Review
  - Threat Modeling (dokumentieren)

- [ ] **A05: Security Misconfiguration**
  - Default-Credentials entfernen
  - Error-Handling ohne sensible Infos
  - Debug-Mode in Prod deaktiviert

- [ ] **A06: Vulnerable Components**
  - Dependency-Check (OWASP Dependency Check)
  - Outdated Dependencies updaten
  - CVE-Scanning

- [ ] **A07: Authentication Failures**
  - Password-Policies erzwingen
  - Account-Lockout (wird von Agent 3 implementiert)
  - Multi-Factor Auth (wird von Agent 3 implementiert)

- [ ] **A08: Software and Data Integrity**
  - Docker-Image-Signierung (Agent 1)
  - CI/CD-Pipeline sichern

- [ ] **A09: Security Logging Failures**
  - Security-Events loggen (wird von Agent 5 implementiert)
  - Login/Logout loggen
  - Failed Access Attempts loggen

- [ ] **A10: Server-Side Request Forgery (SSRF)**
  - URL-Validation implementieren
  - Whitelist für externe Requests
  - Internal-IPs blocken

---

### 4. Spring Boot Security Config

- [ ] **Forward Headers Strategy**
  - `server.forward-headers-strategy=framework` setzen
  - X-Forwarded-* Headers verarbeiten
  - Koordination mit Traefik

- [ ] **Error Handling**
  - Custom Error-Pages ohne sensible Infos
  - Stack-Traces in Prod deaktiviert
  - Error-Logging ohne Credentials

- [ ] **Security Properties**
  - application-prod.yaml Security-Config
  - Environment-spezifische Settings
  - Dev vs. Prod Unterschiede dokumentieren

---

## 📁 Betroffene Dateien

### Zu erstellen
- `src/main/java/de/bbajor/pvs/security/SecurityHeadersConfiguration.java`
- `src/main/java/de/bbajor/pvs/security/WebSecurityConfig.java`
- `src/main/java/de/bbajor/pvs/security/filter/SecurityHeadersFilter.java`
- `src/main/java/de/bbajor/pvs/security/CustomAuthenticationEntryPoint.java`
- `src/test/java/de/bbajor/pvs/security/SecurityHeadersTest.java`
- `src/test/java/de/bbajor/pvs/security/WebSecurityConfigTest.java`
- `docs/security/SECURITY_HEADERS.md`
- `docs/security/OWASP_COMPLIANCE.md`

### Zu modifizieren
- `src/main/resources/application.yaml` (Security-Properties)
- `src/main/resources/application-prod.yaml` (Prod Security-Config)
- `build.gradle` (Security-Dependencies)

---

## 🧪 Testing

### Unit Tests
- [ ] SecurityHeadersFilter setzt alle Header korrekt
- [ ] CSRF-Protection funktioniert
- [ ] Session-Management testet Fixation-Protection
- [ ] Cookie-Flags korrekt gesetzt

### Integration Tests
- [ ] Security-Headers in HTTP-Response vorhanden
- [ ] CSP-Header validieren
- [ ] CSRF-Token in Forms
- [ ] Login-Flow mit Security-Config

### Security Tests
- [ ] OWASP ZAP Scan (Basis-Scan)
- [ ] CSP-Validator (online Tools)
- [ ] Security-Headers-Check (securityheaders.com)

---

## 🔗 Abhängigkeiten

### Code-Abhängigkeiten
- **Agent 1:** Forward-Headers-Strategy benötigt Traefik-Setup

### Dependencies (build.gradle)
```gradle
// Spring Security (bereits vorhanden?)
implementation 'org.springframework.boot:spring-boot-starter-security'

// OWASP Dependency Check (optional)
// Plugin: org.owasp.dependencycheck
```

---

## 📚 Dokumentation

### docs/security/SECURITY_HEADERS.md
- Alle Security-Headers erklärt
- Konfiguration Schritt-für-Schritt
- CSP-Policy für Vaadin
- Testing-Tools
- Troubleshooting

### docs/security/OWASP_COMPLIANCE.md
- OWASP Top 10 Checklist
- Compliance-Status pro Item
- Mitigations implementiert
- Noch offene Punkte

---

## 🎓 Hilfreiche Ressourcen

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)
- [Security Headers](https://securityheaders.com/) - Testing-Tool
- [CSP Evaluator](https://csp-evaluator.withgoogle.com/)
- [Vaadin Security Best Practices](https://vaadin.com/docs/latest/security/overview)

---

## ✅ Definition of Done

- [ ] Alle Security-Headers korrekt gesetzt
- [ ] CSP funktioniert mit Vaadin
- [ ] CSRF-Protection aktiv und getestet
- [ ] OWASP Top 10 Compliance dokumentiert
- [ ] Secure Cookie Flags gesetzt
- [ ] Actuator-Endpoints abgesichert
- [ ] Alle Tests grün (Unit + Integration)
- [ ] Security-Headers-Score: A+ (securityheaders.com)
- [ ] Dokumentation vollständig
- [ ] Build erfolgreich
- [ ] Keine Linter-Errors

---

## 🚨 Wichtige Hinweise

1. **Vaadin CSP:** Vaadin benötigt spezielle CSP-Regeln für inline-styles
2. **CSRF + Vaadin:** Vaadin hat eigenen CSRF-Schutz, Koordination notwendig
3. **Forward Headers:** Ohne Traefik Forward-Headers funktionieren X-Forwarded-* nicht
4. **Actuator Security:** Health-Check muss public sein (für Load Balancer)
5. **Testing:** Security-Headers nur in HTTPS-Responses testen

---

## 🤝 Koordination mit anderen Agenten

### Agent 1 (Infrastructure)
- HSTS-Header in Traefik UND Spring Boot (doppelte Absicherung OK)
- Forward-Headers-Strategy benötigt Traefik X-Forwarded-* Headers

### Agent 3 (MFA)
- SecurityFilterChain wird von Agent 3 erweitert (MFA-Filter)
- Basis-Config hier, MFA-spezifisches dort

### Agent 5 (Monitoring)
- Security-Events hier definieren, Logging dort implementieren

---

**Erstellt:** 2025-10-30  
**Status:** 🟡 Waiting for Agent 1  
**Nächster Agent:** Agent 3 + Agent 5 (nach Agent 2 UND Agent 4)

---

## 🔗 Nach Abschluss: Nächste Agents starten

Wenn du ALLE Tasks abgeschlossen hast UND Agent 4 auch fertig ist:

### Starte Agent 3 (MFA & Rate Limiting)
```
@cursor Hallo Agent 3! 👋

Ich brief dich für Production Security Hardening - Teil 3: Multi-Factor Authentication & Rate Limiting.

# Deine Aufgabe
Lies das Task-File: docs/security/agents/AGENT_3_MFA_RATE_LIMITING.md

Arbeite ALLE Tasks in diesem File ab.

# Branch-Setup
1. Erstelle Branch: feature/security-auth-mfa (von current main mit Agent 1+2+4)
2. Arbeite an deinen Tasks
3. Teste alles lokal

# Nach Abschluss
Siehe docs/security/agents/AGENT_CHAIN.md für nächste Schritte.

Viel Erfolg! 🚀
```

### Starte Agent 5 (Monitoring) - PARALLEL
```
@cursor Hallo Agent 5! 👋

Ich brief dich für Production Security Hardening - Teil 5: Logging, Monitoring & Backup.

# Deine Aufgabe
Lies das Task-File: docs/security/agents/AGENT_5_MONITORING_BACKUP.md

Arbeite ALLE Tasks in diesem File ab.

# Branch-Setup
1. Erstelle Branch: feature/security-monitoring-backup (von current main mit Agent 1+2+4)
2. Arbeite an deinen Tasks
3. Teste alles lokal

# Nach Abschluss
Siehe docs/security/agents/AGENT_CHAIN.md für nächste Schritte.

Viel Erfolg! 🚀
```
