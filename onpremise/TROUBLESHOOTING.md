# Troubleshooting - IVOMPlaner On-Premise

Diese Hinweise beziehen sich auf die native Installation unter Linux mit systemd.

## Service startet nicht

```bash
sudo systemctl status ivomplaner --no-pager
sudo journalctl -u ivomplaner -n 100 --no-pager
```

Haeufige Ursachen:

- `/opt/ivomplaner/current/app/pvs-app.jar` fehlt.
- Java 21 ist nicht installiert.
- PostgreSQL laeuft nicht.
- `/etc/ivomplaner/ivomplaner.env` enthaelt falsche Datenbank-Zugangsdaten.
- Port `8080` ist bereits belegt.

## Datenbank-Verbindung pruefen

```bash
sudo set -a
. /etc/ivomplaner/ivomplaner.env
sudo set +a
PGPASSWORD="$DB_PASSWORD" psql -h 127.0.0.1 -U "$DB_USER" -d "$POSTGRES_DB" -c "select 1;"
```

Passwort neu setzen:

```bash
. /etc/ivomplaner/ivomplaner.env
sudo -u postgres psql -c "ALTER USER \"$DB_USER\" WITH PASSWORD '$DB_PASSWORD';"
sudo systemctl restart ivomplaner
```

## Migrationen schlagen fehl

```bash
sudo journalctl -u ivomplaner -n 200 --no-pager | grep -i flyway
```

Vor Reparaturen immer Backup erstellen:

```bash
sudo ivomplaner-backup
```

Bei fehlgeschlagenen Flyway-Migrationen nicht blind `repair` ausfuehren. Erst Ursache klaeren, Backup pruefen, dann gezielt handeln. Datenbank-Migrationen haben Humor, aber selten Rueckwaertsgang.

## Port belegt

```bash
sudo ss -ltnp | grep ':8080'
```

Wenn der Port belegt ist, `PORT` in `/etc/ivomplaner/ivomplaner.env` aendern und neu starten:

```bash
sudo systemctl restart ivomplaner
```

## Backup und Restore

Backup:

```bash
sudo ivomplaner-backup
```

Restore:

```bash
sudo ivomplaner-restore /opt/ivomplaner/backups/pvs_<timestamp>.dump
```

Restore stoppt den Service, spielt den Dump ein und startet den Service danach wieder.

## Update fehlgeschlagen

`ivomplaner-update` erstellt vor jedem Update ein Datenbank-Backup und schaltet Releases ueber `/opt/ivomplaner/current` um.

```bash
sudo ivomplaner-update latest
sudo journalctl -u ivomplaner -n 100 --no-pager
```

Wenn der Healthcheck fehlschlaegt, setzt das Skript den Symlink automatisch auf das vorherige Release zurueck. Falls eine Datenbankmigration bereits gelaufen ist und ein echter Datenbank-Rollback noetig wird: Backup wiederherstellen.

## Update aus der App startet nicht

Die App startet Updates ueber `sudo -n /usr/local/bin/ivomplaner-update-wrapper latest`. Der Installer legt dafuer eine eng begrenzte sudoers-Regel an.

Pruefung:

```bash
sudo -u ivomplaner sudo -n /usr/local/bin/ivomplaner-update-wrapper latest
sudo ls -l /var/log/ivomplaner/
sudo journalctl -u 'ivomplaner-update-*' -n 100 --no-pager
```

Wenn die App "Update konnte nicht gestartet werden" meldet, pruefe:

- `/etc/sudoers.d/ivomplaner-update` existiert und enthaelt nur den Wrapper.
- `APP_UPDATE_ENABLED=true` in `/etc/ivomplaner/ivomplaner.env`.
- `APP_UPDATE_LATEST_VERSION_URL` ist erreichbar.
- Der Server hat ausgehenden HTTPS-Zugriff auf GitHub Releases.

## Manuelles Release-Rollback

```bash
sudo ls -1 /opt/ivomplaner/releases
sudo ln -sfn /opt/ivomplaner/releases/<previous-version> /opt/ivomplaner/current
sudo chown -h ivomplaner:ivomplaner /opt/ivomplaner/current
sudo systemctl restart ivomplaner
```

## SMTP-Probleme

1. Konfiguration pruefen:
   ```bash
   sudo sed -n '/^SMTP_/p' /etc/ivomplaner/ivomplaner.env
   ```
2. Logs pruefen:
   ```bash
   sudo journalctl -u ivomplaner -n 200 --no-pager | grep -i smtp
   ```
3. `SMTP_ENCRYPTION_KEY` muss gesetzt bleiben. Bei Verlust koennen gespeicherte SMTP-Passwoerter nicht mehr entschluesselt werden.

## Speicherplatz voll

```bash
df -h
sudo du -sh /opt/ivomplaner/*
```

Alte Backups oder sehr alte Releases erst nach geprueftem Backup entfernen:

```bash
sudo ls -lh /opt/ivomplaner/backups
sudo ls -lh /opt/ivomplaner/releases
```

## Informationen fuer Support

```bash
uname -a
java -version
systemctl status ivomplaner --no-pager
systemctl status postgresql --no-pager
journalctl -u ivomplaner -n 200 --no-pager > ivomplaner-service.log
```
