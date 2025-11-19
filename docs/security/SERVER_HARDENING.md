# Server-Härtung - Hetzner Server Security

Umfassender Guide zur Härtung des Hetzner-Servers gegen Angriffe.

## Übersicht

Dieser Guide beschreibt alle Security-Maßnahmen, die auf einem Hetzner-Server implementiert werden sollten, um ihn gegen Angriffe zu schützen.

## Quick Start

```bash
# Auf dem Hetzner Server (als root)
cd /root
wget https://raw.githubusercontent.com/bbajor/pvs/master/scripts/security/harden-server.sh
chmod +x harden-server.sh
sudo ./harden-server.sh
```

**WICHTIG:** Stelle sicher, dass SSH-Key-Authentifizierung bereits funktioniert, bevor du das Script ausführst!

## Benutzer-Struktur

### `pvs`-User (Service-User)
- **Zweck:** Manuelle Admin-Tätigkeiten, Container-Verwaltung
- **Besitzt:** `/opt/pvs` (Projekt-Verzeichnis)
- **Rechte:** Podman-Gruppe, kann Container starten/stoppen
- **Anmeldung:** `ssh pvs@server`
- **Verwendung:** Für alle manuellen Server-Operationen

### `deploy`-User (Deployment-User)
- **Zweck:** Automatisierte Deployments via GitHub Actions
- **Rechte:** Kann als `pvs`-User Befehle ausführen (sudo)
- **Anmeldung:** `ssh deploy@server` (nur für CI/CD)
- **Verwendung:** Wird von GitHub Actions verwendet, nicht für manuelle Arbeit

### Empfehlung
**Für manuelle Anmeldung:** Verwende den **`pvs`-User**
- Direkter Zugriff auf `/opt/pvs`
- Keine sudo-Rechte nötig für normale Operationen
- Klare Trennung zwischen Service- und Deployment-User

## Implementierte Maßnahmen

### 1. SSH-Härtung

#### Konfiguration

- ✅ **Password-Authentifizierung deaktiviert** - Nur Key-basierte Auth
- ✅ **Root-Login deaktiviert** - Kein direkter Root-Zugriff
- ✅ **MaxAuthTries = 3** - Begrenzte Login-Versuche
- ✅ **ClientAliveInterval = 300** - Timeout nach 5 Minuten Inaktivität
- ✅ **X11Forwarding deaktiviert** - Keine X11-Weiterleitung
- ✅ **Verbose Logging** - Detaillierte SSH-Logs

#### SSH-Config

Die SSH-Config wird automatisch gehärtet:

```bash
# Manuelle Prüfung
sudo sshd -T | grep -E "(PasswordAuthentication|PermitRootLogin|MaxAuthTries)"
```

#### SSH-Key-Setup

```bash
# Auf lokalem Rechner: Key generieren (falls noch nicht vorhanden)
ssh-keygen -t ed25519 -C "pvs-server"

# Public Key auf Server kopieren
ssh-copy-id -i ~/.ssh/id_ed25519.pub user@server

# Oder manuell:
cat ~/.ssh/id_ed25519.pub | ssh user@server "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

### 2. Firewall (UFW)

#### Basis-Konfiguration

- ✅ **Default: Deny Incoming** - Alle eingehenden Verbindungen blockiert
- ✅ **Default: Allow Outgoing** - Ausgehende Verbindungen erlaubt
- ✅ **Nur SSH (Port 22) offen** - Standard-Konfiguration
- ✅ **Optional: HTTP/HTTPS (80/443)** - Falls Traefik verwendet wird

#### Firewall-Status prüfen

```bash
# Status anzeigen
sudo ufw status verbose

# Logs prüfen
sudo tail -f /var/log/ufw.log

# Regeln testen
sudo ufw status numbered
```

#### Firewall-Regeln anpassen

```bash
# HTTP/HTTPS hinzufügen (falls benötigt)
sudo ufw allow 80/tcp comment 'HTTP'
sudo ufw allow 443/tcp comment 'HTTPS'

# Regel löschen
sudo ufw delete [NUM]

# Firewall zurücksetzen (VORSICHT!)
sudo ufw --force reset
```

### 3. Fail2Ban

#### Konfiguration

- ✅ **SSH-Schutz aktiviert** - 3 Fehlversuche = 1 Stunde Ban
- ✅ **DDoS-Schutz aktiviert** - 10 Versuche in 10 Minuten = 1 Stunde Ban
- ✅ **Automatische IP-Blockierung**

#### Fail2Ban-Status

```bash
# Status anzeigen
sudo fail2ban-client status

# SSH-Jail-Status
sudo fail2ban-client status sshd

# Gebannte IPs anzeigen
sudo fail2ban-client status sshd | grep "Banned IP list"

# IP manuell bannen
sudo fail2ban-client set sshd banip 1.2.3.4

# IP entbannen
sudo fail2ban-client set sshd unbanip 1.2.3.4
```

#### Fail2Ban-Logs

```bash
# Logs anzeigen
sudo tail -f /var/log/fail2ban.log

# Jail-spezifische Logs
sudo grep "sshd" /var/log/fail2ban.log
```

### 4. Kernel-Parameter (sysctl)

#### Implementierte Optimierungen

- ✅ **IP-Forwarding deaktiviert** - Kein Routing
- ✅ **SYN-Flood-Schutz** - TCP-Syncookies aktiviert
- ✅ **ICMP-Schutz** - Broadcast-Pings blockiert
- ✅ **IP-Spoofing-Schutz** - Reverse Path Filtering
- ✅ **Source-Routing deaktiviert** - Keine Source-Route-Pakete
- ✅ **Redirects deaktiviert** - Keine ICMP-Redirects
- ✅ **Martian-Logging** - Verdächtige Pakete werden geloggt

#### Kernel-Parameter prüfen

```bash
# Alle Security-relevanten Parameter anzeigen
sysctl -a | grep -E "(ip_forward|syncookies|rp_filter|accept_source_route)"

# Parameter manuell setzen
sudo sysctl -w net.ipv4.ip_forward=0
```

### 5. Automatische Updates

#### Konfiguration

- ✅ **Security-Updates automatisch** - Nur Security-Updates
- ✅ **Kernel-Updates automatisch** - Alte Kernel werden entfernt
- ✅ **Kein automatischer Reboot** - Manuelle Kontrolle

#### Update-Status prüfen

```bash
# Update-Logs anzeigen
sudo cat /var/log/unattended-upgrades/unattended-upgrades.log

# Manuelle Updates
sudo apt-get update && sudo apt-get upgrade -y
```

### 6. Unnötige Services

#### Deaktivierte Services

- ✅ **snapd** - Snap-Pakete (nicht benötigt)
- ✅ **bluetooth** - Bluetooth (auf Server nicht benötigt)
- ✅ **cups** - Drucker-Service (nicht benötigt)
- ✅ **avahi-daemon** - mDNS/Bonjour (Sicherheitsrisiko)

#### Services prüfen

```bash
# Alle aktiven Services anzeigen
systemctl list-units --type=service --state=running

# Service-Status prüfen
systemctl status [service-name]

# Service deaktivieren
sudo systemctl stop [service-name]
sudo systemctl disable [service-name]
```

### 7. Logging & Monitoring

#### Audit-Service

- ✅ **auditd aktiviert** - System-Audit-Logging
- ✅ **Logwatch konfiguriert** - Tägliche E-Mail-Reports

#### Logs prüfen

```bash
# SSH-Logs
sudo tail -f /var/log/auth.log

# System-Logs
sudo journalctl -xe

# Audit-Logs
sudo ausearch -m all

# Logwatch manuell ausführen
sudo logwatch --mailto admin@example.com
```

### 8. AppArmor

#### Konfiguration

- ✅ **AppArmor aktiviert** - Mandatory Access Control
- ✅ **Alle Profile im Enforce-Modus**

#### AppArmor-Status

```bash
# Status anzeigen
sudo aa-status

# Profile anzeigen
sudo aa-status | grep "profiles are in enforce mode"

# Profile in Enforce-Modus setzen
sudo aa-enforce /etc/apparmor.d/*
```

### 9. Rootkit-Scanner (rkhunter)

#### Initialisierung

```bash
# Datenbank aktualisieren
sudo rkhunter --update

# Properties aktualisieren
sudo rkhunter --propupd

# Scan ausführen
sudo rkhunter --check
```

#### Automatische Scans

```bash
# Cron-Job für tägliche Scans
sudo crontab -e
# Füge hinzu:
0 2 * * * /usr/bin/rkhunter --check --skip-keypress --report-warnings-only
```

### 10. File Integrity (AIDE)

#### Initialisierung

```bash
# Initiale Datenbank erstellen (wird beim Hardening-Script automatisch gemacht)
sudo aideinit

# Scan ausführen
sudo aide --check

# Datenbank aktualisieren (nach legitimen Änderungen)
sudo aide --update
sudo mv /var/lib/aide/aide.db.new /var/lib/aide/aide.db
```

#### Automatische Scans

```bash
# Cron-Job für tägliche Scans
sudo crontab -e
# Füge hinzu:
0 3 * * * /usr/bin/aide --check
```

## Hetzner Cloud Firewall

### Firewall in Hetzner Console konfigurieren

1. **Firewall erstellen:**
   - Gehe zu Hetzner Cloud Console → Firewalls
   - Erstelle neue Firewall: `pvs-production-firewall`

2. **Inbound Rules:**
   - **SSH (22/tcp)** - Nur von vertrauenswürdigen IPs
     - Source IPs: Deine Büro-IP, VPN-IP, etc.
   - **HTTP (80/tcp)** - Von überall (optional)
   - **HTTPS (443/tcp)** - Von überall (optional)

3. **Outbound Rules:**
   - Alle erlauben (Standard)

4. **Firewall zuweisen:**
   - Server → Networking → Firewalls → Assign Firewall

### IP-Whitelist für SSH

**Empfehlung:** SSH nur von vertrauenswürdigen IPs erlauben:

1. In Hetzner Console: Firewall → Inbound Rules → SSH
2. Source IPs hinzufügen:
   - Büro-IP
   - VPN-IP
   - Persönliche IP (dynamisch, regelmäßig aktualisieren)

**Alternative:** VPN für Admin-Zugriff verwenden

## Security-Checkliste

### Nach dem Hardening

- [ ] SSH-Key-Auth funktioniert
- [ ] Password-Auth ist deaktiviert
- [ ] Root-Login ist deaktiviert
- [ ] Firewall ist aktiviert (nur SSH offen)
- [ ] Fail2Ban ist aktiviert
- [ ] Automatische Updates sind aktiviert
- [ ] Hetzner Cloud Firewall ist konfiguriert
- [ ] SSH nur von vertrauenswürdigen IPs (Hetzner Firewall)

### Regelmäßige Checks

- [ ] **Wöchentlich:** Security-Updates prüfen
- [ ] **Monatlich:** rkhunter-Scan ausführen
- [ ] **Monatlich:** AIDE-Scan ausführen
- [ ] **Monatlich:** Logs prüfen (auth.log, fail2ban.log)
- [ ] **Quartal:** Security-Audit durchführen

## Troubleshooting

### SSH-Zugriff verloren

**Problem:** Nach SSH-Härtung kein Zugriff mehr möglich

**Lösung:**
1. Via Hetzner Console → Rescue-System starten
2. SSH-Config zurücksetzen:
   ```bash
   mount /dev/sda1 /mnt
   cp /mnt/etc/ssh/sshd_config.backup.* /mnt/etc/ssh/sshd_config
   ```
3. Server neu starten

### Fail2Ban blockiert legitime IPs

**Problem:** Eigene IP wird fälschlicherweise gebannt

**Lösung:**
```bash
# IP entbannen
sudo fail2ban-client set sshd unbanip [IP]

# IP zu Whitelist hinzufügen
sudo nano /etc/fail2ban/jail.local
# Füge hinzu:
[sshd]
ignoreip = 127.0.0.1/8 ::1 [DEINE_IP]
```

### Firewall blockiert benötigten Service

**Problem:** Service ist nicht erreichbar

**Lösung:**
```bash
# Port temporär öffnen (zum Testen)
sudo ufw allow [PORT]/tcp

# Regel dauerhaft hinzufügen
sudo ufw allow [PORT]/tcp comment 'Service-Name'
```

## Best Practices

### 1. Minimales Port-Exposure

- Nur notwendige Ports öffnen
- Services nur auf localhost binden, wenn möglich
- Zugriff über Reverse Proxy (Traefik)

### 2. Defense in Depth

- Mehrschichtiger Schutz:
  1. Hetzner Cloud Firewall (erste Ebene)
  2. UFW (zweite Ebene)
  3. Fail2Ban (dritte Ebene)
  4. AppArmor (vierte Ebene)

### 3. Regelmäßige Updates

- Security-Updates automatisch installieren
- Regelmäßig manuelle Updates prüfen
- Kernel-Updates testen

### 4. Monitoring & Logging

- Logs regelmäßig prüfen
- Ungewöhnliche Aktivitäten erkennen
- Automatische Alerts einrichten

### 5. Backup & Recovery

- Regelmäßige Backups
- Disaster-Recovery-Plan
- Test-Restores durchführen

## Weitere Ressourcen

- [Hetzner Cloud Firewall Docs](https://docs.hetzner.com/cloud/firewalls/)
- [UFW Documentation](https://help.ubuntu.com/community/UFW)
- [Fail2Ban Documentation](https://www.fail2ban.org/wiki/index.php/Main_Page)
- [CIS Benchmarks](https://www.cisecurity.org/cis-benchmarks/)

## Support

Bei Fragen oder Problemen:
- GitHub Issues: [Repository](https://github.com/bbajor/pvs)
- Dokumentation: `docs/security/`

