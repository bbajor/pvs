# Troubleshooting - IVOMPlaner On-Premise

Diese Hinweise beziehen sich auf die native Installation ohne Container-Runtime.

## Service startet nicht

### Status und Logs pruefen

```bash
sudo systemctl status pvs-onpremise
sudo journalctl -u pvs-onpremise -n 100 --no-pager
```

Haeufige Ursachen:

- `/opt/pvs/app/pvs-app.jar` fehlt oder gehoert nicht dem User `pvs`
- Java 21 ist nicht installiert
- PostgreSQL laeuft nicht
- `.env` enthaelt falsche Datenbank-Zugangsdaten
- Port `8080` ist bereits belegt

### Java-Version pruefen

```bash
java -version
```

Erwartet wird Java 21. Auf Ubuntu/Debian:

```bash
sudo apt-get install -y openjdk-21-jre-headless
```

## Datenbank-Probleme

### PostgreSQL laeuft nicht

```bash
sudo systemctl status postgresql
sudo systemctl start postgresql
```

### Verbindung testen

```bash
source /opt/pvs/.env
PGPASSWORD="$DB_PASSWORD" psql -h 127.0.0.1 -U "$DB_USER" -d "$POSTGRES_DB" -c "select 1;"
```

### Passwort neu setzen

```bash
source /opt/pvs/.env
sudo -u postgres psql -c "ALTER USER \"$DB_USER\" WITH PASSWORD '$DB_PASSWORD';"
sudo systemctl restart pvs-onpremise
```

## Migrationen schlagen fehl

Flyway validiert die Datenbank beim Start. Details stehen im Journal:

```bash
sudo journalctl -u pvs-onpremise -n 200 --no-pager | grep -i flyway
```

Vor riskanten Eingriffen immer ein Backup erstellen:

```bash
sudo /opt/pvs/backup.sh
```

## Anwendung nicht erreichbar

### Port pruefen

```bash
ss -tulpn | grep ':8080'
curl -f http://127.0.0.1:8080/actuator/health
```

Wenn der Port belegt ist, `PORT` in `/opt/pvs/.env` aendern und neu starten:

```bash
sudo systemctl restart pvs-onpremise
```

### Firewall pruefen

```bash
sudo ufw status
sudo firewall-cmd --list-all
```

## Backup und Restore

### Backup erstellen

```bash
sudo /opt/pvs/backup.sh
```

### Backup wiederherstellen

```bash
sudo systemctl stop pvs-onpremise
sudo /opt/pvs/restore.sh /opt/pvs/backups/pvs_backup_<timestamp>.dump
sudo systemctl start pvs-onpremise
```

## Updates schlagen fehl

`update.sh` erstellt vor dem Austausch des JARs ein Backup und bewahrt das vorherige JAR unter `/opt/pvs/releases` auf.

```bash
sudo /opt/pvs/update.sh /path/to/pvs-app.jar
sudo journalctl -u pvs-onpremise -n 100 --no-pager
```

Manueller Rollback:

```bash
sudo systemctl stop pvs-onpremise
sudo install -o pvs -g pvs -m 0644 /opt/pvs/releases/<previous>.jar /opt/pvs/app/pvs-app.jar
sudo systemctl start pvs-onpremise
```

## SMTP-Probleme

1. `.env` pruefen:
   ```bash
   sudo sed -n '/^SMTP_/p' /opt/pvs/.env
   ```
2. `SMTP_ENCRYPTION_KEY` muss gesetzt sein, wenn SMTP-Passwoerter verschluesselt gespeichert werden.
3. Logs pruefen:
   ```bash
   sudo journalctl -u pvs-onpremise -n 200 --no-pager | grep -i smtp
   ```

## Lokaler Whisper-Service

Der native On-Premise-Pfad verwaltet Whisper nicht automatisch. Wenn Spracherkennung genutzt wird:

- `AI_WHISPER_LOCAL_ENABLED=true` setzen
- lokalen Whisper-kompatiblen Service auf `AI_WHISPER_LOCAL_HOST:AI_WHISPER_LOCAL_PORT` bereitstellen
- IVOMPlaner neu starten

## Zu wenig Speicherplatz

```bash
df -h
sudo journalctl --vacuum-time=14d
```

Alte Release-JARs und Backups koennen nach der eigenen Retention geloescht werden.

## Support-Informationen sammeln

```bash
uname -a
java -version
systemctl status pvs-onpremise --no-pager
systemctl status postgresql --no-pager
journalctl -u pvs-onpremise -n 200 --no-pager > pvs-service.log
```
