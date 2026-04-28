# SSH-Tunnel Setup für Test-Instanz

Die Test-Instanz läuft auf dem Hetzner-Server, ist aber nicht öffentlich erreichbar. Der Zugriff erfolgt über einen SSH-Tunnel.

## Voraussetzungen

- SSH-Zugriff auf Hetzner-Server
- SSH-Key konfiguriert
- Test-Instanz läuft auf Port 8081 (nur localhost)

## 1. SSH-Tunnel manuell einrichten

### Linux/macOS

```bash
# SSH-Tunnel erstellen (Port-Forwarding)
ssh -L 8081:localhost:8081 -N user@hetzner-server.example.com

# Im Hintergrund mit Logging
ssh -L 8081:localhost:8081 -N -f user@hetzner-server.example.com \
  -o ServerAliveInterval=60 \
  -o ServerAliveCountMax=3 \
  -o ExitOnForwardFailure=yes \
  > /tmp/ssh-tunnel.log 2>&1

# Tunnel-Status prüfen
ps aux | grep "ssh.*8081"
```

### Windows (PowerShell)

```powershell
# SSH-Tunnel erstellen
ssh -L 8081:localhost:8081 -N user@hetzner-server.example.com

# Im Hintergrund (Start-Job)
$job = Start-Job -ScriptBlock {
    ssh -L 8081:localhost:8081 -N user@hetzner-server.example.com `
        -o ServerAliveInterval=60 `
        -o ServerAliveCountMax=3 `
        -o ExitOnForwardFailure=yes
}

# Job-Status prüfen
Get-Job
Receive-Job -Id $job.Id
```

### Windows (PuTTY)

1. Öffne PuTTY
2. Connection → SSH → Tunnels
3. Source port: `8081`
4. Destination: `localhost:8081`
5. Add
6. Session → Host Name: `hetzner-server.example.com`
7. Save & Open

## 2. Automatisches SSH-Tunnel-Script

### Linux/macOS Script

```bash
# Script ausführen
./scripts/deployment/ssh-tunnel-test.sh

# Oder mit Parametern
./scripts/deployment/ssh-tunnel-test.sh \
  user@hetzner-server.example.com \
  /path/to/ssh/key
```

### Windows PowerShell Script

```powershell
# Script ausführen
.\scripts\deployment\ssh-tunnel-test.ps1

# Oder mit Parametern
.\scripts\deployment\ssh-tunnel-test.ps1 `
  -Server "user@hetzner-server.example.com" `
  -SshKey "C:\Users\username\.ssh\id_rsa"
```

## 3. Tunnel-Verifikation

### Health Check

```bash
# Nach SSH-Tunnel-Einrichtung
curl http://localhost:8081/actuator/health

# Sollte zurückgeben:
# {"status":"UP",...}
```

### Application-Zugriff

```bash
# Browser öffnen
# http://localhost:8081

# Oder mit curl
curl http://localhost:8081/
```

## 4. Tunnel-Probleme beheben

### Port bereits belegt

```bash
# Prüfe ob Port 8081 bereits verwendet wird
# Linux/macOS
lsof -i :8081
netstat -tulpn | grep 8081

# Windows
netstat -ano | findstr :8081

# Prozess beenden
# Linux/macOS
kill <PID>

# Windows
taskkill /PID <PID> /F
```

### SSH-Verbindung bricht ab

```bash
# ServerAliveInterval setzen (siehe Scripts)
# Oder in ~/.ssh/config:
Host hetzner-server
    HostName hetzner-server.example.com
    User user
    ServerAliveInterval 60
    ServerAliveCountMax 3
```

### Firewall blockiert

```bash
# Prüfe Firewall auf Server
sudo ufw status

# Sollte nur Port 22 (SSH) offen sein
# Application-Port (8081) sollte NICHT in UFW sein
```

## 5. Tunnel dauerhaft einrichten

### Linux/macOS (systemd)

```bash
# Service-Datei erstellen
sudo nano /etc/systemd/system/ssh-tunnel-test.service
```

```ini
[Unit]
Description=SSH Tunnel for PVS Test Instance
After=network.target

[Service]
Type=simple
User=dein-user
ExecStart=/usr/bin/ssh -L 8081:localhost:8081 -N user@hetzner-server.example.com -o ServerAliveInterval=60 -o ServerAliveCountMax=3 -o ExitOnForwardFailure=yes
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# Service aktivieren
sudo systemctl daemon-reload
sudo systemctl enable ssh-tunnel-test
sudo systemctl start ssh-tunnel-test

# Status prüfen
sudo systemctl status ssh-tunnel-test
```

### Windows (Task Scheduler)

1. Task Scheduler öffnen
2. Create Basic Task
3. Name: "PVS Test SSH Tunnel"
4. Trigger: When I log on
5. Action: Start a program
6. Program: `C:\Windows\System32\OpenSSH\ssh.exe`
7. Arguments: `-L 8081:localhost:8081 -N user@hetzner-server.example.com -o ServerAliveInterval=60`
8. Finish

## 6. Automatisches Reconnect

Die Scripts enthalten automatisches Reconnect bei Verbindungsabbruch:

- `ServerAliveInterval=60`: Alle 60 Sekunden Keep-Alive
- `ServerAliveCountMax=3`: Nach 3 fehlgeschlagenen Keep-Alives neu verbinden
- `ExitOnForwardFailure=yes`: Bei Port-Forwarding-Fehler beenden

## 7. Sicherheit

### SSH-Key statt Password

```bash
# SSH-Key generieren (falls noch nicht vorhanden)
ssh-keygen -t ed25519 -C "pvs-test-tunnel"

# Public Key auf Server kopieren
ssh-copy-id -i ~/.ssh/id_ed25519.pub user@hetzner-server.example.com

# Test-Verbindung
ssh user@hetzner-server.example.com
```

### SSH-Config optimieren

```bash
# ~/.ssh/config
Host hetzner-test
    HostName hetzner-server.example.com
    User user
    IdentityFile ~/.ssh/id_ed25519
    ServerAliveInterval 60
    ServerAliveCountMax 3
    Compression yes
    ControlMaster auto
    ControlPath ~/.ssh/control-%h-%p-%r
    ControlPersist 10m
```

## 8. Troubleshooting

### "Address already in use"

```bash
# Port 8081 ist bereits belegt
# Lösung: Anderen Port verwenden oder Prozess beenden
ssh -L 8082:localhost:8081 -N user@hetzner-server.example.com
# Dann: http://localhost:8082
```

### "Connection refused"

```bash
# Prüfe ob Test-Instanz läuft
ssh user@hetzner-server.example.com
podman ps | grep pvs-test

# Prüfe ob Port 8081 auf Server erreichbar ist
curl http://localhost:8081/actuator/health
```

### Tunnel bricht ständig ab

```bash
# Erhöhe ServerAliveInterval
ssh -L 8081:localhost:8081 -N \
  -o ServerAliveInterval=120 \
  -o ServerAliveCountMax=5 \
  user@hetzner-server.example.com
```

## 9. Alternative: VPN

Für dauerhaften Zugriff ohne SSH-Tunnel:

1. WireGuard VPN auf Server einrichten
2. Client verbinden
3. Direkter Zugriff auf `http://10.0.0.X:8081`

Siehe `docs/CLOUD_NETWORK_SECURITY.md` für VPN-Setup.

