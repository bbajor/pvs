#!/bin/bash
# Agent 5: Database Backup Script
# Production PostgreSQL Backup mit Encryption

set -e  # Exit on error

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/backups/prod}"
DB_HOST="${DATABASE_HOST:-postgres-prod}"
DB_PORT="${DATABASE_PORT:-5432}"
DB_NAME="${POSTGRES_DB_PROD:-pvs_prod}"
DB_USER="${POSTGRES_USER_PROD:-pvs_user}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/pvs_backup_$TIMESTAMP.sql"
ENCRYPTED_FILE="$BACKUP_FILE.gpg"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== PVS Database Backup ===${NC}"
echo "Timestamp: $TIMESTAMP"
echo "Database: $DB_NAME"
echo "Backup Dir: $BACKUP_DIR"

# Create backup directory if not exists
mkdir -p "$BACKUP_DIR"

# 1. Dump Database
echo -e "\n${YELLOW}[1/4] Dumping database...${NC}"
PGPASSWORD="${DATABASE_PASSWORD}" pg_dump \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -F c \
    -f "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo -e "${GREEN}✓ Database dumped: $BACKUP_FILE ($BACKUP_SIZE)${NC}"
else
    echo -e "${RED}✗ Database dump failed!${NC}"
    exit 1
fi

# 2. Compress (if not already compressed by -F c)
echo -e "\n${YELLOW}[2/4] Compressing backup...${NC}"
gzip -f "$BACKUP_FILE"
BACKUP_FILE="$BACKUP_FILE.gz"
COMPRESSED_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo -e "${GREEN}✓ Backup compressed: $BACKUP_FILE ($COMPRESSED_SIZE)${NC}"

# 3. Encrypt (optional - GPG)
if [ -n "$BACKUP_GPG_KEY" ]; then
    echo -e "\n${YELLOW}[3/4] Encrypting backup...${NC}"
    gpg --encrypt --recipient "$BACKUP_GPG_KEY" --output "$ENCRYPTED_FILE" "$BACKUP_FILE"
    
    if [ $? -eq 0 ]; then
        rm "$BACKUP_FILE"  # Remove unencrypted
        BACKUP_FILE="$ENCRYPTED_FILE"
        echo -e "${GREEN}✓ Backup encrypted: $ENCRYPTED_FILE${NC}"
    else
        echo -e "${YELLOW}⚠ Encryption failed - backup remains unencrypted${NC}"
    fi
else
    echo -e "\n${YELLOW}[3/4] Skipping encryption (BACKUP_GPG_KEY not set)${NC}"
fi

# 4. Cleanup old backups
echo -e "\n${YELLOW}[4/4] Cleaning up old backups (>$RETENTION_DAYS days)...${NC}"
find "$BACKUP_DIR" -name "pvs_backup_*.sql*" -type f -mtime +$RETENTION_DAYS -delete
DELETED_COUNT=$(find "$BACKUP_DIR" -name "pvs_backup_*.sql*" -type f -mtime +$RETENTION_DAYS | wc -l)
echo -e "${GREEN}✓ Deleted $DELETED_COUNT old backup(s)${NC}"

# Summary
echo -e "\n${GREEN}=== Backup Complete ===${NC}"
echo "Backup File: $BACKUP_FILE"
echo "Size: $(du -h "$BACKUP_FILE" | cut -f1)"
echo "Retention: $RETENTION_DAYS days"

# List recent backups
echo -e "\nRecent backups:"
ls -lht "$BACKUP_DIR"/pvs_backup_*.sql* | head -5
