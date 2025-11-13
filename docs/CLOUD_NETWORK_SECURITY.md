# Netzwerk-Sicherheit - Firewall & Port-Konfiguration

## Firewall-Regeln (UFW)

### Basis-Konfiguration

```bash
# UFW aktivieren
sudo ufw enable

# Standard-Policy
sudo ufw default deny incoming
sudo ufw default allow outgoing
```

### Erlaubte Ports

```bash
# SSH (wichtig: vorher konfigurieren!)
sudo ufw allow 22/tcp comment 'SSH'

# HTTP/HTTPS (via Traefik)
sudo ufw allow 80/tcp comment 'HTTP'
sudo ufw allow 443/tcp comment 'HTTPS'

# PostgreSQL (nur lokal, wenn nicht Managed)
# NICHT öffentlich öffnen!
# sudo ufw allow from 127.0.0.1 to any port 5432

# Redis (nur lokal)
sudo ufw allow from 127.0.0.1 to any port 6379

# Application (nur lokal, via Traefik)
sudo ufw allow from 127.0.0.1 to any port 8080
```

### Firewall-Status prüfen

```bash
# Status anzeigen
sudo ufw status verbose

# Logs prüfen
sudo tail -f /var/log/ufw.log
```

## Hetzner Cloud Firewall

### Firewall-Regeln in Hetzner Console

1. **Firewall erstellen:**
   - Name: `pvs-production-firewall`
   - Inbound Rules:
     - SSH (22/tcp) - nur von vertrauenswürdigen IPs
     - HTTP (80/tcp) - von überall
     - HTTPS (443/tcp) - von überall
   - Outbound Rules:
     - Alle erlauben (Standard)

2. **Firewall zu Server zuweisen:**
   - Server → Networking → Firewalls → Assign Firewall

### Empfohlene IP-Whitelist für SSH

```bash
# Nur von vertrauenswürdigen IPs SSH erlauben
# Beispiel: Büro-IP, VPN-IP
# In Hetzner Console: Firewall → Inbound Rules → SSH → Source IPs
```

## Port-Konfiguration

### Container-Ports

```yaml
# podman-compose.production.yml
services:
  pvs-prod:
    ports:
      - "127.0.0.1:8080:8080"  # Nur lokal, via Traefik
  postgres-prod:
    ports:
      - "127.0.0.1:5432:5432"  # Nur lokal
  redis:
    ports:
      - "127.0.0.1:6379:6379"  # Nur lokal
  traefik:
    ports:
      - "80:80"      # HTTP
      - "443:443"    # HTTPS
```

### Port-Übersicht

| Port | Service | Zugriff | Beschreibung |
|------|---------|---------|--------------|
| 22 | SSH | Extern (whitelist) | Server-Zugriff |
| 80 | Traefik | Extern | HTTP → HTTPS Redirect |
| 443 | Traefik | Extern | HTTPS |
| 8080 | PVS App | Lokal | Application (via Traefik) |
| 5432 | PostgreSQL | Lokal | Datenbank |
| 6379 | Redis | Lokal | Session Storage |

## Traefik Reverse Proxy

### SSL/TLS Konfiguration

```yaml
# traefik.yml
entryPoints:
  web:
    address: ":80"
    http:
      redirections:
        entryPoint:
          to: websecure
          scheme: https
  websecure:
    address: ":443"
    http:
      tls:
        certResolver: letsencrypt

certificatesResolvers:
  letsencrypt:
    acme:
      email: admin@example.com
      storage: /letsencrypt/acme.json
      httpChallenge:
        entryPoint: web
```

## DDoS-Schutz

### Rate-Limiting (Traefik)

```yaml
# traefik.yml
http:
  middlewares:
    rate-limit:
      rateLimit:
        average: 100
        burst: 50
    ip-whitelist:
      ipWhiteList:
        sourceRange:
          - "127.0.0.1/32"
          - "10.0.0.0/8"
```

### Fail2Ban (optional)

```bash
# Installieren
sudo apt-get install fail2ban

# Konfiguration
sudo nano /etc/fail2ban/jail.local
```

```ini
[sshd]
enabled = true
port = 22
maxretry = 3
bantime = 3600
```

## Netzwerk-Monitoring

### Port-Scan prüfen

```bash
# Offene Ports prüfen
sudo netstat -tulpn | grep LISTEN

# Externe Port-Scans
sudo nmap -sS -O localhost
```

### Verbindungen überwachen

```bash
# Aktive Verbindungen
sudo ss -tulpn

# Verbindungen nach Port
sudo ss -tulpn | grep :443
```

## Best Practices

1. **Minimales Port-Exposure:**
   - Nur notwendige Ports öffnen
   - Services nur lokal binden, wenn möglich

2. **SSH-Härtung:**
   - Key-basierte Authentifizierung
   - Password-Auth deaktivieren
   - Port 22 ändern (optional)

3. **Regelmäßige Updates:**
   ```bash
   sudo apt-get update && sudo apt-get upgrade -y
   ```

4. **Log-Monitoring:**
   - UFW-Logs überwachen
   - Ungewöhnliche Verbindungen prüfen

5. **VPN für Admin-Zugriff:**
   - SSH nur über VPN erlauben
   - Firewall-Regeln entsprechend anpassen
