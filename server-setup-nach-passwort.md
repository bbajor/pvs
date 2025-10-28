# Server Setup - Nach Passwort-Wechsel

## ✅ Nach erfolgreichem Passwort-Wechsel:

### 1. Public Key hinzufügen

```bash
# SSH auf Server
ssh root@188.245.253.179

# Public Key hinzufügen
mkdir -p ~/.ssh
echo "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIIUfmtBvnplytzesXcinl9XScn2XKJfpFWDXzIPpD6/1 github-actions-hetzner" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
chmod 700 ~/.ssh
exit
```

### 2. SSH ohne Passwort testen

```powershell
ssh -i $env:USERPROFILE\.ssh\hetzner_deploy root@188.245.253.179 "echo '✅ SSH funktioniert!'"
```

### 3. Server-Setup starten

```powershell
# Setup-Script auf Server kopieren
scp setup-server.sh root@188.245.253.179:/root/

# Setup ausführen
ssh root@188.245.253.179 "chmod +x /root/setup-server.sh && /root/setup-server.sh"
```

### 4. Datenbanken initialisieren

```powershell
scp init-databases.sh root@188.245.253.179:/opt/pvs/
ssh root@188.245.253.179 "cd /opt/pvs && chmod +x init-databases.sh && ./init-databases.sh"
```

### 5. Docker Compose kopieren

```powershell
scp docker-compose.production.yml root@188.245.253.179:/opt/pvs/
```

## 🎯 Danach: GitHub Secrets konfigurieren (siehe nächste Schritte)

