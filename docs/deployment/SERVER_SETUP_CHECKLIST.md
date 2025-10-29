# Server-Setup Checkliste - Hetzner

Diese Checkliste enthält alle noch offenen To-dos für die sichere Konfiguration des Hetzner Servers.

## ✅ Bereits erledigt

- [x] Branch-Struktur: `dev`, `test`, `master` Branches erstellt
- [x] GitHub Actions Workflows korrigiert (Branch-Namen)
- [x] Branch-Schutz Workflows erstellt (`master-protection.yml`, `test-ci.yml`, `dev-ci.yml`)
- [x] CODEOWNERS Datei erstellt
- [x] README.md mit Branching-Strategie aktualisiert

## 🔧 Noch zu erledigen (auf Hetzner Server)

### 1. Datenbank-Credentials prüfen

**Auf Server ausführen:**
```bash
cd /opt/pvs

# Prüfe ob .env Datei existiert
ls -la .env

# Prüfe Dateirechte (muss 600 sein)
ls -la .env | grep -E "^-rw-------"

# Falls nicht korrekt, setze Rechte:
chmod 600 .env

# Prüfe ob alle Passwörter gesetzt sind
grep -E "POSTGRES_PASSWORD_(DEV|TEST|PROD)" .env

# Falls Passwörter fehlen, generiere neue:
openssl rand -base64 32  # Für DEV
openssl rand -base64 32  # Für TEST
openssl rand -base64 32  # Für PROD (WICHTIG: Sicher speichern!)
```

### 2. PostgreSQL Container Status prüfen

**Auf Server ausführen:**
```bash
cd /opt/pvs

# Alle Container auflisten
docker-compose -f docker-compose.production.yml ps

# Prüfe Health Status der PostgreSQL Container
docker-compose -f docker-compose.production.yml --profile dev ps postgres-dev
docker-compose -f docker-compose.production.yml --profile test ps postgres-test
docker-compose -f docker-compose.production.yml --profile prod ps postgres-prod

# Prüfe Logs bei Problemen
docker-compose -f docker-compose.production.yml --profile prod logs postgres-prod

# Starte Container falls nötig
docker-compose -f docker-compose.production.yml --profile dev up -d postgres-dev
docker-compose -f docker-compose.production.yml --profile test up -d postgres-test
docker-compose -f docker-compose.production.yml --profile prod up -d postgres-prod
```

**Erwartete Ausgabe:**
```
NAME                STATUS          PORTS
pvs-postgres-dev   Up (healthy)    127.0.0.1:5433->5432/tcp
pvs-postgres-test  Up (healthy)    127.0.0.1:5434->5432/tcp
pvs-postgres-prod  Up (healthy)    127.0.0.1:5435->5432/tcp
```

### 3. Datenbank-Verbindungen testen

**Auf Server ausführen:**
```bash
cd /opt/pvs

# Test DEV DB Verbindung
docker exec pvs-postgres-dev psql -U pvs_user -d pvs_dev -c "SELECT version();"

# Test TEST DB Verbindung
docker exec pvs-postgres-test psql -U pvs_user -d pvs_test -c "SELECT version();"

# Test PROD DB Verbindung
docker exec pvs-postgres-prod psql -U pvs_user -d pvs_prod -c "SELECT version();"

# Prüfe ob pvs-app Container sich verbinden können (falls bereits deployed)
docker exec pvs-dev curl -f http://localhost:8080/actuator/health
docker exec pvs-test curl -f http://localhost:8080/actuator/health
docker exec pvs-prod curl -f http://localhost:8080/actuator/health
```

### 4. Netzwerk-Isolation sicherstellen

**Prüfe docker-compose.production.yml:**
- Dev/Test Container sollten **KEINE** Traefik Labels haben
- Nur Prod Container sollte Traefik Labels haben
- PostgreSQL Ports (5433-5435) sollten nur auf `127.0.0.1` gebunden sein

**Prüfe aktuell auf Server:**
```bash
# Prüfe welche Container laufen
docker ps --format "table {{.Names}}\t{{.Ports}}"

# Prüfe ob Dev/Test Ports öffentlich zugänglich sind
netstat -tulpn | grep -E "(8080|8081|8082)" | grep -v "127.0.0.1"

# Erwartet: NUR Prod sollte über Traefik (Port 443) erreichbar sein
```

### 5. Firewall konfigurieren

**Auf Server ausführen:**
```bash
# Prüfe ob UFW installiert ist
sudo apt install -y ufw

# Prüfe aktuelle Firewall-Regeln
sudo ufw status verbose

# Falls nicht aktiviert, konfiguriere Firewall:
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Nur SSH, HTTP, HTTPS erlauben
sudo ufw allow 22/tcp comment 'SSH'
sudo ufw allow 80/tcp comment 'HTTP'
sudo ufw allow 443/tcp comment 'HTTPS'

# Firewall aktivieren (VORSICHT: Nur wenn SSH-Zugriff sichergestellt!)
sudo ufw enable

# Status prüfen
sudo ufw status
```

**Erwartete Ausgabe:**
```
Status: active

To                         Action      From
--                         ------      ----
22/tcp                     ALLOW       Anywhere
80/tcp                     ALLOW       Anywhere
443/tcp                    ALLOW       Anywhere
```

### 6. .env Datei absichern

**Auf Server ausführen:**
```bash
cd /opt/pvs

# Stelle sicher dass .env nicht in Git ist (sollte bereits in .gitignore sein)
# Prüfe ob .env in Git-Tracking ist:
git ls-files | grep -E "^\.env$" || echo "✅ .env nicht in Git"

# Stelle sicher dass .env nicht in Logs erscheint
# Prüfe docker-compose.production.yml: Environment-Variablen sollten über .env geladen werden
# aber NICHT in Container-Logs erscheinen

# Setze korrekte Rechte
chmod 600 .env
ls -la .env
```

### 7. GitHub Secrets prüfen

**In GitHub Repository:**
1. Gehe zu: `https://github.com/bbajor/pvs/settings/secrets/actions`
2. Prüfe folgende Secrets:

**Erforderlich:**
- ✅ `HETZNER_HOST` - IP-Adresse des Servers
- ✅ `HETZNER_USER` - SSH User (meist `root`)
- ✅ `HETZNER_SSH_KEY` - Privater SSH-Key für Deployment

**Optional (für Flyway-Validation in CI):**
- `TEST_DB_HOST` - Host für Test-DB (meist `localhost` oder Server-IP)
- `TEST_DB_NAME` - Datenbankname (z.B. `pvs_test`)
- `TEST_DB_USER` - Datenbank-User
- `TEST_DB_PASSWORD` - Datenbank-Passwort

- `PROD_DB_HOST` - Host für Prod-DB
- `PROD_DB_NAME` - Datenbankname (z.B. `pvs_prod`)
- `PROD_DB_USER` - Datenbank-User
- `PROD_DB_PASSWORD` - Datenbank-Passwort

**Hinweis:** Die Secrets `TEST_DB_*` und `PROD_DB_*` werden nur für Flyway-Migration-Validation in CI verwendet. Falls die DB nicht von GitHub Actions aus erreichbar ist, können diese Secrets weggelassen werden (dann wird der Flyway-Validation-Step übersprungen).

### 8. Deployment testen

**Dev-Deployment:**
1. In GitHub: Actions → "Deploy to Dev (Hetzner)" → "Run workflow" → Branch: `dev`
2. Warte bis Workflow durchläuft
3. Auf Server prüfen:
```bash
cd /opt/pvs
docker-compose -f docker-compose.production.yml --profile dev ps
docker-compose -f docker-compose.production.yml --profile dev logs pvs-dev
docker exec pvs-dev curl -f http://localhost:8080/actuator/health
```

**Test-Deployment:**
1. In GitHub: Actions → "Deploy to Test (Hetzner)" → "Run workflow" → Branch: `test`
2. Warte bis Workflow durchläuft
3. Auf Server prüfen:
```bash
cd /opt/pvs
docker-compose -f docker-compose.production.yml --profile test ps
docker-compose -f docker-compose.production.yml --profile test logs pvs-test
docker exec pvs-test curl -f http://localhost:8080/actuator/health

# Prüfe ob Datenbank-Verbindung funktioniert
docker exec pvs-test env | grep DATABASE
```

## 🔒 Sicherheits-Checkliste

- [ ] SSH-Key Authentication aktiviert (kein Passwort-Login)
- [ ] Firewall aktiviert (UFW) - nur Ports 22, 80, 443 offen
- [ ] Starke Passwörter in `.env` (32+ Zeichen, zufällig)
- [ ] `.env` Datei nicht in Git (in `.gitignore`)
- [ ] `.env` Dateirechte: `600` (nur Owner lesbar)
- [ ] PostgreSQL Ports (5433-5435) nur auf `127.0.0.1` gebunden
- [ ] Dev/Test Container nicht öffentlich erreichbar
- [ ] Nur Prod über Traefik/HTTPS öffentlich
- [ ] SSL/TLS aktiviert (Traefik + Let's Encrypt)
- [ ] Regelmäßige Backups eingerichtet
- [ ] Log-Rotation aktiviert

## 📝 Nächste Schritte

1. **Sofort**: Items 1-3 ausführen (Credentials, Container-Status, DB-Verbindungen)
2. **Wichtig**: Items 4-6 (Netzwerk-Isolation, Firewall, .env Absicherung)
3. **Dann**: Items 7-8 (GitHub Secrets, Deployment testen)

Nach Abschluss aller Items ist das System vollständig konfiguriert und sicher! 🎉

