# IVOMPlaner On-Premise Installation

Diese Dokumentation beschreibt die Installation und den Betrieb von IVOMPlaner auf einem lokalen Linux-Server.

## Übersicht

Die On-Premise-Lösung läuft nativ ohne Container-Runtime:

- Java-21-Anwendung als Spring-Boot-JAR
- Lokale PostgreSQL-Datenbank
- Systemd-Service mit automatischem Start
- Backup-, Restore- und Update-Skripte
- Optional: lokaler Whisper-Service außerhalb dieser Installation

## Systemanforderungen

### Linux
- Linux-Distribution mit systemd (Ubuntu/Debian empfohlen)
- Java 21 JRE
- PostgreSQL 15+
- curl, openssl, tar, gzip
- Mindestens 4 GB RAM (8 GB empfohlen)
- 20 GB freier Festplattenspeicher
- Root-Zugriff für Installation

## Schnellstart

### Linux-VM

```bash
export IVOMPLANER_RELEASE_BASE_URL="https://github.com/<org>/<repo>/releases/latest/download"
curl -fsSL "$IVOMPLANER_RELEASE_BASE_URL/install.sh" | sudo IVOMPLANER_RELEASE_BASE_URL="$IVOMPLANER_RELEASE_BASE_URL" bash
```

Der Installer lädt `ivomplaner-onpremise-latest.tar.gz`, prüft die SHA256-Datei, richtet Java/PostgreSQL/systemd ein und startet die Anwendung.

Alternativ mit lokalem Release-Paket:

```bash
sudo bash install.sh /path/to/ivomplaner-onpremise-1.2.3.tar.gz
```

Windows wird für den produktiven On-Premise-Pfad nicht automatisiert. Empfohlen ist ein kleiner Linux-Server oder eine Linux-VM mit systemd.

## Detaillierte Installation

Siehe [INSTALLATION.md](./INSTALLATION.md) für eine ausführliche Installationsanleitung.

## Konfiguration

### Environment-Variablen

Die wichtigsten Konfigurationsoptionen befinden sich in `/etc/ivomplaner/ivomplaner.env`:

- **Datenbank**: PostgreSQL-Zugangsdaten
- **SMTP**: E-Mail-Versand (optional)
- **Ports**: Anpassung der Ports
- **AI/Whisper**: Aktivierung des lokalen Whisper-Services
- **Updates**: `IVOMPLANER_RELEASE_BASE_URL` fuer `ivomplaner-update latest`

Siehe [env.example](./env.example) für alle verfügbaren Optionen.

### Automatischer Start

#### Linux
Der Systemd-Service wird automatisch installiert und aktiviert. Die Anwendung startet nach jedem Systemneustart automatisch.

```bash
# Service aktivieren (bereits bei Installation geschehen)
sudo systemctl enable ivomplaner

# Service manuell starten/stoppen
sudo systemctl start ivomplaner
sudo systemctl stop ivomplaner

# Status prüfen
sudo systemctl status ivomplaner

# Logs anzeigen
sudo journalctl -u ivomplaner -f
```

## Betrieb

### Service-Verwaltung

#### Linux
```bash
# Start/Stop/Restart
sudo systemctl start ivomplaner
sudo systemctl stop ivomplaner
sudo systemctl restart ivomplaner

# Logs
sudo journalctl -u ivomplaner -f
```

### Backup

#### Datenbank-Backup
```bash
sudo ivomplaner-backup
```

### Release-Layout

```text
/opt/ivomplaner/
  current -> /opt/ivomplaner/releases/<version>
  releases/<version>/
  backups/
/etc/ivomplaner/ivomplaner.env
```

Die Releases sind austauschbar. PostgreSQL-Daten liegen getrennt in der lokalen Datenbank und bleiben bei App-Updates erhalten.

### Updates

```bash
# Latest Release aus IVOMPLANER_RELEASE_BASE_URL
sudo ivomplaner-update latest

# Oder aus lokalem Release-Paket
sudo ivomplaner-update /path/to/ivomplaner-onpremise-1.2.4.tar.gz
```

Updates erstellen vorher automatisch ein PostgreSQL-Backup unter `/opt/ivomplaner/backups`. Danach wird der `current`-Symlink auf das neue Release gesetzt. Schlaegt der Healthcheck fehl, wird auf das vorherige Release zurueckgeschaltet.

### Update aus der App

Super-Admins sehen in der Navigation den Punkt **System-Update**. Die App prueft dort die installierte Version gegen die `VERSION`-Datei des neuesten GitHub-Releases.

Beim Klick auf **Update installieren**:

1. bestaetigt der Anwender, dass offene Aenderungen gespeichert wurden,
2. startet die App `sudo -n /usr/local/bin/ivomplaner-update-wrapper latest`,
3. der Wrapper startet das eigentliche Update per `systemd-run`,
4. `ivomplaner-update` erstellt ein Datenbank-Backup,
5. das neue Release wird installiert und der Service neu gestartet.

Update-Logs liegen unter `/var/log/ivomplaner/update-*.log`.

## Troubleshooting

Siehe [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) für häufige Probleme und Lösungen.

## Sicherheit

### Wichtige Sicherheitshinweise

1. **Passwörter ändern**: Nach der Installation sollten alle Standard-Passwörter geändert werden
2. **Firewall**: Nur notwendige Ports öffnen (standardmäßig nur 8080)
3. **Backups**: Regelmäßige Backups der Datenbank durchführen
4. **Updates**: Release-Pakete zeitnah einspielen
5. **SMTP_ENCRYPTION_KEY**: Sicher aufbewahren - bei Verlust können verschlüsselte SMTP-Passwörter nicht mehr entschlüsselt werden

### Netzwerk

Die Anwendung lauscht standardmäßig auf Port `8080`. Für produktive externe Zugriffe:

1. Firewall-Regeln konfigurieren
2. Reverse Proxy (z.B. Nginx, Traefik) verwenden
3. SSL/TLS-Zertifikate einrichten

## Deinstallation

### Linux

```bash
# Als root ausführen
sudo ivomplaner-uninstall
```

Das Skript führt interaktiv durch die Deinstallation:
- Entfernt Systemd-Service
- Stoppt den Systemd-Service
- **WICHTIG**: Fragt explizit nach Datenbank und Backups
- Entfernt Installations-Verzeichnis (Backups optional behalten)
- Zeigt Zusammenfassung am Ende

## Support

Bei Problemen oder Fragen:

1. Prüfe die [Troubleshooting-Dokumentation](./TROUBLESHOOTING.md)
2. Prüfe die Logs: `journalctl -u ivomplaner -f`
3. Erstelle ein Issue im Repository

## Lizenz

Siehe [LICENSE.md](../LICENSE.md) für Lizenzinformationen.

