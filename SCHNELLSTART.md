# 🚀 Schnellstart - Hetzner Deployment

## Code ist auf GitHub! ✅

## Nächste Schritte:

### 1. Hetzner VPS erstellen (2 Min)

👉 [hetzner.com/cloud](https://www.hetzner.com/cloud)
- **Type**: CX21 (5€/Monat)
- **Location**: Nürnberg oder Falkenstein
- **Image**: Ubuntu 22.04
- **IP notieren!**

### 2. Server-Setup ausführen (3 Min)

```bash
# Script auf Server kopieren
scp setup-server.sh root@<IP>:/root/

# Setup ausführen
ssh root@<IP> "chmod +x /root/setup-server.sh && /root/setup-server.sh"
```

### 3. Datenbanken initialisieren (2 Min)

```bash
scp init-databases.sh root@<IP>:/opt/pvs/
ssh root@<IP> "cd /opt/pvs && chmod +x init-databases.sh && ./init-databases.sh"
```

### 4. Docker Compose kopieren (1 Min)

```bash
scp docker-compose.production.yml root@<IP>:/opt/pvs/
```

### 5. GitHub Secrets konfigurieren (3 Min)

```bash
# SSH Key generieren
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/hetzner_deploy -N ""

# Public Key auf Server
cat ~/.ssh/hetzner_deploy.pub | ssh root@<IP> "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"

# Private Key für GitHub
cat ~/.ssh/hetzner_deploy

# Postgres Password
ssh root@<IP> "grep POSTGRES_PASSWORD /opt/pvs/.env"
```

GitHub → Settings → Secrets → Actions → New Secret:
- `HETZNER_HOST`: `<IP>`
- `HETZNER_USER`: `root`
- `HETZNER_SSH_KEY`: `<Private Key Inhalt>`
- `PROD_DB_HOST`: `localhost`
- `PROD_DB_NAME`: `pvs_prod`
- `PROD_DB_USER`: `pvs_user`
- `PROD_DB_PASSWORD`: `<aus .env>`

### 6. Deployment testen

GitHub → Actions → "Build and Push Docker Images (Hetzner)" → Run workflow → Stage: dev

**Fertig!** 🎉

## Detaillierte Anleitung

Siehe: `docs/deployment/SETUP_STEPS.md`

