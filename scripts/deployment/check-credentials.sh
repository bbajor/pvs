#!/bin/bash
# Quick-Check Script für DB-Credentials und Container-Status
# Ausführung: bash scripts/deployment/check-credentials.sh

set -e  # Bei Fehler abbrechen

cd "$(dirname "$0")/../.." || cd /opt/pvs

echo "=========================================="
echo "🔍 DB-Credentials und Container Check"
echo "=========================================="
echo ""

# 1. .env Datei prüfen
echo "=== 1. .env Datei prüfen ==="
if [ -f .env ]; then
  echo "✅ .env existiert in $(pwd)"
  
  # Prüfe Dateirechte
  PERMS=$(stat -c "%a" .env 2>/dev/null || stat -f "%OLp" .env 2>/dev/null)
  if [ "$PERMS" = "600" ]; then
    echo "✅ Rechte korrekt (600)"
  else
    echo "⚠️  Rechte: $PERMS (sollten 600 sein)"
    echo "   Tipp: chmod 600 .env"
  fi
else
  echo "❌ .env fehlt in $(pwd)!"
  echo "   Erstelle sie mit: nano .env"
  exit 1
fi

echo ""

# 2. Passwörter prüfen
echo "=== 2. Passwörter in .env prüfen ==="
REQUIRED_VARS=(
  "POSTGRES_PASSWORD_DEV"
  "POSTGRES_PASSWORD_TEST"
  "POSTGRES_PASSWORD_PROD"
  "POSTGRES_USER_DEV"
  "POSTGRES_USER_TEST"
  "POSTGRES_USER_PROD"
  "POSTGRES_DB_DEV"
  "POSTGRES_DB_TEST"
  "POSTGRES_DB_PROD"
)

MISSING=0
for VAR in "${REQUIRED_VARS[@]}"; do
  if grep -q "^${VAR}=" .env; then
    if [[ "$VAR" == *"PASSWORD"* ]]; then
      # Zeige nur Länge für Passwörter
      LEN=$(grep "^${VAR}=" .env | awk -F'=' '{print length($2)}')
      if [ "$LEN" -ge 32 ]; then
        echo "✅ ${VAR}: ${LEN} Zeichen (sicher)"
      else
        echo "⚠️  ${VAR}: ${LEN} Zeichen (sollte >= 32 sein)"
      fi
    else
      # Zeige Wert für andere Variablen
      VALUE=$(grep "^${VAR}=" .env | cut -d'=' -f2)
      echo "✅ ${VAR}: ${VALUE}"
    fi
  else
    echo "❌ ${VAR} fehlt in .env"
    MISSING=1
  fi
done

if [ "$MISSING" -eq 1 ]; then
  echo ""
  echo "⚠️  Einige Variablen fehlen. Beispiel .env Inhalt:"
  echo ""
  echo "POSTGRES_DB_DEV=pvs_dev"
  echo "POSTGRES_USER_DEV=pvs_user"
  echo "POSTGRES_PASSWORD_DEV=$(openssl rand -base64 32)"
  echo ""
  echo "POSTGRES_DB_TEST=pvs_test"
  echo "POSTGRES_USER_TEST=pvs_user"
  echo "POSTGRES_PASSWORD_TEST=$(openssl rand -base64 32)"
  echo ""
  echo "POSTGRES_DB_PROD=pvs_prod"
  echo "POSTGRES_USER_PROD=pvs_user"
  echo "POSTGRES_PASSWORD_PROD=$(openssl rand -base64 32)"
fi

echo ""

# 3. PostgreSQL Container prüfen
echo "=== 3. PostgreSQL Container Status ==="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | head -1
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep postgres || echo "⚠️  Keine PostgreSQL Container gefunden"

echo ""

# 4. Docker Compose Datei prüfen
echo "=== 4. Docker Compose Konfiguration ==="
if [ -f docker-compose.production.yml ]; then
  echo "✅ docker-compose.production.yml gefunden"
  
  # Prüfe welche Profile definiert sind
  echo "Verfügbare Profile:"
  grep -E "^  [a-z-]+:" docker-compose.production.yml | grep -E "profiles:" -A 5 | grep -E "^\s+-" | sed 's/^[ ]*- /  - /' || echo "  Standard-Profile (keine expliziten Profile)"
else
  echo "❌ docker-compose.production.yml nicht gefunden"
fi

echo ""

# 5. Datenbank-Verbindungen testen
echo "=== 5. Datenbank-Verbindungen testen ==="

# Lade .env Variablen
if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

# Prüfe jeden Container
for STAGE in dev test prod; do
  CONTAINER="pvs-postgres-${STAGE}"
  DB_USER_VAR="POSTGRES_USER_${STAGE^^}"
  DB_NAME_VAR="POSTGRES_DB_${STAGE^^}"
  
  DB_USER="${!DB_USER_VAR:-pvs_user}"
  DB_NAME="${!DB_NAME_VAR:-pvs_${STAGE}}"
  
  if docker ps --format "{{.Names}}" | grep -q "^${CONTAINER}$"; then
    echo -n "Testing ${CONTAINER} (${DB_USER}@${DB_NAME})... "
    
    # Teste Verbindung
    if docker exec "${CONTAINER}" pg_isready -U "${DB_USER}" >/dev/null 2>&1; then
      echo "✅ OK"
      
      # Teste Query
      if docker exec "${CONTAINER}" psql -U "${DB_USER}" -d "${DB_NAME}" -c "SELECT version();" >/dev/null 2>&1; then
        echo "   └─ Query erfolgreich"
      else
        echo "   └─ ⚠️  Query fehlgeschlagen (möglicherweise DB existiert noch nicht)"
      fi
    else
      echo "❌ Fehler"
      echo "   └─ Container läuft, aber pg_isready schlägt fehl"
    fi
  else
    echo "⚠️  ${CONTAINER} läuft nicht"
    echo "   └─ Starte mit: docker-compose -f docker-compose.production.yml --profile ${STAGE} up -d postgres-${STAGE}"
  fi
done

# Prüfe auch den aktuellen pvs-postgres Container (falls vorhanden)
if docker ps --format "{{.Names}}" | grep -q "^pvs-postgres$"; then
  echo ""
  echo "=== Info: Alten pvs-postgres Container gefunden ==="
  echo "⚠️  Du hast einen Container 'pvs-postgres' (nicht pvs-postgres-dev/test/prod)"
  echo "    Prüfe ob das der richtige ist oder ob du die neuen Container starten solltest"
  docker inspect pvs-postgres | grep -E "POSTGRES_DB|POSTGRES_USER" | head -5
fi

echo ""
echo "=========================================="
echo "✅ Check abgeschlossen!"
echo "=========================================="
echo ""
echo "📝 Nächste Schritte:"
echo "   1. Prüfe GitHub Secrets: https://github.com/bbajor/pvs/settings/secrets/actions"
echo "   2. Teste Deployment über GitHub Actions"
echo "   3. Siehe auch: docs/deployment/VERIFY_CREDENTIALS.md"
echo ""

