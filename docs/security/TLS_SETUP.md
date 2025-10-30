# TLS/SSL Setup mit Let's Encrypt

**Automatische HTTPS-Verschlüsselung für Production**

---

## 🎯 Überblick

Let's Encrypt bietet kostenlose SSL/TLS-Zertifikate mit automatischer Erneuerung. Traefik managed den kompletten Lifecycle:
- ✅ Automatische Zertifikats-Anforderung
- ✅ Automatische Erneuerung (90 Tage Gültigkeit)
- ✅ TLS 1.2+ Enforcement (preferiert TLS 1.3)
- ✅ Perfect Forward Secrecy
- ✅ Modern Cipher Suites
- ✅ HSTS Headers (1 Jahr)

---

## 🚀 Quick Start

### 1. Domain konfigurieren

**DNS A-Record:**
```
deine-domain.com     A    123.45.67.89 (deine Server-IP)
*.deine-domain.com   A    123.45.67.89 (Wildcard, optional)
```

**DNS-Propagation prüfen:**
```bash
dig +short deine-domain.com
# Sollte deine Server-IP zurückgeben

nslookup deine-domain.com
# Sollte deine Server-IP zeigen
```

### 2. Environment-Variablen setzen

`.env`:
```env
DOMAIN=deine-domain.com
LETSENCRYPT_EMAIL=admin@deine-domain.com
```

### 3. Traefik starten

```bash
docker-compose -f docker-compose.production.yml --profile prod up -d traefik

# Logs verfolgen (Zertifikats-Anforderung)
docker logs -f pvs-traefik
```

### 4. Zertifikat validieren

```bash
# Warte 30-60 Sekunden für Zertifikats-Anforderung

# Prüfe Zertifikat
openssl s_client -connect deine-domain.com:443 -servername deine-domain.com < /dev/null 2>/dev/null | openssl x509 -noout -dates

# Ausgabe sollte zeigen:
# notBefore=... notAfter=... (90 Tage Gültigkeit)
```

---

## 🔧 TLS-Konfiguration

### TLS Options (traefik.yml)

```yaml
tls:
  options:
    default:
      minVersion: VersionTLS12  # TLS 1.2 minimum
      preferServerCipherSuites: true
      cipherSuites:
        # TLS 1.3 (automatisch, sicherste Option)
        # TLS 1.2 (sicher):
        - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
        - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
        - TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305
      curvePreferences:
        - CurveP521
        - CurveP384
      sniStrict: true
```

**Erklärung:**
- `minVersion: VersionTLS12`: Nur TLS 1.2 und 1.3 erlaubt
- `preferServerCipherSuites: true`: Server wählt sicherste Cipher Suite
- `cipherSuites`: Modern, sichere Cipher Suites (ECDHE für PFS)
- `curvePreferences`: Elliptic Curves für ECDHE
- `sniStrict: true`: SNI erforderlich (verhindert Cert-Leaks)

### Let's Encrypt Challenge

**TLS-ALPN-01 Challenge (Standard, empfohlen):**
```yaml
certificatesResolvers:
  letsencrypt:
    acme:
      email: ${LETSENCRYPT_EMAIL}
      storage: /letsencrypt/acme.json
      tlsChallenge: {}
```

**Vorteile:**
- Kein Port 80 erforderlich
- Funktioniert hinter Firewall
- Schnellere Validierung

**HTTP-01 Challenge (Fallback):**
```yaml
certificatesResolvers:
  letsencrypt:
    acme:
      email: ${LETSENCRYPT_EMAIL}
      storage: /letsencrypt/acme.json
      httpChallenge:
        entryPoint: web
```

**Vorteile:**
- Funktioniert immer
- Einfacher zu debuggen

---

## 🔒 HSTS Configuration

### HSTS Headers (via Traefik Middleware)

`docker/traefik/dynamic/middlewares.yml`:
```yaml
http:
  middlewares:
    securityHeaders:
      headers:
        stsSeconds: 31536000        # 1 Jahr
        stsIncludeSubdomains: true
        stsPreload: true
        forceSTSHeader: true
```

**Erklärung:**
- `stsSeconds: 31536000`: 1 Jahr (empfohlen für Production)
- `stsIncludeSubdomains: true`: Gilt auch für Subdomains
- `stsPreload: true`: Für HSTS Preload List (optional)
- `forceSTSHeader: true`: Immer senden

### HSTS Preload List (optional)

Für maximale Sicherheit: [https://hstspreload.org/](https://hstspreload.org/)

**Anforderungen:**
- HSTS Header mit min. 1 Jahr
- `includeSubDomains` aktiv
- `preload` Flag

**Warnung:** Einmal in Preload List = schwer zu entfernen!

---

## 🧪 Testing

### SSL Labs Test

**Online-Test:**
[https://www.ssllabs.com/ssltest/](https://www.ssllabs.com/ssltest/)

**Erwartetes Rating:** A+

**Kriterien:**
- Certificate: Valid
- Protocol Support: TLS 1.2, TLS 1.3
- Key Exchange: ECDHE (Perfect Forward Secrecy)
- Cipher Strength: 256 bits
- HSTS: Yes

### Lokaler TLS-Test

```bash
# TLS-Version prüfen
openssl s_client -connect deine-domain.com:443 -tls1_2 < /dev/null
# Sollte funktionieren

openssl s_client -connect deine-domain.com:443 -tls1_1 < /dev/null
# Sollte FEHLSCHLAGEN (TLS 1.1 nicht erlaubt)

# Cipher Suites testen
nmap --script ssl-enum-ciphers -p 443 deine-domain.com
# Sollte nur sichere Cipher Suites zeigen
```

### HSTS-Test

```bash
# HSTS Header prüfen
curl -I https://deine-domain.com | grep Strict-Transport-Security

# Erwartete Ausgabe:
# Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

### Certificate Details

```bash
# Zertifikats-Details anzeigen
echo | openssl s_client -connect deine-domain.com:443 -servername deine-domain.com 2>/dev/null | openssl x509 -noout -text

# Issuer: Let's Encrypt
# Validity: 90 Tage
# Subject Alternative Names: deine-domain.com, www.deine-domain.com
```

---

## 🔄 Zertifikats-Erneuerung

### Automatische Erneuerung

Let's Encrypt-Zertifikate sind 90 Tage gültig. Traefik erneuert automatisch ab 30 Tage vor Ablauf.

**Kein manueller Eingriff nötig!**

### Manuelle Erneuerung (für Testing)

```bash
# Altes Zertifikat löschen
docker exec pvs-traefik rm /letsencrypt/acme.json

# Traefik neustarten (fordert neues Zertifikat an)
docker restart pvs-traefik

# Logs verfolgen
docker logs -f pvs-traefik
```

### Erneuerungs-Status prüfen

```bash
# Zertifikats-Gültigkeit prüfen
openssl s_client -connect deine-domain.com:443 -servername deine-domain.com < /dev/null 2>/dev/null | openssl x509 -noout -dates

# Ausgabe:
# notBefore=Oct 30 12:00:00 2025 GMT
# notAfter=Jan 28 12:00:00 2026 GMT

# Verbleibende Tage berechnen
openssl s_client -connect deine-domain.com:443 -servername deine-domain.com < /dev/null 2>/dev/null | openssl x509 -noout -enddate | cut -d= -f2 | xargs -I {} date -d {} +%s | xargs -I {} expr \( {} - $(date +%s) \) / 86400

# Erwartete Ausgabe: 60-90 Tage
```

---

## 🚨 Troubleshooting

### Problem: Let's Encrypt Rate Limit

**Symptom:** Error "too many certificates already issued"

**Lösung:**
1. **Staging-Modus verwenden** (unbegrenzte Rate):
```yaml
# traefik.yml
certificatesResolvers:
  letsencrypt:
    acme:
      caServer: https://acme-staging-v02.api.letsencrypt.org/directory
      email: ${LETSENCRYPT_EMAIL}
      storage: /letsencrypt/acme.json
      tlsChallenge: {}
```

2. Nach erfolgreichen Tests: Zurück zu Production
3. **Rate Limits:**
   - 50 Certificates/Woche pro Domain
   - 5 Duplicate Certificates/Woche
   - 300 Certificates/Woche pro Account

### Problem: DNS nicht propagiert

**Symptom:** Challenge fails mit "DNS resolution error"

**Lösung:**
1. **DNS-Propagation prüfen:**
```bash
dig +short deine-domain.com
nslookup deine-domain.com
```

2. **Warten:** 5-60 Minuten für DNS-Propagation
3. **DNS-Propagation-Tool:** [https://dnschecker.org/](https://dnschecker.org/)

### Problem: Challenge fails (Firewall)

**Symptom:** "Connection refused" oder "Timeout"

**Lösung:**
1. **Firewall-Regeln prüfen:**
```bash
# Port 80 (HTTP Challenge) oder 443 (TLS Challenge) offen?
sudo ufw status
sudo iptables -L -n | grep -E '80|443'
```

2. **Firewall öffnen:**
```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

3. **Cloud-Provider-Firewall:** Auch Security Groups prüfen!

### Problem: Invalid Certificate

**Symptom:** Browser zeigt "Invalid Certificate" oder "Certificate Error"

**Lösung:**
1. **Staging-Zertifikat aktiv?**
   - Staging-Zertifikate sind nicht vertrauenswürdig
   - Zurück zu Production Let's Encrypt

2. **Zertifikat abgelaufen?**
```bash
openssl s_client -connect deine-domain.com:443 -servername deine-domain.com < /dev/null 2>/dev/null | openssl x509 -noout -dates
```

3. **Manuelle Erneuerung** (siehe oben)

---

## 🛡️ Security Best Practices

### TLS 1.3 bevorzugen

TLS 1.3 ist sicherer und schneller als TLS 1.2:
- Weniger Handshakes (schnellere Verbindung)
- Sicherere Cipher Suites
- Forward Secrecy immer aktiv

**Traefik bevorzugt TLS 1.3 automatisch!**

### Certificate Transparency

Let's Encrypt-Zertifikate sind in Certificate Transparency Logs:
[https://crt.sh/?q=deine-domain.com](https://crt.sh/)

**Vorteil:** Missbrauch erkennbar

### Certificate Pinning (NICHT empfohlen)

**Warum NICHT?**
- Let's Encrypt erneuert alle 90 Tage
- Certificate Pinning macht automatische Erneuerung unmöglich
- Risiko: App/Browser kann nicht mehr verbinden

**Besser:** HSTS mit lange max-age

---

## 📚 Weiterführende Docs

- [Reverse Proxy Setup](../deployment/REVERSE_PROXY_SETUP.md) - Traefik-Konfiguration
- [Security Headers](./SECURITY_HEADERS.md) - OWASP-Compliance
- [Container Security](./CONTAINER_SECURITY.md) - Docker Security

---

## 📖 Externe Ressourcen

- [Let's Encrypt Docs](https://letsencrypt.org/docs/)
- [SSL Labs Test](https://www.ssllabs.com/ssltest/)
- [HSTS Preload](https://hstspreload.org/)
- [Traefik TLS Docs](https://doc.traefik.io/traefik/https/tls/)

---

**Erstellt:** 2025-10-30  
**Version:** 1.0  
**Status:** ✅ Production-Ready
