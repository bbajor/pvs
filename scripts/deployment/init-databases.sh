#!/bin/bash
# Database Initialisierung Script
# Führe dieses Script aus, nachdem PostgreSQL gestartet wurde

set -e

echo "🗄️ Initialisiere Datenbanken..."

# Warte bis PostgreSQL bereit ist
echo "⏳ Warte auf PostgreSQL..."
until docker exec pvs-postgres pg_isready -U pvs_user > /dev/null 2>&1; do
    echo "   PostgreSQL ist noch nicht bereit..."
    sleep 2
done

echo "✅ PostgreSQL ist bereit!"

# Datenbanken erstellen
echo "📦 Erstelle Datenbanken..."

docker exec -i pvs-postgres psql -U pvs_user -d postgres <<EOF
CREATE DATABASE pvs_dev;
CREATE DATABASE pvs_test;
CREATE DATABASE pvs_prod;
EOF

echo ""
echo "✅ Datenbanken erstellt:"
echo "   - pvs_dev"
echo "   - pvs_test"
echo "   - pvs_prod"
echo ""
echo "📝 Nächste Schritte:"
echo "   - GitHub Secrets konfigurieren (siehe QUICKSTART.md)"
echo "   - Erste Deployment testen"

