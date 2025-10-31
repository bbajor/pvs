# Agent 1: Infrastructure & Reverse Proxy

**Branch:** `feature/security-infrastructure`  
**Priorität:** 🔴 Highest  
**Start:** Sofort möglich  
**Geschätzte Dauer:** 2-3 Tage  

---

## 🎯 Mission

Aufbau der Infrastruktur-Basis für Production Security: Reverse-Proxy, TLS/HTTPS, Container Security.

---

## 📋 Tasks

### 1. Reverse-Proxy Setup (Traefik)

#### Warum Traefik?
- Native Docker-Integration
- Automatische Let's Encrypt Integration
- Einfache Konfiguration via Labels
- Dashboard für Monitoring

#### Implementierung
- [ ] **Traefik Docker-Compose Service erstellen**
  - Traefik v3.x verwenden
  - Ports 80, 443, 8080 (Dashboard) konfigurieren
  - Volumes für Zertifikate und Config
  - Labels für Auto-Discovery

- [ ] **Routing-Regeln für pvs-app**
  - Upstream auf `pvs-app:8080`
  - Host-basiertes Routing
  - Path-basiertes Routing (optional)
  - Load-Balancing-Strategie

- [ ] **Health-Check-Endpoints**
  - Health-Check für pvs-app einrichten
  - Traefik Health-Check konfigurieren
  - Failover-Strategie

- [ ] **Logging**
  - Access-Logs für alle Requests
  - Error-Logs für Traefik
  - JSON-Format für strukturiertes Logging

- [ ] **Geo-IP-Filtering (optional)**
  - Middleware für Geo-Blocking
  - Whitelist/Blacklist konfigurieren

---

### 2. HTTPS/TLS Verschlüsselung

- [ ] **Let's Encrypt Integration**
  - ACME-Provider konfigurieren (DNS-01 oder HTTP-01)
  - Email für Zertifikats-Benachrichtigungen
  - Staging-Modus für Tests
  - Production-Modus nach Validierung

- [ ] **Automatische Zertifikats-Erneuerung**
  - Traefik Auto-Renewal aktivieren
  - Zertifikats-Storage (Volume oder File)
  - Renewal-Hooks (optional)

- [ ] **HTTP → HTTPS Redirect**
  - Redirect-Middleware
  - Permanent Redirect (301)

- [ ] **HSTS Headers**
  - HSTS-Middleware im Traefik
  - max-age=31536000 (1 Jahr)
  - includeSubDomains
  - preload (optional)

- [ ] **TLS-Konfiguration**
  - TLS 1.2+ erzwingen (preferiert 1.3)
  - Perfect Forward Secrecy
  - Modern Cipher Suites
  - SSL Labs A+ Rating anstreben

---

### 3. Container Security

- [ ] **Non-Root User in Dockerfiles**
  - `Dockerfile` für pvs-app anpassen
  - User `appuser` mit UID 1000 erstellen
  - Permissions für /app anpassen
  - ENTRYPOINT als Non-Root ausführen

- [ ] **Multi-Stage Builds optimieren**
  - Bestehende Multi-Stage Builds prüfen
  - Nur notwendige Artefakte in Final-Stage
  - Build-Tools nicht in Final-Stage

- [ ] **Minimal Base Images**
  - Alpine oder Distroless evaluieren
  - Aktuelles Eclipse-Temurin JRE prüfen
  - Image-Größe reduzieren
  - Security-Patches aktuell halten

- [ ] **Security Scanning**
  - Trivy in CI/CD Pipeline integrieren
  - Vulnerability-Scans bei jedem Build
  - Critical/High Vulnerabilities blocken
  - Scan-Reports generieren

- [ ] **Image-Versionierung**
  - Semantic Versioning für Images
  - Git-Commit-Hash als Tag
  - `latest` nur für Prod-Releases

---

### 4. Docker Network Security

- [ ] **Interne Netzwerke**
  - Separates Netzwerk für DB
  - Separates Netzwerk für Monitoring
  - pvs-app nur in notwendigen Netzwerken

- [ ] **Nur notwendige Ports exponieren**
  - Port 80/443 nur über Traefik
  - pvs-app:8080 nur intern
  - PostgreSQL nur intern
  - Whisper nur intern

- [ ] **Network Policies dokumentieren**
  - Welcher Service kommuniziert mit wem
  - Firewall-Regeln dokumentieren

---

## 📁 Betroffene Dateien

### Zu erstellen
- `docker/traefik/traefik.yml` - Traefik Hauptkonfiguration
- `docker/traefik/dynamic-config.yml` - Dynamische Routing-Regeln
- `docker-compose.production.yml` - Production Docker-Compose mit Traefik
- `docs/deployment/REVERSE_PROXY_SETUP.md` - Setup-Dokumentation
- `docs/security/TLS_SETUP.md` - TLS-Konfiguration und Let's Encrypt
- `docs/security/CONTAINER_SECURITY.md` - Container Security Best Practices

### Zu modifizieren
- `Dockerfile` - Non-Root User, optimiertes Image
- `docker-compose.yml` - Traefik-Integration (optional für Dev)
- `.gitignore` - Traefik-Zertifikate und Logs ignorieren

---

## 🧪 Testing

### Lokales Testing (mit Self-Signed Certs)
- [ ] Traefik startet ohne Fehler
- [ ] HTTP → HTTPS Redirect funktioniert
- [ ] Self-Signed Cert wird akzeptiert
- [ ] pvs-app erreichbar über Traefik

### Staging (mit Let's Encrypt Staging)
- [ ] Let's Encrypt Staging-Zertifikat wird generiert
- [ ] HTTPS funktioniert mit Staging-Cert
- [ ] Auto-Renewal wird getestet (manuell Cert löschen)

### Production
- [ ] Let's Encrypt Production-Zertifikat wird generiert
- [ ] HTTPS funktioniert
- [ ] SSL Labs Test: A+ Rating
- [ ] Security Headers korrekt gesetzt (via Traefik)

---

## 🔗 Abhängigkeiten

- **Keine Code-Abhängigkeiten** (kann sofort starten)
- **Domain erforderlich:** Für Let's Encrypt muss eine Domain konfiguriert sein
- **DNS muss auf Server zeigen:** A-Record für Domain

---

## 📚 Dokumentation

### docs/deployment/REVERSE_PROXY_SETUP.md
- Traefik-Installation
- Konfiguration Schritt-für-Schritt
- Troubleshooting
- Monitoring-Dashboard

### docs/security/TLS_SETUP.md
- Let's Encrypt Setup
- DNS vs. HTTP Challenge
- Zertifikats-Erneuerung
- TLS-Konfiguration
- Troubleshooting

### docs/security/CONTAINER_SECURITY.md
- Non-Root User
- Image-Scanning mit Trivy
- Best Practices
- Security-Checkliste

---

## 🎓 Hilfreiche Ressourcen

- [Traefik v3 Docs](https://doc.traefik.io/traefik/)
- [Let's Encrypt Docs](https://letsencrypt.org/docs/)
- [SSL Labs Test](https://www.ssllabs.com/ssltest/)
- [Trivy Scanner](https://trivy.dev/)
- [Docker Security Best Practices](https://docs.docker.com/develop/security-best-practices/)

---

## ✅ Definition of Done

- [ ] Traefik läuft als Reverse-Proxy
- [ ] HTTPS mit Let's Encrypt funktioniert
- [ ] HTTP → HTTPS Redirect aktiv
- [ ] HSTS Headers gesetzt
- [ ] SSL Labs Rating: A+
- [ ] Container laufen als Non-Root
- [ ] Trivy-Scans integriert
- [ ] Alle Tests grün
- [ ] Dokumentation vollständig
- [ ] Build erfolgreich (`./gradlew build`)
- [ ] Keine Linter-Errors

---

## 🚨 Wichtige Hinweise

1. **Let's Encrypt Rate Limits:** Staging-Modus für Tests verwenden!
2. **Domain erforderlich:** Ohne Domain keine Let's Encrypt-Zerts
3. **DNS Propagation:** A-Record muss propagiert sein (ca. 5-60 Minuten)
4. **Security:** Traefik-Dashboard nur intern oder mit Auth exponieren
5. **Zertifikats-Storage:** Volume für persistente Zertifikate verwenden

---

**Erstellt:** 2025-10-30  
**Status:** 🟢 Ready to Start  
**Nächster Agent:** Agent 2 + Agent 4 (parallel starten)

---

## 🔗 Nach Abschluss: Nächste Agents starten

Wenn du ALLE Tasks abgeschlossen hast, deinen Branch gemergt hast und alles funktioniert:

### Starte Agent 2 (Spring Security)
```
@cursor Hallo Agent 2! 👋

Ich brief dich für Production Security Hardening - Teil 2: Spring Security Headers & OWASP.

# Deine Aufgabe
Lies das Task-File: docs/security/agents/AGENT_2_SPRING_SECURITY.md

Arbeite ALLE Tasks in diesem File ab.

# Branch-Setup
1. Erstelle Branch: feature/security-spring-headers (von current main mit Agent 1)
2. Arbeite an deinen Tasks
3. Teste alles lokal

# Nach Abschluss
Siehe docs/security/agents/AGENT_CHAIN.md für nächste Schritte.

Viel Erfolg! 🚀
```

### Starte Agent 4 (Database Security) - PARALLEL
```
@cursor Hallo Agent 4! 👋

Ich brief dich für Production Security Hardening - Teil 4: Database Security & Secrets Management.

# Deine Aufgabe
Lies das Task-File: docs/security/agents/AGENT_4_DATABASE_SECRETS.md

Arbeite ALLE Tasks in diesem File ab.

# Branch-Setup
1. Erstelle Branch: feature/security-database-secrets (von current main mit Agent 1)
2. Arbeite an deinen Tasks
3. Teste alles lokal

# Nach Abschluss
Siehe docs/security/agents/AGENT_CHAIN.md für nächste Schritte.

Viel Erfolg! 🚀
```
