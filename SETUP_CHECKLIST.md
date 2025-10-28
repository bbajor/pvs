# Hetzner Deployment Checklist

## ✅ Bereit für Deployment?

### Code vorbereitet
- [x] Hetzner Docker Compose Config erstellt
- [x] GitHub Actions Workflows erstellt
- [x] Dokumentation vorhanden
- [ ] Code zu GitHub gepusht

### Hetzner Server Setup
- [ ] Hetzner VPS erstellt (CX21, 5€/Monat)
- [ ] SSH-Zugriff funktioniert
- [ ] Docker installiert
- [ ] Docker Compose installiert
- [ ] Projekt-Verzeichnis `/opt/pvs` erstellt
- [ ] `.env` Datei mit Passwörtern erstellt
- [ ] `docker-compose.production.yml` kopiert
- [ ] PostgreSQL Datenbanken erstellt (dev/test/prod)

### GitHub Secrets
- [ ] `HETZNER_HOST` (IP-Adresse)
- [ ] `HETZNER_USER` (meist `root`)
- [ ] `HETZNER_SSH_KEY` (privater SSH Key)
- [ ] `PROD_DB_HOST` (localhost)
- [ ] `PROD_DB_NAME` (pvs_prod)
- [ ] `PROD_DB_USER` (pvs_user)
- [ ] `PROD_DB_PASSWORD` (aus .env)

### Optional (Domain & SSL)
- [ ] Domain vorhanden
- [ ] DNS A Records gesetzt
- [ ] Traefik Labels in docker-compose angepasst
- [ ] Traefik Container gestartet

## Nächste Schritte

1. **Code pushen:**
   ```bash
   git push origin master
   ```

2. **Hetzner VPS erstellen** (siehe QUICKSTART.md)

3. **Server Setup durchführen** (siehe QUICKSTART.md)

4. **GitHub Secrets konfigurieren**

5. **Erste Deployment testen**

Soll ich dir Schritt für Schritt helfen? 😊

