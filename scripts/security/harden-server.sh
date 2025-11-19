#!/bin/bash
# Server-Härtung für Hetzner Server
# Führt umfassende Security-Maßnahmen durch
# WARNUNG: Script muss als root ausgeführt werden!

set -e

echo "🛡️  PVS Server Security Hardening"
echo "=================================="
echo ""
echo "⚠️  WICHTIG: Dieses Script:"
echo "   - Härtet SSH-Konfiguration"
echo "   - Konfiguriert Firewall (UFW)"
echo "   - Aktiviert Fail2Ban"
echo "   - Optimiert Kernel-Parameter"
echo "   - Deaktiviert unnötige Services"
echo ""
read -p "Fortfahren? (j/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Jj]$ ]]; then
    exit 1
fi

# Prüfe ob Script als root ausgeführt wird
if [ "$EUID" -ne 0 ]; then 
    echo "❌ Script muss als root ausgeführt werden"
    echo "Verwende: sudo $0"
    exit 1
fi

# System aktualisieren
echo ""
echo "📦 System aktualisieren..."
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get upgrade -y

# Security-Pakete installieren
echo ""
echo "📦 Installiere Security-Pakete..."
apt-get install -y \
    ufw \
    fail2ban \
    unattended-upgrades \
    apt-listchanges \
    rkhunter \
    aide \
    logwatch \
    auditd \
    apparmor \
    apparmor-utils

# ============================================
# 1. SSH-Härtung
# ============================================
echo ""
echo "🔐 Härte SSH-Konfiguration..."

# Backup der SSH-Config
cp /etc/ssh/sshd_config /etc/ssh/sshd_config.backup.$(date +%Y%m%d_%H%M%S)

# SSH-Config optimieren
cat >> /etc/ssh/sshd_config <<'EOF'

# ============================================
# Security Hardening (PVS)
# ============================================
# Nur Key-basierte Authentifizierung
PasswordAuthentication no
PubkeyAuthentication yes
PermitRootLogin no

# SSH-Protokoll-Version
Protocol 2

# Login-Versuche limitieren
MaxAuthTries 3
MaxSessions 3
MaxStartups 3:50:10

# Timeouts
ClientAliveInterval 300
ClientAliveCountMax 2
LoginGraceTime 60

# Deaktiviere unsichere Features
PermitEmptyPasswords no
X11Forwarding no
AllowTcpForwarding yes
PermitTunnel no

# Kompression deaktivieren (CPU-intensive)
Compression no

# Umgebungsvariablen nicht übernehmen
PermitUserEnvironment no
AcceptEnv LANG LC_*

# Logging
SyslogFacility AUTH
LogLevel VERBOSE
EOF

# SSH-Config testen
if sshd -t; then
    echo "✅ SSH-Config ist gültig"
    systemctl restart sshd
    echo "✅ SSH-Service neu gestartet"
else
    echo "❌ SSH-Config-Fehler! Stelle Backup wieder her..."
    cp /etc/ssh/sshd_config.backup.* /etc/ssh/sshd_config
    exit 1
fi

# ============================================
# 2. Firewall (UFW)
# ============================================
echo ""
echo "🔥 Konfiguriere Firewall..."

# UFW zurücksetzen
ufw --force reset

# Standard-Policies
ufw default deny incoming
ufw default allow outgoing

# SSH erlauben (wichtig: vorher SSH-Key konfigurieren!)
echo ""
echo "⚠️  WICHTIG: Stelle sicher, dass SSH-Key-Auth funktioniert!"
echo ""
echo "📋 Benutzer-Struktur:"
echo "   - 'pvs': Service-User für manuelle Admin-Tätigkeiten"
echo "   - 'deploy': Deployment-User für GitHub Actions (optional)"
echo ""
read -p "SSH-Key-Auth für mindestens einen User konfiguriert? (j/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Jj]$ ]]; then
    echo "⚠️  Bitte zuerst SSH-Key konfigurieren, dann Script erneut ausführen!"
    exit 1
fi

ufw allow 22/tcp comment 'SSH'

# HTTP/HTTPS (falls Traefik verwendet wird)
read -p "HTTP/HTTPS (80/443) für Traefik öffnen? (j/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Jj]$ ]]; then
    ufw allow 80/tcp comment 'HTTP'
    ufw allow 443/tcp comment 'HTTPS'
fi

# Firewall aktivieren
ufw --force enable
echo "✅ Firewall konfiguriert und aktiviert"

# ============================================
# 3. Fail2Ban
# ============================================
echo ""
echo "🛡️  Konfiguriere Fail2Ban..."

cat > /etc/fail2ban/jail.local <<'EOF'
[DEFAULT]
# Ban-Zeit: 1 Stunde
bantime = 3600
# Zeitfenster: 10 Minuten
findtime = 600
# Max. Versuche: 3
maxretry = 3
# E-Mail-Benachrichtigung (optional)
# destemail = admin@example.com
# sendername = Fail2Ban
# action = %(action_mwl)s

[sshd]
enabled = true
port = 22
logpath = %(sshd_log)s
maxretry = 3
bantime = 3600
findtime = 600

# Zusätzlicher Schutz für SSH
[sshd-ddos]
enabled = true
port = 22
logpath = %(sshd_log)s
maxretry = 10
findtime = 600
bantime = 3600
EOF

systemctl enable fail2ban
systemctl restart fail2ban
echo "✅ Fail2Ban konfiguriert und aktiviert"

# Fail2Ban-Status anzeigen
echo ""
echo "📊 Fail2Ban-Status:"
fail2ban-client status

# ============================================
# 4. Kernel-Parameter (sysctl)
# ============================================
echo ""
echo "⚙️  Optimiere Kernel-Parameter..."

# Backup
cp /etc/sysctl.conf /etc/sysctl.conf.backup.$(date +%Y%m%d_%H%M%S)

# Security-relevante Kernel-Parameter
cat >> /etc/sysctl.conf <<'EOF'

# ============================================
# Security Hardening (PVS)
# ============================================

# IP-Forwarding deaktivieren (wenn nicht benötigt)
net.ipv4.ip_forward = 0
net.ipv6.conf.all.forwarding = 0

# SYN-Flood-Schutz
net.ipv4.tcp_syncookies = 1
net.ipv4.tcp_max_syn_backlog = 2048
net.ipv4.tcp_synack_retries = 2
net.ipv4.tcp_syn_retries = 5

# ICMP-Schutz
net.ipv4.icmp_echo_ignore_broadcasts = 1
net.ipv4.icmp_ignore_bogus_error_responses = 1

# IP-Spoofing-Schutz
net.ipv4.conf.all.rp_filter = 1
net.ipv4.conf.default.rp_filter = 1

# Source-Routing deaktivieren
net.ipv4.conf.all.accept_source_route = 0
net.ipv4.conf.default.accept_source_route = 0
net.ipv6.conf.all.accept_source_route = 0
net.ipv6.conf.default.accept_source_route = 0

# Redirects deaktivieren
net.ipv4.conf.all.accept_redirects = 0
net.ipv4.conf.default.accept_redirects = 0
net.ipv6.conf.all.accept_redirects = 0
net.ipv6.conf.default.accept_redirects = 0
net.ipv4.conf.all.send_redirects = 0
net.ipv4.conf.default.send_redirects = 0

# IPv6 deaktivieren (optional, wenn nicht benötigt)
# net.ipv6.conf.all.disable_ipv6 = 1
# net.ipv6.conf.default.disable_ipv6 = 1

# Logging von verdächtigen Paketen
net.ipv4.conf.all.log_martians = 1
net.ipv4.conf.default.log_martians = 1

# TCP-Timestamps (optional, kann Performance beeinträchtigen)
# net.ipv4.tcp_timestamps = 0
EOF

# Änderungen anwenden
sysctl -p
echo "✅ Kernel-Parameter optimiert"

# ============================================
# 5. Automatische Updates
# ============================================
echo ""
echo "🔄 Konfiguriere automatische Updates..."

cat > /etc/apt/apt.conf.d/50unattended-upgrades <<'EOF'
Unattended-Upgrade::Allowed-Origins {
    "${distro_id}:${distro_codename}-security";
    "${distro_id}:${distro_codename}-updates";
};
Unattended-Upgrade::AutoFixInterruptedDpkg "true";
Unattended-Upgrade::MinimalSteps "true";
Unattended-Upgrade::Remove-Unused-Kernel-Packages "true";
Unattended-Upgrade::Remove-Unused-Dependencies "true";
Unattended-Upgrade::Automatic-Reboot "false";
Unattended-Upgrade::Mail "root";
EOF

systemctl enable unattended-upgrades
systemctl start unattended-upgrades
echo "✅ Automatische Updates konfiguriert"

# ============================================
# 6. Unnötige Services deaktivieren
# ============================================
echo ""
echo "🧹 Deaktiviere unnötige Services..."

# Services, die typischerweise nicht benötigt werden
SERVICES_TO_DISABLE=(
    "snapd"
    "bluetooth"
    "cups"
    "avahi-daemon"
)

for service in "${SERVICES_TO_DISABLE[@]}"; do
    if systemctl is-enabled "$service" &>/dev/null; then
        systemctl stop "$service"
        systemctl disable "$service"
        echo "   ✅ $service deaktiviert"
    fi
done

# ============================================
# 7. Logging & Monitoring
# ============================================
echo ""
echo "📝 Konfiguriere Logging..."

# Audit-Service aktivieren
systemctl enable auditd
systemctl start auditd

# Logwatch konfigurieren (tägliche E-Mail-Reports)
if [ -f /etc/logwatch/conf/logwatch.conf ]; then
    sed -i 's/^MailTo = root/MailTo = root/' /etc/logwatch/conf/logwatch.conf
    sed -i 's/^Range = yesterday/Range = yesterday/' /etc/logwatch/conf/logwatch.conf
fi

echo "✅ Logging konfiguriert"

# ============================================
# 8. AppArmor aktivieren
# ============================================
echo ""
echo "🔒 Aktiviere AppArmor..."

systemctl enable apparmor
systemctl start apparmor
aa-enforce /etc/apparmor.d/*
echo "✅ AppArmor aktiviert"

# ============================================
# 9. Rootkit-Scanner (rkhunter)
# ============================================
echo ""
echo "🔍 Initialisiere Rootkit-Scanner..."

# Initiale Datenbank erstellen
rkhunter --update
rkhunter --propupd
echo "✅ Rootkit-Scanner initialisiert"

# ============================================
# 10. File Integrity (AIDE)
# ============================================
echo ""
echo "🔐 Initialisiere File Integrity Monitoring..."

# Initiale Datenbank erstellen (kann etwas dauern)
if [ ! -f /var/lib/aide/aide.db ]; then
    aideinit --yes
    echo "✅ AIDE initialisiert"
else
    echo "ℹ️  AIDE bereits initialisiert"
fi

# ============================================
# Zusammenfassung
# ============================================
echo ""
echo "✅ Server-Härtung abgeschlossen!"
echo ""
echo "📊 Status-Übersicht:"
echo "==================="
echo ""
echo "🔥 Firewall:"
ufw status verbose | head -5
echo ""
echo "🛡️  Fail2Ban:"
fail2ban-client status | head -3
echo ""
echo "🔐 SSH:"
systemctl status sshd --no-pager | head -3
echo ""
echo "📝 Nächste Schritte:"
echo "==================="
echo ""
echo "1. SSH-Verbindung testen (mit Key-Auth):"
echo "   # Als pvs-User (für manuelle Admin-Tätigkeiten):"
echo "   ssh -i ~/.ssh/id_ed25519 pvs@server"
echo ""
echo "   # Als deploy-User (für GitHub Actions, optional):"
echo "   ssh -i ~/.ssh/id_ed25519 deploy@server 'sudo -u pvs whoami'"
echo ""
echo "2. Firewall-Status prüfen:"
echo "   sudo ufw status verbose"
echo ""
echo "3. Fail2Ban-Status prüfen:"
echo "   sudo fail2ban-client status sshd"
echo ""
echo "4. Offene Ports prüfen:"
echo "   sudo ss -tulpn"
echo ""
echo "5. Hetzner Cloud Firewall konfigurieren:"
echo "   - SSH nur von vertrauenswürdigen IPs"
echo "   - HTTP/HTTPS von überall (falls benötigt)"
echo ""
echo "6. Regelmäßige Security-Checks:"
echo "   sudo rkhunter --check"
echo "   sudo aide --check"
echo ""
echo "⚠️  WICHTIG:"
echo "   - SSH-Password-Auth ist deaktiviert!"
echo "   - Nur Key-basierte Authentifizierung möglich"
echo "   - Root-Login ist deaktiviert"
echo "   - Firewall blockiert alle Ports außer SSH (und ggf. 80/443)"
echo ""
echo "🔗 Weitere Infos: docs/security/SERVER_HARDENING.md"
echo ""

