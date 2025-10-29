# PostgreSQL User und Passwort-Problem beheben

Wenn du die Fehlermeldung `FATAL: role "postgres" does not exist` bekommst, bedeutet das, dass der PostgreSQL Container mit einem benutzerdefinierten User erstellt wurde (nicht dem Standard `postgres` User).

## Problem-Diagnose

### 1. Welcher User existiert tatsächlich?

```bash
# Auf Hetzner Server:
cd /opt/pvs

# Prüfe Container-Konfiguration
docker inspect pvs-postgres | grep -E "POSTGRES_USER|POSTGRES_DB"

# Oder prüfe Environment-Variablen
docker exec pvs-postgres env | grep POSTGRES

# Versuche verschiedene User-Namen
docker exec pvs-postgres psql -U pvs_user -d postgres -c "SELECT current_user;" 2>&1
docker exec pvs-postgres psql -U postgres -d postgres -c "SELECT current_user;" 2>&1
```

### 2. Verwende den korrekten User

**Wenn der User `pvs_user` ist:**
```bash
# Statt:
docker exec pvs-postgres psql -U postgres -d postgres -c "SELECT version();"

# Verwende:
docker exec pvs-postgres psql -U pvs_user -d postgres -c "SELECT version();"
```

## Lösung: Passwort aktualisieren

### Schritt 1: Prüfe aktuellen User

```bash
cd /opt/pvs

# Zeige Container-Environments
docker inspect pvs-postgres | grep -A 30 "Env" | grep POSTGRES
```

### Schritt 2: Prüfe .env Datei

```bash
cat .env | grep POSTGRES
```

### Schritt 3: Update Passwort mit Script

**Option A: Mit Script (empfohlen)**
```bash
# Generiere neues sicheres Passwort
NEW_PASSWORD=$(openssl rand -base64 32)

# Kopiere Script auf Server (falls nicht vorhanden)
# Oder nutze direkt:
docker exec pvs-postgres psql -U pvs_user -d postgres -c "ALTER USER pvs_user WITH PASSWORD '${NEW_PASSWORD}';"

# Teste Verbindung
docker exec pvs-postgres psql -U pvs_user -d postgres -c "SELECT version();"
```

**Option B: Mit check-credentials Script**
```bash
# Das Script zeigt dir, welche User tatsächlich existieren
bash scripts/deployment/check-credentials.sh
```

### Schritt 4: Aktualisiere .env Datei

```bash
cd /opt/pvs

# Öffne .env
nano .env

# Aktualisiere das Passwort (ersetze <NEW_PASSWORD>):
POSTGRES_PASSWORD_<STAGE>=<NEW_PASSWORD>

# Speichere und schließe (Strg+X, dann Y, dann Enter)
```

### Schritt 5: Starte Container neu

```bash
# Damit neue .env Werte geladen werden
docker-compose -f docker-compose.production.yml restart pvs-postgres-<stage>

# Oder falls einzeln gestartet:
docker restart pvs-postgres
```

## Migration: Alten Container zu neuem Setup

Falls du einen alten `pvs-postgres` Container hast und zu `pvs-postgres-dev/test/prod` migrieren willst:

### Schritt 1: Backup erstellen

```bash
# Backup der alten Datenbank
docker exec pvs-postgres pg_dump -U pvs_user -d pvs_prod > /opt/pvs/backup_old.sql

# Oder für alle Datenbanken:
docker exec pvs-postgres pg_dumpall -U pvs_user > /opt/pvs/backup_all.sql
```

### Schritt 2: Neue Container mit .env starten

```bash
cd /opt/pvs

# Stelle sicher, dass .env korrekt ist
cat .env | grep POSTGRES

# Starte neue Container
docker-compose -f docker-compose.production.yml --profile prod up -d postgres-prod
```

### Schritt 3: Daten migrieren

```bash
# Restore auf neuen Container
docker exec -i pvs-postgres-prod psql -U pvs_user -d pvs_prod < /opt/pvs/backup_old.sql
```

### Schritt 4: Alten Container stoppen (nach erfolgreichem Test)

```bash
docker stop pvs-postgres
docker rm pvs-postgres  # Optional: nur wenn sicher
```

## Quick-Check: Welcher User wird verwendet?

```bash
# Auf dem Server:
cd /opt/pvs

# Script zeigt dir alle verfügbaren User
cat > /tmp/check-pg-users.sh <<'EOF'
#!/bin/bash
CONTAINER="${1:-pvs-postgres}"

echo "=== PostgreSQL User Check ==="
echo "Container: ${CONTAINER}"
echo ""

# Zeige Environment
echo "Environment Variablen:"
docker inspect "${CONTAINER}" | grep -E "POSTGRES_USER|POSTGRES_DB" | head -5
echo ""

# Versuche verschiedene Users
for USER in postgres pvs_user admin root; do
  echo -n "Testing User '${USER}'... "
  if docker exec "${CONTAINER}" psql -U "${USER}" -d postgres -c "SELECT current_user;" >/dev/null 2>&1; then
    echo "✅ Funktioniert"
  else
    echo "❌ Fehler"
  fi
done

echo ""
echo "Alle User im Container:"
docker exec "${CONTAINER}" psql -U pvs_user -d postgres -c "\du" 2>/dev/null || \
docker exec "${CONTAINER}" psql -U postgres -d postgres -c "\du" 2>/dev/null || \
echo "Konnte keine User auflisten"
EOF

chmod +x /tmp/check-pg-users.sh
/tmp/check-pg-users.sh pvs-postgres
```

## Häufige Probleme

### Problem: "password authentication failed"

**Lösung:**
1. Prüfe ob Passwort in `.env` korrekt ist
2. Restarte Container, damit .env neu geladen wird
3. Prüfe ob Passwort in Container-Environment übereinstimmt

### Problem: "role does not exist"

**Lösung:**
- Verwende den korrekten User-Namen (nicht immer `postgres`)
- Prüfe mit: `docker inspect <container> | grep POSTGRES_USER`

### Problem: Container zeigt falsches Passwort

**Lösung:**
```bash
# Prüfe aktuelles Passwort im Container
docker exec pvs-postgres env | grep POSTGRES_PASSWORD

# Falls falsch, starte Container neu mit korrektem .env
docker-compose -f docker-compose.production.yml --profile prod down
docker-compose -f docker-compose.production.yml --profile prod up -d
```

## Nächste Schritte

Nach dem Passwort-Update:

1. ✅ Teste Verbindung mit neuem Passwort
2. ✅ Aktualisiere `.env` Datei
3. ✅ Restarte betroffene Container
4. ✅ Aktualisiere GitHub Secrets (falls verwendet)
5. ✅ Teste Deployment über GitHub Actions

