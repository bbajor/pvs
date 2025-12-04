#!/bin/bash
# PVS OnPremise Deinstaller für Linux
# Entfernt PVS OnPremise Installation mit interaktiven Optionen

set -euo pipefail

# Farben für Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Installation-Verzeichnis
INSTALL_DIR="/opt/pvs"
SERVICE_NAME="pvs-onpremise"
SERVICE_USER="pvs"

# Tracking-Variablen
REMOVED_SERVICES=()
REMOVED_CONTAINERS=()
REMOVED_VOLUMES=()
REMOVED_DIRS=()
KEPT_DATA=()

echo -e "${BLUE}=== PVS OnPremise Deinstaller ===${NC}"
echo ""
echo -e "${YELLOW}Dieses Skript entfernt die PVS OnPremise Installation.${NC}"
echo -e "${YELLOW}WICHTIG: Datenbank-Daten können optional erhalten bleiben.${NC}"
echo ""

# Prüfe Root-Rechte
if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}Fehler: Dieses Skript muss als root ausgeführt werden${NC}"
    exit 1
fi

# Prüfe ob Installation existiert
if [ ! -d "$INSTALL_DIR" ]; then
    echo -e "${YELLOW}Installations-Verzeichnis $INSTALL_DIR nicht gefunden.${NC}"
    echo -e "${YELLOW}PVS OnPremise scheint nicht installiert zu sein.${NC}"
    exit 0
fi

echo -e "${GREEN}Installation gefunden in: $INSTALL_DIR${NC}"
echo ""

# Funktion: Ja/Nein-Abfrage
ask_yes_no() {
    local prompt="$1"
    local default="${2:-n}"
    local answer
    
    if [ "$default" = "y" ]; then
        read -p "$prompt [Y/n]: " answer
        answer="${answer:-y}"
    else
        read -p "$prompt [y/N]: " answer
        answer="${answer:-n}"
    fi
    
    case "$answer" in
        [Yy]|[Yy][Ee][Ss]) return 0 ;;
        *) return 1 ;;
    esac
}

# 1. Systemd-Service stoppen und entfernen
echo -e "${BLUE}=== 1. Systemd-Service ===${NC}"
if systemctl is-active --quiet "$SERVICE_NAME.service" 2>/dev/null; then
    echo -e "${YELLOW}Service $SERVICE_NAME ist aktiv.${NC}"
    if ask_yes_no "Service stoppen und entfernen?" "y"; then
        systemctl stop "$SERVICE_NAME.service" || true
        systemctl disable "$SERVICE_NAME.service" || true
        rm -f "/etc/systemd/system/$SERVICE_NAME.service"
        systemctl daemon-reload
        REMOVED_SERVICES+=("$SERVICE_NAME.service")
        echo -e "${GREEN}✓ Service entfernt${NC}"
    else
        echo -e "${YELLOW}⚠ Service wird nicht entfernt${NC}"
    fi
elif [ -f "/etc/systemd/system/$SERVICE_NAME.service" ]; then
    echo -e "${YELLOW}Service-Datei existiert, aber Service ist nicht aktiv.${NC}"
    if ask_yes_no "Service-Datei entfernen?" "y"; then
        rm -f "/etc/systemd/system/$SERVICE_NAME.service"
        systemctl daemon-reload
        REMOVED_SERVICES+=("$SERVICE_NAME.service (inaktiv)")
        echo -e "${GREEN}✓ Service-Datei entfernt${NC}"
    fi
else
    echo -e "${GREEN}✓ Kein Systemd-Service gefunden${NC}"
fi
echo ""

# 2. Container stoppen und entfernen
echo -e "${BLUE}=== 2. Container ===${NC}"
cd "$INSTALL_DIR" 2>/dev/null || {
    echo -e "${YELLOW}Kann nicht ins Installations-Verzeichnis wechseln.${NC}"
    echo -e "${YELLOW}Container werden übersprungen.${NC}"
    echo ""
}

if command -v podman-compose &> /dev/null && [ -f "$INSTALL_DIR/podman-compose.onpremise.yml" ]; then
    # Prüfe laufende Container
    RUNNING_CONTAINERS=$(podman ps -a --filter "name=pvs-onpremise" --format "{{.Names}}" 2>/dev/null || true)
    
    if [ -n "$RUNNING_CONTAINERS" ]; then
        echo -e "${YELLOW}Gefundene Container:${NC}"
        echo "$RUNNING_CONTAINERS" | while read -r container; do
            echo "  - $container"
        done
        echo ""
        
        if ask_yes_no "Container stoppen und entfernen?" "y"; then
            echo "Stoppe Container..."
            podman-compose -f "$INSTALL_DIR/podman-compose.onpremise.yml" down 2>/dev/null || true
            
            # Entferne einzelne Container falls noch vorhanden
            while IFS= read -r container; do
                if [ -n "$container" ]; then
                    podman stop "$container" 2>/dev/null || true
                    podman rm "$container" 2>/dev/null || true
                    REMOVED_CONTAINERS+=("$container")
                fi
            done <<< "$RUNNING_CONTAINERS"
            
            echo -e "${GREEN}✓ Container entfernt${NC}"
        else
            echo -e "${YELLOW}⚠ Container werden nicht entfernt${NC}"
            # Speichere für Zusammenfassung
            while IFS= read -r container; do
                if [ -n "$container" ]; then
                    REMOVED_CONTAINERS+=("$container (nicht entfernt)")
                fi
            done <<< "$RUNNING_CONTAINERS"
        fi
    else
        echo -e "${GREEN}✓ Keine Container gefunden${NC}"
    fi
else
    echo -e "${YELLOW}podman-compose nicht verfügbar oder Konfiguration nicht gefunden${NC}"
fi
echo ""

# 3. Volumes (Datenbank-Daten!)
echo -e "${BLUE}=== 3. Daten-Volumes ===${NC}"
echo -e "${RED}⚠️  WICHTIG: Volumes enthalten die Datenbank-Daten (inkl. IVOM-Behandlungsdaten)!${NC}"
echo ""

if command -v podman &> /dev/null; then
    VOLUMES=$(podman volume ls --filter "name=pvs-onpremise" --format "{{.Name}}" 2>/dev/null || true)
    
    if [ -n "$VOLUMES" ]; then
        echo -e "${YELLOW}Gefundene Volumes:${NC}"
        echo "$VOLUMES" | while read -r volume; do
            echo "  - $volume"
        done
        echo ""
        
        # Spezielle Warnung für Datenbank-Volumes
        DB_VOLUMES=$(echo "$VOLUMES" | grep -E "(postgres|kbv)" || true)
        if [ -n "$DB_VOLUMES" ]; then
            echo -e "${RED}⚠️  KRITISCH: Die folgenden Volumes enthalten Datenbank-Daten:${NC}"
            echo "$DB_VOLUMES" | while read -r volume; do
                echo -e "${RED}  - $volume${NC}"
            done
            echo ""
            echo -e "${YELLOW}Diese enthalten:${NC}"
            echo "  - IVOM-Behandlungsdaten"
            echo "  - Patientendaten"
            echo "  - Alle anderen Anwendungsdaten"
            echo ""
            
            if ask_yes_no "Datenbank-Volumes LÖSCHEN? (Daten gehen VERLOREN!)" "n"; then
                while IFS= read -r volume; do
                    if [ -n "$volume" ]; then
                        if podman volume rm "$volume" 2>/dev/null; then
                            REMOVED_VOLUMES+=("$volume")
                            echo -e "${GREEN}✓ Volume $volume gelöscht${NC}"
                        fi
                    fi
                done <<< "$DB_VOLUMES"
            else
                echo -e "${GREEN}✓ Datenbank-Volumes werden BEHALTEN${NC}"
                while IFS= read -r volume; do
                    if [ -n "$volume" ]; then
                        KEPT_DATA+=("$volume")
                    fi
                done <<< "$DB_VOLUMES"
            fi
            echo ""
        fi
        
        # Andere Volumes
        OTHER_VOLUMES=$(echo "$VOLUMES" | grep -vE "(postgres|kbv)" || true)
        if [ -n "$OTHER_VOLUMES" ]; then
            echo -e "${YELLOW}Weitere Volumes:${NC}"
            echo "$OTHER_VOLUMES" | while read -r volume; do
                echo "  - $volume"
            done
            echo ""
            
            if ask_yes_no "Diese Volumes entfernen?" "y"; then
                while IFS= read -r volume; do
                    if [ -n "$volume" ]; then
                        if podman volume rm "$volume" 2>/dev/null; then
                            REMOVED_VOLUMES+=("$volume")
                            echo -e "${GREEN}✓ Volume $volume gelöscht${NC}"
                        fi
                    fi
                done <<< "$OTHER_VOLUMES"
            else
                while IFS= read -r volume; do
                    if [ -n "$volume" ]; then
                        KEPT_DATA+=("$volume")
                    fi
                done <<< "$OTHER_VOLUMES"
            fi
        fi
    else
        echo -e "${GREEN}✓ Keine Volumes gefunden${NC}"
    fi
else
    echo -e "${YELLOW}Podman nicht verfügbar${NC}"
fi
echo ""

# 4. Installations-Verzeichnis
echo -e "${BLUE}=== 4. Installations-Verzeichnis ===${NC}"
echo -e "${YELLOW}Installations-Verzeichnis: $INSTALL_DIR${NC}"
echo ""

if [ -d "$INSTALL_DIR" ]; then
    # Zeige Größe des Verzeichnisses
    SIZE=$(du -sh "$INSTALL_DIR" 2>/dev/null | cut -f1 || echo "unbekannt")
    echo -e "${YELLOW}Größe: $SIZE${NC}"
    echo ""
    
    # Zeige wichtige Dateien/Verzeichnisse
    echo -e "${YELLOW}Enthält:${NC}"
    [ -f "$INSTALL_DIR/.env" ] && echo "  - .env (Konfiguration)"
    [ -d "$INSTALL_DIR/backups" ] && echo "  - backups/ (Backup-Dateien)"
    [ -f "$INSTALL_DIR/podman-compose.onpremise.yml" ] && echo "  - podman-compose.onpremise.yml"
    echo ""
    
    if ask_yes_no "Installations-Verzeichnis komplett löschen?" "y"; then
        # Frage nach Backups
        if [ -d "$INSTALL_DIR/backups" ] && [ "$(ls -A $INSTALL_DIR/backups 2>/dev/null)" ]; then
            echo ""
            echo -e "${YELLOW}Backup-Verzeichnis enthält Dateien:${NC}"
            ls -lh "$INSTALL_DIR/backups" | tail -n +2 || true
            echo ""
            if ask_yes_no "Backup-Verzeichnis auch löschen?" "n"; then
                BACKUP_ACTION="delete"
            else
                BACKUP_ACTION="keep"
            fi
        fi
        
        # Lösche Verzeichnis
        if [ "$BACKUP_ACTION" = "keep" ] && [ -d "$INSTALL_DIR/backups" ]; then
            # Verschiebe Backups temporär
            TEMP_BACKUP="/tmp/pvs-backups-$(date +%s)"
            mv "$INSTALL_DIR/backups" "$TEMP_BACKUP" 2>/dev/null || true
            rm -rf "$INSTALL_DIR"
            mkdir -p "$(dirname $INSTALL_DIR)"
            mv "$TEMP_BACKUP" "$INSTALL_DIR/backups" 2>/dev/null || true
            echo -e "${GREEN}✓ Installations-Verzeichnis gelöscht (Backups behalten)${NC}"
            KEPT_DATA+=("$INSTALL_DIR/backups")
        else
            rm -rf "$INSTALL_DIR"
            REMOVED_DIRS+=("$INSTALL_DIR")
            echo -e "${GREEN}✓ Installations-Verzeichnis gelöscht${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ Installations-Verzeichnis wird BEHALTEN${NC}"
        KEPT_DATA+=("$INSTALL_DIR")
    fi
else
    echo -e "${GREEN}✓ Installations-Verzeichnis existiert nicht${NC}"
fi
echo ""

# 5. Service-User (optional)
echo -e "${BLUE}=== 5. Service-User ===${NC}"
if id "$SERVICE_USER" &>/dev/null; then
    echo -e "${YELLOW}Service-User '$SERVICE_USER' existiert.${NC}"
    if ask_yes_no "Service-User entfernen?" "n"; then
        # Prüfe ob User noch verwendet wird
        if [ -d "$INSTALL_DIR" ] && [ "$(stat -c '%U' "$INSTALL_DIR" 2>/dev/null)" = "$SERVICE_USER" ]; then
            echo -e "${YELLOW}⚠ User besitzt noch Dateien. Wird nicht entfernt.${NC}"
        else
            userdel -r "$SERVICE_USER" 2>/dev/null || userdel "$SERVICE_USER" 2>/dev/null || true
            REMOVED_SERVICES+=("User: $SERVICE_USER")
            echo -e "${GREEN}✓ Service-User entfernt${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ Service-User wird BEHALTEN${NC}"
    fi
else
    echo -e "${GREEN}✓ Service-User existiert nicht${NC}"
fi
echo ""

# Zusammenfassung
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}=== Deinstallations-Zusammenfassung ===${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${GREEN}✓ Entfernt:${NC}"
if [ ${#REMOVED_SERVICES[@]} -gt 0 ]; then
    echo -e "${GREEN}  Services:${NC}"
    for item in "${REMOVED_SERVICES[@]}"; do
        echo "    - $item"
    done
fi

if [ ${#REMOVED_CONTAINERS[@]} -gt 0 ]; then
    echo -e "${GREEN}  Container:${NC}"
    for item in "${REMOVED_CONTAINERS[@]}"; do
        echo "    - $item"
    done
fi

if [ ${#REMOVED_VOLUMES[@]} -gt 0 ]; then
    echo -e "${GREEN}  Volumes:${NC}"
    for item in "${REMOVED_VOLUMES[@]}"; do
        echo "    - $item"
    done
fi

if [ ${#REMOVED_DIRS[@]} -gt 0 ]; then
    echo -e "${GREEN}  Verzeichnisse:${NC}"
    for item in "${REMOVED_DIRS[@]}"; do
        echo "    - $item"
    done
fi

if [ ${#REMOVED_SERVICES[@]} -eq 0 ] && [ ${#REMOVED_CONTAINERS[@]} -eq 0 ] && [ ${#REMOVED_VOLUMES[@]} -eq 0 ] && [ ${#REMOVED_DIRS[@]} -eq 0 ]; then
    echo -e "${YELLOW}  (nichts entfernt)${NC}"
fi

echo ""

if [ ${#KEPT_DATA[@]} -gt 0 ]; then
    echo -e "${YELLOW}⚠ Behalten (noch vorhanden):${NC}"
    for item in "${KEPT_DATA[@]}"; do
        echo "    - $item"
    done
    echo ""
    echo -e "${YELLOW}Hinweis: Diese Daten können manuell entfernt werden:${NC}"
    for item in "${KEPT_DATA[@]}"; do
        if [[ "$item" == *"postgres"* ]] || [[ "$item" == *"kbv"* ]]; then
            echo -e "${YELLOW}  Volume: podman volume rm $item${NC}"
        elif [ -d "$item" ]; then
            echo -e "${YELLOW}  Verzeichnis: rm -rf $item${NC}"
        fi
    done
else
    echo -e "${GREEN}✓ Keine Daten behalten${NC}"
fi

echo ""
echo -e "${BLUE}========================================${NC}"
echo ""

# Finale Prüfung
REMAINING=$(podman ps -a --filter "name=pvs-onpremise" --format "{{.Names}}" 2>/dev/null | wc -l || echo "0")
if [ "$REMAINING" -gt 0 ]; then
    echo -e "${YELLOW}⚠ Es sind noch Container vorhanden.${NC}"
    echo -e "${YELLOW}  Prüfe mit: podman ps -a --filter 'name=pvs-onpremise'${NC}"
fi

REMAINING_VOLUMES=$(podman volume ls --filter "name=pvs-onpremise" --format "{{.Name}}" 2>/dev/null | wc -l || echo "0")
if [ "$REMAINING_VOLUMES" -gt 0 ]; then
    echo -e "${YELLOW}⚠ Es sind noch Volumes vorhanden.${NC}"
    echo -e "${YELLOW}  Prüfe mit: podman volume ls --filter 'name=pvs-onpremise'${NC}"
fi

if [ -d "$INSTALL_DIR" ]; then
    echo -e "${YELLOW}⚠ Installations-Verzeichnis existiert noch: $INSTALL_DIR${NC}"
fi

echo ""
echo -e "${GREEN}Deinstallation abgeschlossen!${NC}"

