# Datenbank-Credentials und GitHub Secrets prüfen

Diese Anleitung hilft dabei, die Datenbank-Credentials auf dem Hetzner Server und die GitHub Secrets zu prüfen und zu validieren.

## 🔍 Schritt 1: DB-Credentials auf Hetzner Server prüfen

### 1.1 .env Datei prüfen

**Auf dem Server ausführen:**
```bash
cd /opt/pvs

# Prüfe ob .env Datei existiert
if [ -f .env ]; then
  echo "✅ .env Datei existiert"
else
  echo "❌ .env Datei fehlt!"
  exit 1
fi

# Prüfe Dateirechte (sollte 600 sein - nur Owner kann lesen/schreiben)
ls -la .env
# Sollte zeigen: -rw------- (600)

# Falls Rechte nicht korrekt, setze sie:
chmod 600 .env
```

### 1.2 Passwörter in .env Datei prüfen

**Auf dem Server ausführen:**
```bash
cd /opt/pvs

# Prüfe ob alle benötigten Passwörter gesetzt sind
echo "=== Prüfe DEV Passwörter ==="
grep -E "^POSTGRES_PASSWORD_DEV=" .env || echo "❌ POSTGRES_PASSWORD_DEV fehlt!"

echo "=== Prüfe TEST Passwörter ==="
grep -E "^POSTGRES_PASSWORD_TEST=" .env || echo "❌ POSTGRES_PASSWORD_TEST fehlt!"

echo "=== Prüfe PROD Passwörter ==="
grep -E "^POSTGRES_PASSWORD_PROD=" .env || echo "❌ POSTGRES_PASSWORD_PROD fehlt!"

# Zeige Länge der Passwörter (sollten mindestens 32 Zeichen sein für Sicherheit)
echo ""
echo "=== Passwort-Längen (sollten >= 32 Zeichen sein) ==="
grep -E "^POSTGRES_PASSWORD_DEV=" .env | awk -F'=' '{print "DEV:  " length($2) " Zeichen"}'
grep -E "^POSTGRES_PASSWORD_TEST=" .env | awk -F'=' '{print "TEST: " length($2) " Zeichen"}'
grep -E "^POSTGRES_PASSWORD_PROD=" .env | awk -F'=' '{print "PROD: " length($2) " Zeichen"}'

# WICHTIG: Zeige die Passwörter NICHT an, nur ob sie gesetzt sind!
echo ""
echo "⚠️  Passwörter werden hier NICHT angezeigt (Sicherheit)"
```

### 1.3 Datenbank-Verbindung testen

**Hinweis:** Du hast aktuell einen Container `pvs-postgres` auf Port 5432. Prüfe zuerst, welche Datenbank das ist:

```bash
# Prüfe laufende PostgreSQL Container
docker ps | grep postgres

# Prüfe welche Umgebungsvariablen der Container hat
docker inspect pvs-postgres | grep -A 20 "Env"
```

**Dann teste die Verbindung:**

```bash
cd /opt/pvs

# Hole die Credentials aus .env (nur für Verbindungstest)
source .env

# Prüfe ob DEV-DB erreichbar ist (falls postgres-dev Container läuft)
if docker ps | grep -q "postgres-dev"; then
  echo "=== Teste DEV Datenbank-Verbindung ==="
  docker exec pvs-postgres-dev psql -U ${POSTGRES_USER_DEV:-pvs_user} -d ${POSTGRES_DB_DEV:-pvs_dev} -c "SELECT version();" 2>&1
fi

# Prüfe ob TEST-DB erreichbar ist (falls postgres-test Container läuft)
if docker ps | grep -q "postgres-test"; then
  echo "=== Teste TEST Datenbank-Verbindung ==="
  docker exec pvs-postgres-test psql -U ${POSTGRES_USER_TEST:-pvs_user} -d ${POSTGRES_DB_TEST:-pvs_test} -c "SELECT version();" 2>&1
fi

# Prüfe ob PROD-DB erreichbar ist (falls postgres-prod Container läuft)
if docker ps | grep -q "postgres-prod"; then
  echo "=== Teste PROD Datenbank-Verbindung ==="
  docker exec pvs-postgres-prod psql -U ${POSTGRES_USER_PROD:-pvs_user} -d ${POSTGRES_DB_PROD:-pvs_prod} -c "SELECT version();" 2>&1
fi

# Oder teste den aktuellen pvs-postgres Container
echo "=== Teste aktuellen pvs-postgres Container ==="
docker exec pvs-postgres psql -U postgres -d postgres -c "SELECT version();" 2>&1
```

### 1.4 Prüfe ob Credentials mit Docker Compose übereinstimmen

```bash
cd /opt/pvs

# Zeige welche Umgebungsvariablen Docker Compose für die Container setzen würde
echo "=== DEV Container Umgebungsvariablen ==="
docker-compose -f docker-compose.production.yml --profile dev config | grep -A 10 "POSTGRES_USER_DEV\|POSTGRES_PASSWORD_DEV" || echo "Nicht gefunden"

echo "=== TEST Container Umgebungsvariablen ==="
docker-compose -f docker-compose.production.yml --profile test config | grep -A 10 "POSTGRES_USER_TEST\|POSTGRES_PASSWORD_TEST" || echo "Nicht gefunden"

echo "=== PROD Container Umgebungsvariablen ==="
docker-compose -f docker-compose.production.yml --profile prod config | grep -A 10 "POSTGRES_USER_PROD\|POSTGRES_PASSWORD_PROD" || echo "Nicht gefunden"
```

## 🔐 Schritt 2: GitHub Secrets prüfen

### 2.1 Im Browser prüfen

1. **GitHub Repository öffnen:**
   ```
   https://github.com/bbajor/pvs
   ```

2. **Zu Secrets navigieren:**
   - Klicke auf **Settings** (oben im Repository)
   - Im linken Menü: **Secrets and variables** → **Actions**
   - Oder direkter Link: `https://github.com/bbajor/pvs/settings/secrets/actions`

3. **Prüfe folgende Secrets (müssen vorhanden sein):**

   **Erforderlich für Hetzner Deployment:**
   - ✅ `HETZNER_HOST` - IP-Adresse des Servers
   - ✅ `HETZNER_USER` - SSH User (meist `root`)
   - ✅ `HETZNER_SSH_KEY` - Privater SSH-Key für Deployment

   **Optional (für Flyway-Validation):**
   - `TEST_DB_HOST` - Hostname/IP der Test-DB
   - `TEST_DB_NAME` - Datenbankname (z.B. `pvs_test`)
   - `TEST_DB_USER` - Datenbank-User
   - `TEST_DB_PASSWORD` - Datenbank-Passwort

   - `PROD_DB_HOST` - Hostname/IP der Prod-DB
   - `PROD_DB_NAME` - Datenbankname (z.B. `pvs_prod`)
   - `PROD_DB_USER` - Datenbank-User
   - `PROD_DB_PASSWORD` - Datenbank-Passwort

### 2.2 GitHub Secrets via GitHub CLI prüfen (falls installiert)

```bash
# Prüfe ob gh CLI installiert ist
which gh || echo "GitHub CLI nicht installiert"

# Falls installiert, liste alle Secrets (namen, keine Werte!)
gh secret list --repo bbajor/pvs
```

### 2.3 GitHub Secrets-Werte NICHT direkt einsehen

**⚠️ WICHTIG:** GitHub Secrets können aus Sicherheitsgründen NICHT nachträglich eingesehen werden. Du kannst nur:
- Prüfen ob sie existieren (Name)
- Neue Secrets erstellen/überschreiben
- Secrets löschen

**Um zu prüfen ob Werte korrekt sind:**
- Teste die Workflows manuell (siehe unten)
- Prüfe die Workflow-Logs (werden nach Ausführung verfügbar)

## 🧪 Schritt 3: Secrets durch Test-Deployment validieren

### 3.1 Teste Hetzner-Verbindung

**Auf deinem lokalen Rechner oder via GitHub Actions:**

```bash
# Teste SSH-Verbindung zum Server (mit dem SSH-Key aus HETZNER_SSH_KEY)
ssh -i ~/.ssh/hetzner_deploy $HETZNER_USER@$HETZNER_HOST "echo '✅ SSH-Verbindung erfolgreich'"
```

**Oder teste über GitHub Actions:**
1. Gehe zu: `https://github.com/bbajor/pvs/actions`
2. Wähle: **Deploy to Dev (Hetzner)**
3. Klicke: **Run workflow** → Branch: `dev`
4. Prüfe die Logs, ob SSH-Verbindung erfolgreich ist

### 3.2 Teste Datenbank-Secrets (falls verwendet)

**Via GitHub Actions Workflow:**
- Die Workflows nutzen die Secrets automatisch
- Prüfe Workflow-Logs ob Flyway-Validation erfolgreich ist
- Falls Fehler: Secrets sind möglicherweise falsch gesetzt

## 📋 Quick-Check Script

Erstelle ein Script auf dem Server:

```bash
cat > /opt/pvs/check-credentials.sh <<'EOF'
#!/bin/bash
# Quick-Check für Credentials und Container

cd /opt/pvs

echo "=== 1. .env Datei prüfen ==="
if [ -f .env ]; then
  echo "✅ .env existiert"
  ls -la .env | grep -q "^-rw-------" && echo "✅ Rechte korrekt (600)" || echo "❌ Rechte sollten 600 sein"
else
  echo "❌ .env fehlt!"
  exit 1
fi

echo ""
echo "=== 2. Passwörter prüfen ==="
for VAR in POSTGRES_PASSWORD_DEV POSTGRES_PASSWORD_TEST POSTGRES_PASSWORD_PROD; do
  if grep -q "^${VAR}=" .env; then
    LEN=$(grep "^${VAR}=" .env | awk -F'=' '{print length($2)}')
    [ "$LEN" -ge 32 ] && echo "✅ ${VAR}: ${LEN} Zeichen" || echo "⚠️  ${VAR}: ${LEN} Zeichen (sollte >= 32 sein)"
  else
    echo "❌ ${VAR} fehlt in .env"
  fi
done

echo ""
echo "=== 3. PostgreSQL Container prüfen ==="
docker ps --format "table {{.Names}}\t{{.Status}}" | grep postgres

echo ""
echo "=== 4. Datenbank-Verbindungen testen ==="
source .env 2>/dev/null

for STAGE in dev test prod; do
  CONTAINER="pvs-postgres-${STAGE}"
  if docker ps | grep -q "$CONTAINER"; then
    echo -n "Testing $CONTAINER... "
    docker exec "$CONTAINER" pg_isready -U pvs_user >/dev/null 2>&1 && echo "✅ OK" || echo "❌ Fehler"
  fi
done

echo ""
echo "✅ Check abgeschlossen!"
EOF

chmod +x /opt/pvs/check-credentials.sh
```

**Dann ausführen:**
```bash
/opt/pvs/check-credentials.sh
```

## ⚠️ Wichtige Hinweise

1. **Passwörter NIEMALS in Logs ausgeben** - nutze das Check-Script, es zeigt nur Längen, nie Werte
2. **.env Datei NIEMALS committen** - sollte bereits in `.gitignore` sein
3. **GitHub Secrets können nicht rückgelesen werden** - nur überschreiben möglich
4. **Teste Secrets immer durch tatsächliches Deployment** - das ist der beste Validierungstest

## 🔧 Troubleshooting

### Problem: Passwort zu kurz
```bash
# Generiere neues sicheres Passwort
openssl rand -base64 32

# Füge in .env ein:
POSTGRES_PASSWORD_PROD=<generiertes_passwort>
```

### Problem: Container kann sich nicht verbinden
```bash
# Prüfe ob Container überhaupt läuft
docker ps | grep postgres

# Prüfe Logs
docker logs pvs-postgres-prod

# Prüfe Netzwerk
docker network inspect pvs_pvs-network
```

### Problem: GitHub Workflow schlägt bei SSH fehl
- Prüfe ob `HETZNER_SSH_KEY` den kompletten privaten Key enthält (inkl. `-----BEGIN OPENSSH PRIVATE KEY-----` Zeilen)
- Prüfe ob `HETZNER_USER` korrekt ist (meist `root`)
- Prüfe ob `HETZNER_HOST` die korrekte IP-Adresse ist

