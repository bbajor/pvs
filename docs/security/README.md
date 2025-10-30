# Security Documentation

**Übersicht der Sicherheits-Dokumentation für das PVS-System**

---

## 📋 Inhaltsverzeichnis

1. [Production Security Hardening](#production-security-hardening)
2. [Bestehende Security-Docs](#bestehende-security-docs)
3. [Neue Security-Features](#neue-security-features)
4. [Quick Links](#quick-links)

---

## 🔒 Production Security Hardening

**Status:** 🟢 In Planung  
**Start:** 2025-10-30  
**Priorität:** 🔴 High  

### Master Plan
- **[Production Security Hardening Master Plan](./PRODUCTION_SECURITY_HARDENING_MASTER_PLAN.md)** - Zentrale Planung und Koordination für alle Security-Härtungs-Maßnahmen

### Branch-Struktur
```
feature/production-security-hardening
  ├── feature/security-infrastructure
  ├── feature/security-spring-headers
  ├── feature/security-auth-mfa
  ├── feature/security-database-secrets
  └── feature/security-monitoring-backup
```

---

## 📚 Bestehende Security-Docs

### Passwort-Management
- [Database Password Change](./DATABASE_PASSWORD_CHANGE.md) - Prozess für Änderung des Datenbank-Passworts
- [Server Root Password Change](./SERVER_ROOT_PASSWORD_CHANGE.md) - Prozess für Änderung des Server-Root-Passworts

---

## 🆕 Neue Security-Features (in Entwicklung)

### Infrastructure Security
- **[Reverse Proxy Setup](./REVERSE_PROXY_SETUP.md)** *(geplant)* - Traefik/Nginx als Reverse-Proxy
- **[TLS Setup](./TLS_SETUP.md)** *(geplant)* - HTTPS/TLS mit Let's Encrypt
- **[Container Security](./CONTAINER_SECURITY.md)** *(geplant)* - Docker Security Best Practices

### Application Security
- **[Security Headers](./SECURITY_HEADERS.md)** *(geplant)* - OWASP Top 10 Compliance, CSP, etc.
- **[TOTP Setup](./TOTP_SETUP.md)** *(geplant)* - Multi-Factor Authentication (TOTP)
- **[Hybrid 2FA Setup](./HYBRID_2FA_SETUP.md)** *(geplant)* - Optional aktivierbare 2FA
- **[Rate Limiting](./RATE_LIMITING.md)** *(geplant)* - Brute-Force Protection

### Data Security
- **[Database Security](./DATABASE_SECURITY.md)** *(geplant)* - PostgreSQL Hardening
- **[Secrets Management](./SECRETS_MANAGEMENT.md)** *(geplant)* - Environment-Variablen, Vault
- **[Backup Encryption](./BACKUP_ENCRYPTION.md)** *(geplant)* - Verschlüsselte Backups

### Monitoring & Compliance
- **[Logging & Monitoring](./LOGGING_MONITORING.md)** *(geplant)* - Security Event Logging, Prometheus
- **[Backup & Disaster Recovery](../deployment/BACKUP_DISASTER_RECOVERY.md)** *(geplant)* - Backup-Strategie

---

## 🔗 Quick Links

### Deployment-relevante Security-Docs
- [Deployment README](../deployment/README.md)
- [Hetzner Setup](../deployment/HETZNER_SETUP.md)
- [DSGVO Compliance](../deployment/DSGVO.md)

### Externe Ressourcen
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Traefik Docs](https://doc.traefik.io/traefik/)
- [Let's Encrypt](https://letsencrypt.org/)

---

## 🎯 Security Checklist

### Production-Ready Kriterien

#### Infrastructure
- [ ] Reverse-Proxy läuft vor der Applikation
- [ ] HTTPS mit Let's Encrypt konfiguriert
- [ ] Alle Container laufen als Non-Root
- [ ] Interne Netzwerke für DB/Services
- [ ] Security-Scans für Docker-Images

#### Application
- [ ] Security-Headers gesetzt (CSP, X-Frame-Options, etc.)
- [ ] CSRF-Protection aktiv
- [ ] MFA optional aktivierbar
- [ ] Rate Limiting aktiv
- [ ] Brute-Force Protection

#### Data
- [ ] Keine hardcodierten Credentials
- [ ] PostgreSQL SSL-Verbindung
- [ ] Backup-Strategie implementiert
- [ ] Backup-Encryption aktiv

#### Monitoring
- [ ] Structured Logging (JSON)
- [ ] Security-Events werden geloggt
- [ ] Monitoring-Dashboard läuft
- [ ] Alerts bei verdächtigen Aktivitäten

#### Compliance
- [ ] DSGVO-konform
- [ ] OWASP Top 10 Compliance
- [ ] Audit-Logs für sensible Operationen
- [ ] Penetration Testing durchgeführt

---

## 📝 Notizen

- **Entwicklungsumgebung:** Test-Credentials und Sample-Users sind in dev/test-Profilen erlaubt
- **Production:** NULL-Toleranz für hardcodierte Credentials
- **Security-First:** Alle neuen Features müssen Security-Review durchlaufen

---

**Erstellt:** 2025-10-30  
**Zuletzt aktualisiert:** 2025-10-30  
**Verantwortlich:** Security Team
