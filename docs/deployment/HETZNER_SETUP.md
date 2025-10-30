# Hetzner Cloud Setup Guide

## Übersicht

Dieses Setup nutzt einen Hetzner Cloud VPS (5€/Monat) für alle 3 Deployment-Stages mit Docker Compose, Traefik als Reverse Proxy und GitHub Actions für CI/CD.

**Kosten: ~5€/Monat für alles!**

## Voraussetzungen

1. Hetzner Cloud Account: [hetzner.com/cloud](https://www.hetzner.com/cloud)
2. GitHub Repository
3. Domain (optional, für SSL)

## Schritt 1: Hetzner VPS erstellen

1. **Hetzner Cloud Console** → "Add Server"
2. **Location**: Nürnberg oder Falkenstein (Deutschland, DSGVO-konform)
3. **Image**: Ubuntu 22.04
4. **Type**: CX21 (2 vCPU, 4GB RAM) - **5€/Monat**
5. **SSH Key** hinzufügen (oder erstellen)
6. Server erstellen und IP notieren

## Schritt 2: Server Setup

### SSH-Zugriff einrichten

```bash
ssh root@<HETZNER_IP>
```

### Docker & Docker Compose installieren

```bash
# Docker installieren
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Docker Compose installieren
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Docker ohne sudo nutzen
sudo usermod -aG docker $USER
newgrp docker

# Test
docker --version
docker-compose --version
```

### Projekt-Verzeichnis erstellen

```bash
sudo mkdir -p /opt/pvs
sudo chown $USER:$USER /opt/pvs
cd /opt/pvs
```

### Environment-Datei erstellen

```bash
cat > .env <<EOF
# Database Configuration
POSTGRES_DB=pvs
POSTGRES_USER=pvs_user
POSTGRES_PASSWORD=$(openssl rand -base64 32)

# Docker Registry
DOCKER_REGISTRY=ghcr.io
DOCKER_IMAGE=bbajor/pvs

# Let's Encrypt
LETSENCRYPT_EMAIL=deine@email.de

# Domains (anpassen!)
# Für dev/test/prod: dev.pvs.example.com, test.pvs.example.com, pvs.example.com
EOF
```

### Docker Compose File kopieren

Kopiere `docker-compose.production.yml` nach `/opt/pvs/`:

```bash
# Via SCP von lokalem Rechner
scp docker-compose.production.yml root@<HETZNER_IP>:/opt/pvs/
```

## Schritt 3: GitHub Secrets konfigurieren

GitHub Repository → Settings → Secrets and variables → Actions

**Erforderliche Secrets:**

```
HETZNER_HOST=<HETZNER_IP>
HETZNER_USER=root
HETZNER_SSH_KEY=<Dein privater SSH Key>
PROD_DB_HOST=localhost
PROD_DB_NAME=pvs_prod
PROD_DB_USER=pvs_user
PROD_DB_PASSWORD=<Aus .env kopieren>
```

**SSH Key generieren:**

```bash
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/hetzner_deploy
```

Public Key auf Hetzner Server hinzufügen:
```bash
cat ~/.ssh/hetzner_deploy.pub | ssh root@<HETZNER_IP> "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

Private Key als GitHub Secret speichern:
```bash
cat ~/.ssh/hetzner_deploy
```

## Schritt 4: Datenbank initialisieren

```bash
cd /opt/pvs
docker-compose -f docker-compose.production.yml up -d postgres

# Warten bis DB bereit ist
docker-compose -f docker-compose.production.yml logs postgres

# Datenbanken für alle Stages erstellen
docker exec -it pvs-postgres psql -U pvs_user -d postgres -c "CREATE DATABASE pvs_dev;"
docker exec -it pvs-postgres psql -U pvs_user -d postgres -c "CREATE DATABASE pvs_test;"
docker exec -it pvs-postgres psql -U pvs_user -d postgres -c "CREATE DATABASE pvs_prod;"
```

## Schritt 5: DNS & Domain konfigurieren

Wenn du eine Domain hast:

1. **DNS A Records** setzen:
   ```
   dev.pvs.example.com  → <HETZNER_IP>
   test.pvs.example.com → <HETZNER_IP>
   pvs.example.com      → <HETZNER_IP>
   ```

2. **Traefik automatisch SSL**:
   - Let's Encrypt wird automatisch von Traefik eingerichtet
   - Certificates werden in Docker Volume gespeichert

**Ohne Domain**: Nutze IP + Port (weniger sicher, kein SSL)

## Schritt 6: Erste Deployments

### Dev Deployment

```bash
# GitHub Actions Workflow wird automatisch ausgeführt bei Push auf develop
# Oder manuell:
gh workflow run "Build and Push Docker Images (Hetzner)" -f stage=dev
```

### Test Deployment

```bash
# Automatisch bei Push auf master
# Oder manuell:
gh workflow run "Build and Push Docker Images (Hetzner)" -f stage=test
```

### Production Deployment

```bash
# Manuell via GitHub Actions:
gh workflow run "Deploy Production to Hetzner"
```

## Monitoring & Wartung

### Logs ansehen

```bash
# Alle Services
docker-compose -f docker-compose.production.yml logs -f

# Spezifischer Service
docker-compose -f docker-compose.production.yml logs -f pvs-prod
```

### Status prüfen

```bash
docker-compose -f docker-compose.production.yml ps
```

### Health Checks

```bash
# Dev
curl http://localhost:8080/actuator/health

# Via Traefik (mit Domain)
curl https://dev.pvs.example.com/actuator/health
```

### Backup PostgreSQL

```bash
# Automatisches Backup (in Cron)
0 2 * * * docker exec pvs-postgres pg_dump -U pvs_user pvs_prod > /opt/pvs/backups/pvs_prod_$(date +\%Y\%m\%d).sql

# Manuelles Backup
docker exec pvs-postgres pg_dump -U pvs_user pvs_prod > backup.sql
```

### Rollback Production

```bash
cd /opt/pvs
docker-compose -f docker-compose.production.yml stop pvs-prod
docker tag ghcr.io/bbajor/pvs:prod-backup-YYYYMMDD-HHMMSS ghcr.io/bbajor/pvs:prod-latest
docker-compose -f docker-compose.production.yml up -d pvs-prod
```

## Firewall konfigurieren (Hetzner Cloud)

```bash
# UFW installieren
sudo apt install ufw

# Standard-Regeln
sudo ufw default deny incoming
sudo ufw default allow outgoing

# SSH erlauben
sudo ufw allow 22/tcp

# HTTP/HTTPS erlauben
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Firewall aktivieren
sudo ufw enable
sudo ufw status
```

## Troubleshooting

### Container startet nicht

```bash
docker-compose -f docker-compose.production.yml logs pvs-prod
docker ps -a
```

### Database Connection Error

```bash
# DB Status prüfen
docker exec -it pvs-postgres psql -U pvs_user -l

# Connection testen
docker exec -it pvs-postgres psql -U pvs_user -d pvs_prod -c "SELECT version();"
```

### SSL-Zertifikat funktioniert nicht

```bash
# Traefik Logs prüfen
docker logs pvs-traefik

# Zertifikate prüfen
docker exec pvs-traefik ls -la /letsencrypt/
```

### Port bereits belegt

```bash
sudo netstat -tulpn | grep :80
sudo netstat -tulpn | grep :443
# Process beenden oder Port ändern
```

## Sicherheits-Checkliste

- [ ] SSH Key Authentication (kein Passwort-Login)
- [ ] Firewall aktiviert (nur 22, 80, 443)
- [ ] Starke Passwörter in .env
- [ ] .env Datei nicht in Git committen
- [ ] SSL/TLS aktiviert (Traefik)
- [ ] Regelmäßige Backups
- [ ] Docker Images aus vertrauenswürdiger Registry
- [ ] Security Updates installieren

## Kosten-Übersicht

- **VPS (CX21)**: 5€/Monat
- **Traffic**: Unlimited (bei Hetzner)
- **Backups**: Optional (1€/Monat für automatische Snapshots)
- **Domain**: 10-15€/Jahr (optional)

**Gesamt: ~5€/Monat** 🎉

## Vorteile dieses Setups

✅ **Sehr günstig**: 5€/Monat für alles
✅ **Volle Kontrolle**: Self-Hosted
✅ **DSGVO-konform**: Server in Deutschland
✅ **Skalierbar**: Kann später auf mehrere Server erweitert werden
✅ **Backup-freundlich**: Alle Daten auf einem Server
✅ **GitHub Actions**: Kostenlose CI/CD

## Nachteile

⚠️ **Mehr Setup**: Mehr Konfiguration als Managed Services
⚠️ **Wartung**: Updates, Backups, Monitoring selbst verwalten
⚠️ **Single Point of Failure**: Ein Server für alles
⚠️ **Kein Auto-Scaling**: Manuelles Hochskalieren

