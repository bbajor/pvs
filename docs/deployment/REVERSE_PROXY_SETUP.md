# Reverse Proxy Setup (Traefik)

**Production-Ready Reverse-Proxy mit Traefik v3**

---

## 🎯 Überblick

Traefik v3 dient als Reverse-Proxy vor der PVS-Applikation und bietet:
- ✅ Automatische HTTPS mit Let's Encrypt
- ✅ Security Headers (HSTS, CSP, X-Frame-Options, etc.)
- ✅ Rate Limiting & DDoS-Schutz
- ✅ HTTP → HTTPS Redirect
- ✅ TLS 1.2+ Enforcement
- ✅ Load Balancing & Health Checks
- ✅ Prometheus Metrics

---

## 📦 Architektur

```
Internet
    ↓
Traefik (Port 80/443)
    ↓ (HTTP → HTTPS Redirect)
    ↓ (Security Middlewares)
    ↓
PVS App (Port 8080, intern)
```

---

## 🚀 Quick Start

### 1. Environment-Variablen setzen

Erstelle `.env` aus `.env.production.example`:

```bash
cp .env.production.example .env
```

Setze mindestens:
```env
DOMAIN=deine-domain.com
LETSENCRYPT_EMAIL=admin@deine-domain.com
```

### 2. Traefik starten

```bash
# Production
docker-compose -f docker-compose.production.yml --profile prod up -d traefik

# Logs verfolgen
docker logs -f pvs-traefik
```

### 3. PVS-App starten

```bash
# Production
docker-compose -f docker-compose.production.yml --profile prod up -d pvs-prod
```

### 4. Testen

```bash
# HTTPS funktioniert?
curl -I https://deine-domain.com

# Security Headers prüfen
curl -I https://deine-domain.com | grep -E "Strict-Transport-Security|Content-Security-Policy|X-Frame-Options"
```

---

## 🔧 Konfiguration

### Traefik Hauptkonfiguration

Datei: `docker/traefik/traefik.yml`

Wichtigste Einstellungen:
- **Entry Points:** Port 80 (HTTP) und 443 (HTTPS)
- **HTTP → HTTPS Redirect:** Automatisch
- **Let's Encrypt:** TLS-ALPN-01 Challenge
- **TLS Options:** TLS 1.2+ min, preferiert TLS 1.3
- **Logging:** JSON-Format für strukturiertes Logging
- **Metrics:** Prometheus auf Port 8082

### Security Middlewares

Datei: `docker/traefik/dynamic/middlewares.yml`

**Security Headers:**
- `Strict-Transport-Security`: 1 Jahr, includeSubdomains, preload
- `Content-Security-Policy`: Vaadin-kompatible CSP
- `X-Frame-Options`: DENY
- `X-Content-Type-Options`: nosniff
- `Referrer-Policy`: strict-origin-when-cross-origin
- `Permissions-Policy`: Camera, Microphone, Geolocation deaktiviert

**Rate Limiting:**
- Standard: 100 req/s, Burst 50
- Auth-Endpoints: 5 req/min, Burst 10

### Routing-Regeln

Datei: `docker/traefik/dynamic/routers.yml`

- **Traefik Dashboard:** `traefik.deine-domain.com` (Basic Auth)
- **PVS Production:** `deine-domain.com`
- **PVS Dev:** `dev.deine-domain.com`
- **PVS Test:** `test.deine-domain.com`

---

## 🔒 Security Features

### TLS/SSL

- **TLS 1.2+** (preferiert TLS 1.3)
- **Perfect Forward Secrecy** (ECDHE)
- **Modern Cipher Suites**
- **Automatische Zertifikats-Erneuerung** (Let's Encrypt)
- **HSTS Headers** (1 Jahr)

### Security Headers

Alle Responses enthalten:
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
Content-Security-Policy: default-src 'self'; ...
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
```

### Rate Limiting

- **Standard:** 100 req/s pro IP
- **Auth-Endpoints:** 5 req/min pro IP
- **Burst-Limit:** 50 Requests

---

## 🧪 Testing

### Lokales Testing (ohne Let's Encrypt)

Für lokale Tests mit Self-Signed Certificates:

1. Erstelle Self-Signed Cert:
```bash
mkdir -p docker/traefik/certs
openssl req -x509 -newkey rsa:4096 -keyout docker/traefik/certs/key.pem \
  -out docker/traefik/certs/cert.pem -days 365 -nodes \
  -subj "/CN=localhost"
```

2. Passe `traefik.yml` an:
```yaml
# Kommentiere Let's Encrypt aus, aktiviere File-Provider
tls:
  certificates:
    - certFile: /certs/cert.pem
      keyFile: /certs/key.pem
```

3. Starte Traefik:
```bash
docker-compose up -d traefik
```

### SSL Labs Test

Teste deine TLS-Konfiguration:

```bash
# Online-Test
# https://www.ssllabs.com/ssltest/analyze.html?d=deine-domain.com

# Erwartetes Rating: A+
```

### Security Headers Test

```bash
# Online-Test
# https://securityheaders.com/?q=https://deine-domain.com

# Erwartetes Rating: A+
```

### Rate Limiting Test

```bash
# Teste Rate Limit
for i in {1..150}; do
  curl -s -o /dev/null -w "%{http_code}\n" https://deine-domain.com
done

# Erwartete Response: HTTP 429 (Too Many Requests) nach ~100 Requests
```

---

## 📊 Monitoring

### Traefik Dashboard

URL: `https://traefik.deine-domain.com`

**Zugang:**
- Username: `admin`
- Password: Siehe `.env` oder `docker/traefik/dynamic/middlewares.yml`

**Features:**
- Aktive Routers & Services
- Health-Checks-Status
- TLS-Zertifikate
- Middlewares

### Prometheus Metrics

URL: `https://metrics.deine-domain.com`

**Metrics:**
- `traefik_entrypoint_requests_total`
- `traefik_entrypoint_request_duration_seconds`
- `traefik_service_requests_total`
- `traefik_service_request_duration_seconds`

### Logs

```bash
# Traefik Logs
docker logs -f pvs-traefik

# Access Logs (JSON-Format)
docker exec pvs-traefik cat /var/log/traefik/access.log

# Error Logs
docker exec pvs-traefik cat /var/log/traefik/traefik.log
```

---

## 🔧 Troubleshooting

### Problem: Let's Encrypt Rate Limit erreicht

**Symptom:** Fehler "too many certificates already issued"

**Lösung:**
1. Nutze Staging-Modus für Tests:
```yaml
# traefik.yml
certificatesResolvers:
  letsencrypt:
    acme:
      caServer: https://acme-staging-v02.api.letsencrypt.org/directory
```

2. Nach erfolgreichen Tests: Zurück zu Production
3. Rate Limits: 50 Certs/Woche pro Domain

### Problem: DNS nicht propagiert

**Symptom:** Let's Encrypt Challenge fails

**Lösung:**
1. Prüfe DNS-Propagation:
```bash
dig +short deine-domain.com
nslookup deine-domain.com
```

2. Warte 5-60 Minuten für DNS-Propagation
3. Nutze HTTP-Challenge statt TLS-Challenge (optional)

### Problem: 502 Bad Gateway

**Symptom:** Traefik zeigt 502 Error

**Lösung:**
1. Prüfe ob PVS-App läuft:
```bash
docker ps | grep pvs-prod
docker logs pvs-prod
```

2. Prüfe Health-Check:
```bash
curl http://localhost:8080/actuator/health
```

3. Prüfe Network:
```bash
docker network inspect pvs_pvs-network
```

### Problem: Security Headers fehlen

**Symptom:** Headers nicht in Response

**Lösung:**
1. Prüfe Middleware-Config:
```bash
docker exec pvs-traefik cat /etc/traefik/dynamic/middlewares.yml
```

2. Prüfe Router-Labels:
```bash
docker inspect pvs-prod | grep traefik.http.routers
```

3. Restart Traefik:
```bash
docker-compose restart traefik
```

---

## 🚀 Production Deployment

### Pre-Deployment Checklist

- [ ] Domain DNS auf Server-IP konfiguriert
- [ ] `.env` mit Production-Werten
- [ ] Traefik Dashboard-Password geändert
- [ ] Firewall-Regeln (Port 80, 443 offen)
- [ ] Backup-Strategy vorhanden

### Deployment

```bash
# 1. Environment-Variablen prüfen
cat .env

# 2. Traefik starten
docker-compose -f docker-compose.production.yml --profile prod up -d traefik

# 3. Let's Encrypt Zertifikat warten (ca. 30-60 Sekunden)
docker logs -f pvs-traefik

# 4. PVS-App starten
docker-compose -f docker-compose.production.yml --profile prod up -d pvs-prod

# 5. Testen
curl -I https://deine-domain.com
```

### Post-Deployment Validation

```bash
# SSL Labs Test
# https://www.ssllabs.com/ssltest/analyze.html?d=deine-domain.com

# Security Headers Test
# https://securityheaders.com/?q=https://deine-domain.com

# Health-Check
curl https://deine-domain.com/actuator/health

# Rate Limiting
for i in {1..150}; do curl -s -o /dev/null -w "%{http_code}\n" https://deine-domain.com; done
```

---

## 📚 Weiterführende Docs

- [TLS Setup](../security/TLS_SETUP.md) - Detaillierte TLS-Konfiguration
- [Container Security](../security/CONTAINER_SECURITY.md) - Docker Security Best Practices
- [Security Headers](../security/SECURITY_HEADERS.md) - OWASP Top 10 Compliance

---

**Erstellt:** 2025-10-30  
**Version:** 1.0  
**Status:** ✅ Production-Ready
