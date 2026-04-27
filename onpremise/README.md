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

### Linux

```bash
# 1. Repository klonen oder Release entpacken
cd /tmp
git clone <repository-url> pvs
cd pvs/onpremise

# 2. Anwendung bauen und natives Paket erstellen
./build-native-package.sh

# 3. Installer ausführen
sudo bash install.sh

# 4. Konfiguration prüfen
sudo nano /opt/pvs/.env

# 5. Service starten
sudo systemctl start pvs-onpremise

# 6. Status prüfen
sudo systemctl status pvs-onpremise
curl http://127.0.0.1:8080/actuator/health
```

Windows wird für den produktiven On-Premise-Pfad nicht automatisiert. Empfohlen ist ein kleiner Linux-Server oder eine Linux-VM mit systemd.

## Detaillierte Installation

Siehe [INSTALLATION.md](./INSTALLATION.md) für eine ausführliche Installationsanleitung.

## Konfiguration

### Environment-Variablen

Die wichtigsten Konfigurationsoptionen befinden sich in der `.env`-Datei:

- **Datenbank**: PostgreSQL-Zugangsdaten
- **SMTP**: E-Mail-Versand (optional)
- **Ports**: Anpassung der Ports
- **AI/Whisper**: Aktivierung des lokalen Whisper-Services

Siehe [env.example](./env.example) für alle verfügbaren Optionen.

### Automatischer Start

#### Linux
Der Systemd-Service wird automatisch installiert und aktiviert. Die Anwendung startet nach jedem Systemneustart automatisch.

```bash
# Service aktivieren (bereits bei Installation geschehen)
sudo systemctl enable pvs-onpremise

# Service manuell starten/stoppen
sudo systemctl start pvs-onpremise
sudo systemctl stop pvs-onpremise

# Status prüfen
sudo systemctl status pvs-onpremise

# Logs anzeigen
sudo journalctl -u pvs-onpremise -f
```

## Betrieb

### Service-Verwaltung

#### Linux
```bash
# Start/Stop/Restart
sudo systemctl start pvs-onpremise
sudo systemctl stop pvs-onpremise
sudo systemctl restart pvs-onpremise

# Logs
sudo journalctl -u pvs-onpremise -f
```

### Backup

#### Datenbank-Backup
```bash
sudo /opt/pvs/backup.sh
```

### Updates

```bash
# Aus lokalem Release-Paket
sudo /opt/pvs/update.sh /path/to/ivomplaner-onpremise.tar.gz

# Oder per URL aus RELEASE_ARTIFACT_URL in /opt/pvs/.env
sudo /opt/pvs/update.sh
```

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
sudo bash onpremise/uninstall.sh
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
2. Prüfe die Logs: `journalctl -u pvs-onpremise -f`
3. Erstelle ein Issue im Repository

## Lizenz

Siehe [LICENSE.md](../LICENSE.md) für Lizenzinformationen.

