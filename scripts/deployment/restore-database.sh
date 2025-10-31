#!/bin/bash
# Agent 5: Database Restore Script
# Production PostgreSQL Restore mit Decryption

set -e  # Exit on error

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/backups/prod}"
DB_HOST="${DATABASE_HOST:-postgres-prod}"
DB_PORT="${DATABASE_PORT:-5432}"
DB_NAME="${POSTGRES_DB_PROD:-pvs_prod}"
DB_USER="${POSTGRES_USER_PROD:-pvs_user}"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${RED}=== PVS Database Restore ===${NC}"
echo -e "${YELLOW}⚠ WARNING: This will OVERWRITE the current database!${NC}"

# Check for backup file argument
if [ -z "$1" ]; then
    echo -e "\n${YELLOW}Available backups:${NC}"
    ls -lht "$BACKUP_DIR"/pvs_backup_*.sql* | head -10
    echo ""
    echo "Usage: $0 <backup-file>"
    echo "Example: $0 $BACKUP_DIR/pvs_backup_20251030_120000.sql.gz"
    exit 1
fi

BACKUP_FILE="$1"

# Check if backup file exists
if [ ! -f "$BACKUP_FILE" ]; then
    echo -e "${RED}✗ Backup file not found: $BACKUP_FILE${NC}"
    exit 1
fi

echo "Backup File: $BACKUP_FILE"
echo "Database: $DB_NAME"

# Confirm restore
read -p "Are you sure you want to restore? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "Restore cancelled."
    exit 0
fi

# 1. Decrypt if encrypted
if [[ "$BACKUP_FILE" == *.gpg ]]; then
    echo -e "\n${YELLOW}[1/4] Decrypting backup...${NC}"
    DECRYPTED_FILE="${BACKUP_FILE%.gpg}"
    gpg --decrypt --output "$DECRYPTED_FILE" "$BACKUP_FILE"
    
    if [ $? -eq 0 ]; then
        BACKUP_FILE="$DECRYPTED_FILE"
        echo -e "${GREEN}✓ Backup decrypted${NC}"
    else
        echo -e "${RED}✗ Decryption failed!${NC}"
        exit 1
    fi
else
    echo -e "\n${YELLOW}[1/4] Skipping decryption (not encrypted)${NC}"
fi

# 2. Decompress if gzipped
if [[ "$BACKUP_FILE" == *.gz ]]; then
    echo -e "\n${YELLOW}[2/4] Decompressing backup...${NC}"
    gunzip -c "$BACKUP_FILE" > "${BACKUP_FILE%.gz}"
    BACKUP_FILE="${BACKUP_FILE%.gz}"
    echo -e "${GREEN}✓ Backup decompressed${NC}"
else
    echo -e "\n${YELLOW}[2/4] Skipping decompression (not compressed)${NC}"
fi

# 3. Drop existing database (optional - dangerous!)
echo -e "\n${YELLOW}[3/4] Preparing database...${NC}"
# PGPASSWORD="${DATABASE_PASSWORD}" dropdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" --if-exists "$DB_NAME"
# PGPASSWORD="${DATABASE_PASSWORD}" createdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME"
echo -e "${YELLOW}⚠ Skipping drop/create (uncomment in script if needed)${NC}"

# 4. Restore Database
echo -e "\n${YELLOW}[4/4] Restoring database...${NC}"
PGPASSWORD="${DATABASE_PASSWORD}" pg_restore \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    --clean \
    --if-exists \
    "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Database restored successfully!${NC}"
else
    echo -e "${RED}✗ Database restore failed!${NC}"
    exit 1
fi

# Cleanup temp files
rm -f "$DECRYPTED_FILE" 2>/dev/null || true

echo -e "\n${GREEN}=== Restore Complete ===${NC}"
echo "Restored from: $(basename $1)"
