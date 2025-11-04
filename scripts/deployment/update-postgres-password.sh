#!/bin/bash
# Script zum Update des PostgreSQL-Passworts
# Verwendung: bash scripts/deployment/update-postgres-password.sh <container-name> <username> <new-password>

set -e

CONTAINER_NAME="${1:-pvs-postgres}"
DB_USER="${2:-pvs_user}"
NEW_PASSWORD="${3}"

if [ -z "$NEW_PASSWORD" ]; then
  echo "❌ Fehler: Passwort nicht angegeben"
  echo ""
  echo "Verwendung:"
  echo "  $0 <container-name> <username> <new-password>"
  echo ""
  echo "Beispiele:"
  echo "  $0 pvs-postgres pvs_user \$(openssl rand -base64 32)"
  echo "  $0 pvs-postgres-dev pvs_user \"MeinSicheresPasswort123!\""
  exit 1
fi

echo "=========================================="
echo "🔐 PostgreSQL Passwort Update"
echo "=========================================="
echo ""
echo "Container: ${CONTAINER_NAME}"
echo "User: ${DB_USER}"
echo ""

# Prüfe ob Container läuft
if ! podman ps --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
  echo "❌ Container '${CONTAINER_NAME}' läuft nicht!"
  echo "   Starte ihn zuerst: podman start ${CONTAINER_NAME}"
  exit 1
fi

echo "✅ Container läuft"
echo ""

# Prüfe ob User existiert
echo "Prüfe ob User '${DB_USER}' existiert..."
if podman exec "${CONTAINER_NAME}" psql -U postgres -d postgres -tc "SELECT 1 FROM pg_roles WHERE rolname='${DB_USER}';" | grep -q 1; then
  echo "✅ User '${DB_USER}' existiert"
  USE_POSTGRES_USER=true
elif podman exec "${CONTAINER_NAME}" psql -U "${DB_USER}" -d postgres -tc "SELECT 1;" >/dev/null 2>&1; then
  echo "⚠️  User 'postgres' existiert nicht, aber '${DB_USER}' ist erreichbar"
  USE_POSTGRES_USER=false
else
  echo "❌ Weder 'postgres' noch '${DB_USER}' sind als Superuser verfügbar"
  echo ""
  echo "Verfügbare Users im Container:"
  podman exec "${CONTAINER_NAME}" psql -U "${DB_USER}" -d postgres -c "\du" 2>/dev/null || \
  podman exec "${CONTAINER_NAME}" psql -U postgres -d postgres -c "\du" 2>/dev/null || \
  echo "Konnte Users nicht auflisten - möglicherweise fehlende Berechtigung"
  exit 1
fi

echo ""
echo "🔄 Update Passwort für User '${DB_USER}'..."

# Update Passwort
if [ "$USE_POSTGRES_USER" = true ]; then
  # Nutze postgres Superuser
  podman exec "${CONTAINER_NAME}" psql -U postgres -d postgres -c "ALTER USER ${DB_USER} WITH PASSWORD '${NEW_PASSWORD}';"
else
  # Versuche mit dem User selbst (wenn er Superuser ist)
  podman exec "${CONTAINER_NAME}" psql -U "${DB_USER}" -d postgres -c "ALTER USER ${DB_USER} WITH PASSWORD '${NEW_PASSWORD}';"
fi

if [ $? -eq 0 ]; then
  echo "✅ Passwort erfolgreich aktualisiert!"
else
  echo "❌ Passwort-Update fehlgeschlagen"
  exit 1
fi

echo ""
echo "🧪 Teste neue Verbindung..."
if podman exec "${CONTAINER_NAME}" psql -U "${DB_USER}" -d postgres -c "SELECT current_user;" >/dev/null 2>&1; then
  echo "✅ Verbindung mit neuem Passwort erfolgreich!"
else
  echo "⚠️  Verbindungstest fehlgeschlagen - möglicherweise benötigt Container Neustart"
fi

echo ""
echo "=========================================="
echo "⚠️  WICHTIG: Nächste Schritte"
echo "=========================================="
echo ""
echo "1. Aktualisiere .env Datei auf dem Server:"
echo "   POSTGRES_PASSWORD_<STAGE>=${NEW_PASSWORD}"
echo ""
echo "2. Aktualisiere podman-compose.production.yml Environment-Variablen"
echo ""
echo "3. Starte Container neu (damit neue .env Werte geladen werden):"
echo "   podman-compose -f podman-compose.production.yml restart pvs-<stage>"
echo ""
echo "4. Falls GitHub Secrets betroffen sind, aktualisiere sie:"
echo "   https://github.com/bbajor/pvs/settings/secrets/actions"
echo ""

