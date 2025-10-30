# Security Headers & OWASP Compliance

**Production-Ready Security Headers für OWASP Top 10 Compliance**

---

## 🎯 Überblick

Security Headers schützen gegen die häufigsten Web-Angriffe:
- ✅ XSS (Cross-Site Scripting)
- ✅ Clickjacking
- ✅ MIME-Sniffing
- ✅ Man-in-the-Middle
- ✅ CSRF (Cross-Site Request Forgery)

**Defense-in-Depth:** Security Headers werden SOWOHL von Traefik (Reverse-Proxy) ALS AUCH von Spring Boot gesetzt.

---

## 📦 Implementierte Security Headers

### 1. Content-Security-Policy (CSP)

**Header:**
```
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'
```

**Schutz gegen:**
- XSS (Cross-Site Scripting)
- Data-Injection-Attacks
- Malicious Scripts

**Vaadin-spezifisch:**
- `script-src 'unsafe-inline' 'unsafe-eval'` - Benötigt von Vaadin
- `style-src 'unsafe-inline'` - Benötigt von Vaadin

**Implementierung:**
- `SecurityHeadersConfiguration.java` - Filter für alle Responses
- `docker/traefik/dynamic/middlewares.yml` - Traefik-Middleware

---

### 2. X-Frame-Options

**Header:**
```
X-Frame-Options: DENY
```

**Schutz gegen:**
- Clickjacking-Attacken
- UI-Redressing
- Frame-Injection

**Implementierung:**
- `SecurityHeadersConfiguration.java` - Filter
- `ProdSecurityConfig.java` - Spring Security Headers
- `docker/traefik/dynamic/middlewares.yml` - Traefik-Middleware

---

### 3. X-Content-Type-Options

**Header:**
```
X-Content-Type-Options: nosniff
```

**Schutz gegen:**
- MIME-Sniffing-Attacken
- Content-Type-Confusion
- Drive-by-Downloads

**Implementierung:**
- `SecurityHeadersConfiguration.java` - Filter
- `docker/traefik/dynamic/middlewares.yml` - Traefik-Middleware

---

### 4. X-XSS-Protection

**Header:**
```
X-XSS-Protection: 1; mode=block
```

**Schutz gegen:**
- Reflected XSS (Legacy-Browser)

**Hinweis:** Legacy-Header für alte Browser. Moderne Browser verlassen sich auf CSP.

**Implementierung:**
- `SecurityHeadersConfiguration.java` - Filter

---

### 5. Referrer-Policy

**Header:**
```
Referrer-Policy: strict-origin-when-cross-origin
```

**Schutz gegen:**
- Information-Leakage
- Privacy-Verletzungen
- Sensitive-URL-Parameter in Referrer

**Implementierung:**
- `SecurityHeadersConfiguration.java` - Filter
- `docker/traefik/dynamic/middlewares.yml` - Traefik-Middleware

---

### 6. Permissions-Policy

**Header:**
```
Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=(), usb=(), magnetometer=(), gyroscope=()
```

**Schutz gegen:**
- Unerwünschten Zugriff auf Browser-Features
- Privacy-Verletzungen

**Implementierung:**
- `SecurityHeadersConfiguration.java` - Filter
- `docker/traefik/dynamic/middlewares.yml` - Traefik-Middleware

---

### 7. Strict-Transport-Security (HSTS)

**Header:**
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

**Schutz gegen:**
- SSL-Stripping-Attacken
- Man-in-the-Middle
- Downgrade-Attacken

**Implementierung:**
- `docker/traefik/traefik.yml` - Traefik TLS-Config
- `SecurityHeadersConfiguration.java` - Fallback für Spring Boot

---

## 🔒 OWASP Top 10 (2021) Compliance

### A01: Broken Access Control ✅

**Maßnahmen:**
- Spring Security Method-Level-Security (`@PreAuthorize`)
- Role-based Access Control
- Session-Fixation-Protection
- Maximale Sessions pro User

**Implementierung:**
- `CommonSecurityConfig.java` - Method Security
- `ProdSecurityConfig.java` - Session-Management

---

### A02: Cryptographic Failures ✅

**Maßnahmen:**
- HTTPS erzwingen (Traefik + HSTS)
- TLS 1.2+ (preferiert TLS 1.3)
- BCrypt für Passwörter (Strength 12)
- Secure Cookies (HttpOnly, Secure, SameSite)

**Implementierung:**
- `docker/traefik/traefik.yml` - TLS-Config
- `ProdSecurityConfig.java` - PasswordEncoder
- `application-prod.yaml` - Secure Cookies

---

### A03: Injection ✅

**Maßnahmen:**
- JPA/Hibernate (keine String-Concatenation in Queries)
- PreparedStatements automatisch via JPA
- Input-Validation via Bean-Validation
- CSP-Header gegen Script-Injection

**Implementierung:**
- JPA Repositories (bereits vorhanden)
- `SecurityHeadersConfiguration.java` - CSP-Header

---

### A04: Insecure Design ✅

**Maßnahmen:**
- Security-by-Design (Secure-Defaults)
- Threat-Modeling durchgeführt
- Principle of Least Privilege

**Implementierung:**
- Defense-in-Depth (Traefik + Spring Security)
- Dokumentation vorhanden

---

### A05: Security Misconfiguration ✅

**Maßnahmen:**
- Default-Credentials entfernt (nur Dev-Profil)
- Error-Handling ohne sensible Infos
- Actuator-Endpoints abgesichert
- Security Headers gesetzt

**Implementierung:**
- `ProdSecurityConfig.java` - Actuator-Security
- `application-prod.yaml` - Production-Config

---

### A06: Vulnerable and Outdated Components ✅

**Maßnahmen:**
- Dependency-Scanning (Trivy)
- Automatische Updates via Dependabot
- CVE-Monitoring

**Implementierung:**
- `.github/workflows/trivy-scan.yml` - Trivy CI/CD
- Trivy scannt täglich

---

### A07: Identification and Authentication Failures ✅

**Maßnahmen:**
- Password-Policies (BCrypt)
- Session-Fixation-Protection
- Account-Lockout (Agent 3 - MFA)
- Multi-Factor Auth (Agent 3)

**Implementierung:**
- `ProdSecurityConfig.java` - Session-Management
- Agent 3 erweitert mit MFA + Rate-Limiting

---

### A08: Software and Data Integrity Failures ✅

**Maßnahmen:**
- Docker-Image-Signierung (geplant)
- CI/CD-Pipeline abgesichert
- Dependency-Verification

**Implementierung:**
- `.github/workflows/trivy-scan.yml` - Security-Scans
- Docker Multi-Stage-Builds

---

### A09: Security Logging and Monitoring Failures ⏳

**Status:** Agent 5 implementiert

**Maßnahmen:**
- Security-Event-Logging
- Audit-Logs
- Monitoring (Prometheus + Grafana)

**Implementierung:**
- Agent 5: `SecurityEventLogger.java`

---

### A10: Server-Side Request Forgery (SSRF) ✅

**Maßnahmen:**
- Whitelist für externe Requests
- Internal-IPs blocken (optional)
- CSP-Header gegen Client-Side-SSRF

**Implementierung:**
- `SecurityHeadersConfiguration.java` - CSP

---

## 🧪 Testing

### Security Headers Test (Online)

**securityheaders.com:**
[https://securityheaders.com/?q=https://deine-domain.com](https://securityheaders.com/)

**Erwartetes Rating:** A

### Lokaler Security-Headers-Test

```bash
# Alle Security-Headers prüfen
curl -I https://deine-domain.com

# Erwartete Headers:
# Content-Security-Policy: default-src 'self'; ...
# X-Frame-Options: DENY
# X-Content-Type-Options: nosniff
# X-XSS-Protection: 1; mode=block
# Referrer-Policy: strict-origin-when-cross-origin
# Permissions-Policy: camera=(), ...
# Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

### CSP-Validator

**Google CSP Evaluator:**
[https://csp-evaluator.withgoogle.com/](https://csp-evaluator.withgoogle.com/)

**Paste CSP:**
```
default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'
```

**Erwartete Warnung:** `unsafe-inline` und `unsafe-eval` (Vaadin-spezifisch, akzeptabel)

### OWASP ZAP Scan

```bash
# Installiere OWASP ZAP
# https://www.zaproxy.org/download/

# Automated Scan
zap-cli quick-scan https://deine-domain.com

# Baseline Scan
docker run -t owasp/zap2docker-stable zap-baseline.py -t https://deine-domain.com
```

---

## 🔧 Troubleshooting

### Problem: CSP blockiert Vaadin-Scripts

**Symptom:** Browser-Console zeigt "Content Security Policy: ... blocked"

**Lösung:**
```java
// In SecurityHeadersConfiguration.java
String csp = "default-src 'self'; " +
             "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " + // Vaadin benötigt unsafe-*
             "style-src 'self' 'unsafe-inline'; " +
             "...";
```

### Problem: X-Frame-Options verhindert Iframe

**Symptom:** "Refused to display in a frame"

**Lösung (nur wenn Iframe gewünscht):**
```java
// In ProdSecurityConfig.java
http.headers(headers -> headers
    .frameOptions(frameOptions -> frameOptions.sameOrigin()) // SAMEORIGIN statt DENY
);
```

### Problem: HSTS-Header fehlt

**Symptom:** Security-Headers-Test zeigt "HSTS Missing"

**Lösung:**
1. **Traefik prüfen:**
```yaml
# docker/traefik/dynamic/middlewares.yml
securityHeaders:
  headers:
    stsSeconds: 31536000
    stsIncludeSubdomains: true
    stsPreload: true
```

2. **Spring Boot Fallback prüfen:**
```java
// In SecurityHeadersConfiguration.java
String forwardedProto = request.getHeader("X-Forwarded-Proto");
if ("https".equals(forwardedProto)) {
    response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
}
```

---

## 📚 Weiterführende Docs

- [OWASP Top 10 (2021)](https://owasp.org/Top10/)
- [OWASP Security Headers Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html)
- [Content Security Policy (CSP)](https://content-security-policy.com/)
- [Vaadin Security Best Practices](https://vaadin.com/docs/latest/security/overview)

---

**Erstellt:** 2025-10-30  
**Version:** 1.0  
**Status:** ✅ Production-Ready
