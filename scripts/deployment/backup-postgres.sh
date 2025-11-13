#!/bin/bash
# PostgreSQL Backup Script für Cloud-Deployment
# Erstellt tägliche Backups mit Retention-Policy

set -e

# Konfiguration
BACKUP_DIR=${BACKUP_DIR:-/opt/pvs/backups}
RETENTION_DAYS=${RETENTION_DAYS:-30}
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Database configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-pvs_prod}
DB_USER=${DB_USER:-pvs_user}
DB_PASSWORD=${DB_PASSWORD:-}

# Backup file name
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"

echo "🗄️  PostgreSQL Backup"
echo "======================"
echo "Database: $DB_NAME"
echo "Host: $DB_HOST:$DB_PORT"
echo "Backup file: $BACKUP_FILE"
echo ""

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# Set password via environment variable
export PGPASSWORD="$DB_PASSWORD"

# Create backup
echo "📥 Creating backup..."
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
    --no-owner --no-acl --clean --if-exists \
    | gzip > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "✅ Backup created successfully: $BACKUP_FILE"
    
    # Get backup size
    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "📊 Backup size: $BACKUP_SIZE"
else
    echo "❌ Backup failed!"
    exit 1
fi

# Clean up old backups (retention policy)
echo ""
echo "🧹 Cleaning up old backups (retention: $RETENTION_DAYS days)..."
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -type f -mtime +$RETENTION_DAYS -delete
echo "✅ Cleanup completed"

# List remaining backups
echo ""
echo "📋 Remaining backups:"
ls -lh "$BACKUP_DIR"/${DB_NAME}_*.sql.gz 2>/dev/null | tail -5 || echo "No backups found"

echo ""
echo "✅ Backup process completed"


