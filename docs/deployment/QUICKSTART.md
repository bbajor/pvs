# Quick Start: Hetzner Deployment in 10 Minuten

## Schritt 1: Hetzner VPS erstellen (2 Min)

1. Gehe zu [hetzner.com/cloud](https://www.hetzner.com/cloud)
2. "Create Server" klicken
3. **Settings:**
   - Location: Nürnberg oder Falkenstein
   - Image: Ubuntu 22.04
   - Type: **CX21** (2 vCPU, 4GB RAM, 40GB SSD) - **5€/Monat**
   - SSH Key: Hinzufügen (oder später erstellen)
4. "Create & Buy now" → Server wird erstellt
5. **IP-Adresse** notieren!

## Schritt 2: Server Setup (3 Min)

SSH auf Server:

```bash
ssh root@<DEINE_IP>
```

Dann ausführen:

```bash
# Docker installieren
curl -fsSL https://get.docker.com -o get-docker.sh && sudo sh get-docker.sh

# Docker Compose installieren
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# User zu Docker-Gruppe hinzufügen
sudo usermod -aG docker $USER
newgrp docker

# Projekt-Verzeichnis erstellen
sudo mkdir -p /opt/pvs && sudo chown $USER:$USER /opt/pvs
cd /opt/pvs

# Environment-Datei erstellen
cat > .env <<EOF
POSTGRES_DB=pvs
POSTGRES_USER=pvs_user
POSTGRES_PASSWORD=$(openssl rand -base64 32)
DOCKER_REGISTRY=ghcr.io
DOCKER_IMAGE=bbajor/pvs
LETSENCRYPT_EMAIL=deine@email.de
EOF

echo "✅ Server Setup abgeschlossen!"
```

## Schritt 3: Docker Compose File kopieren (1 Min)

Von deinem lokalen Rechner:

```bash
scp docker-compose.production.yml root@<DEINE_IP>:/opt/pvs/
```

## Schritt 4: Datenbank initialisieren (2 Min)

Wieder auf dem Server:

```bash
cd /opt/pvs

# PostgreSQL starten
docker-compose -f docker-compose.production.yml up -d postgres

# Warten bis DB bereit ist (~10 Sekunden)
sleep 15

# Datenbanken für alle Stages erstellen
docker exec -it pvs-postgres psql -U pvs_user -d postgres -c "CREATE DATABASE pvs_dev;"
docker exec -it pvs-postgres psql -U pvs_user -d postgres -c "CREATE DATABASE pvs_test;"
docker exec -it pvs-postgres psql -U pvs_user -d postgres -c "CREATE DATABASE pvs_prod;"

echo "✅ Datenbanken erstellt!"
```

## Schritt 5: GitHub Secrets konfigurieren (2 Min)

1. **SSH Key für Deployment generieren:**

```bash
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/hetzner_deploy -N ""
```

2. **Public Key auf Server hinzufügen:**

```bash
cat ~/.ssh/hetzner_deploy.pub | ssh root@<DEINE_IP> "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

3. **GitHub Repository → Settings → Secrets → New Secret:**

Erstelle folgende Secrets:

```
HETZNER_HOST=<DEINE_IP>
HETZNER_USER=root
HETZNER_SSH_KEY=<Inhalt von ~/.ssh/hetzner_deploy>
PROD_DB_HOST=localhost
PROD_DB_NAME=pvs_prod
PROD_DB_USER=pvs_user
PROD_DB_PASSWORD=<Aus /opt/pvs/.env kopieren - Postgres Password>
```

**Private Key anzeigen:**
```bash
cat ~/.ssh/hetzner_deploy
```
→ Ganzer Inhalt kopieren und als `HETZNER_SSH_KEY` Secret speichern

**Postgres Password anzeigen (auf Server):**
```bash
grep POSTGRES_PASSWORD /opt/pvs/.env
```

## Schritt 6: Erste Deployment testen

### Option A: Via GitHub Actions (Empfohlen)

1. **Push Code zu GitHub:**
```bash
git push origin master
```

2. **GitHub Actions** wird automatisch:
   - Build durchführen
   - Docker Image erstellen
   - Zu Hetzner deployen

3. **Status prüfen:** GitHub Repository → Actions Tab

### Option B: Manueller Test

Auf dem Server:

```bash
cd /opt/pvs

# Dev Service starten
docker-compose -f docker-compose.production.yml up -d pvs-dev

# Logs ansehen
docker-compose -f docker-compose.production.yml logs -f pvs-dev

# Health Check
curl http://localhost:8080/actuator/health
```

## Schritt 7: Domain konfigurieren (Optional)

Falls du eine Domain hast:

1. **DNS A Records setzen:**
   ```
   dev.pvs.example.com  → <DEINE_IP>
   test.pvs.example.com → <DEINE_IP>
   pvs.example.com      → <DEINE_IP>
   ```

2. **docker-compose.production.yml** anpassen:
   - Ersetze `pvs.example.com` mit deiner Domain
   - Ersetze in allen `Host(...)` Labels

3. **Traefik starten:**
   ```bash
   docker-compose -f docker-compose.production.yml up -d traefik
   ```

4. **SSL wird automatisch** von Let's Encrypt eingerichtet!

## Fertig! 🎉

Deine App sollte jetzt laufen:
- **Dev**: `http://<DEINE_IP>:8080` (oder `https://dev.pvs.example.com`)
- **Test**: Via GitHub Actions bei Push auf `master`
- **Prod**: Via GitHub Actions manuell auslösen

## Troubleshooting

### Container startet nicht?
```bash
docker-compose -f docker-compose.production.yml logs -f
```

### Port bereits belegt?
```bash
sudo netstat -tulpn | grep :80
sudo netstat -tulpn | grep :443
```

### Database Error?
```bash
docker exec -it pvs-postgres psql -U pvs_user -l
```

## Nächste Schritte

- ✅ Monitoring einrichten (siehe HETZNER_SETUP.md)
- ✅ Backups konfigurieren
- ✅ Firewall einrichten
- ✅ Production User erstellen (über UI)

Soll ich dir beim nächsten Schritt helfen? 😊

