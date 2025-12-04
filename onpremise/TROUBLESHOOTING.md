# Troubleshooting - PVS OnPremise

Häufige Probleme und Lösungen bei der Installation und dem Betrieb von PVS OnPremise.

## Container starten nicht

### Problem: Container bleiben im Status "Restarting"

**Lösung:**
```bash
# Logs prüfen
podman-compose -f podman-compose.onpremise.yml logs

# Spezifischen Container-Log prüfen
podman logs pvs-onpremise-app
podman logs pvs-onpremise-postgres

# Container-Status prüfen
podman ps -a
```

**Häufige Ursachen:**
- Falsche Datenbank-Passwörter in `.env`
- Port bereits belegt
- Unzureichende Ressourcen (RAM, Disk)

### Problem: Container starten nicht nach Neustart

**Linux:**
```bash
# Prüfe Systemd-Service
sudo systemctl status pvs-onpremise
sudo journalctl -u pvs-onpremise -n 50

# Service neu aktivieren
sudo systemctl enable pvs-onpremise
sudo systemctl daemon-reload
```

**Windows:**
```powershell
# Prüfe Scheduled Task
Get-ScheduledTask -TaskName "PVS-OnPremise-Start" | Get-ScheduledTaskInfo

# Task neu erstellen
.\install.ps1
```

## Datenbank-Probleme

### Problem: "Connection refused" oder "Authentication failed"

**Lösung:**
1. Prüfe `.env`-Datei:
   ```bash
   # Linux
   sudo cat /opt/pvs/.env | grep POSTGRES
   
   # Windows
   type "C:\Program Files\PVS\.env" | findstr POSTGRES
   ```

2. Prüfe Container-Logs:
   ```bash
   podman logs pvs-onpremise-postgres
   ```

3. Prüfe Datenbank-Verbindung:
   ```bash
   podman exec -it pvs-onpremise-postgres psql -U pvs -d pvs
   ```

4. Container neu starten:
   ```bash
   podman-compose -f podman-compose.onpremise.yml restart postgres
   ```

### Problem: Datenbank-Volumes sind korrupt

**Lösung (ACHTUNG: Datenverlust!):**
```bash
# Container stoppen
podman-compose -f podman-compose.onpremise.yml down

# Volume löschen (nur wenn Backup vorhanden!)
podman volume rm pvs-onpremise_postgres-data

# Container neu starten
podman-compose -f podman-compose.onpremise.yml up -d postgres

# Datenbank wiederherstellen (falls Backup vorhanden)
podman exec -i pvs-onpremise-postgres psql -U pvs pvs < backup.sql
```

## Port-Probleme

### Problem: "Port already in use"

**Lösung:**
```bash
# Prüfe, welcher Prozess den Port belegt
# Linux
sudo lsof -i :8080
sudo netstat -tulpn | grep 8080

# Windows
netstat -ano | findstr :8080

# Port in .env ändern
# APP_PORT=8081
```

### Problem: Anwendung nicht erreichbar

**Lösung:**
1. Prüfe Container-Status:
   ```bash
   podman-compose -f podman-compose.onpremise.yml ps
   ```

2. Prüfe Firewall:
   ```bash
   # Linux
   sudo ufw status
   sudo firewall-cmd --list-all
   
   # Windows
   Get-NetFirewallRule | Where-Object {$_.DisplayName -like "*PVS*"}
   ```

3. Prüfe Health-Check:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## Performance-Probleme

### Problem: Anwendung ist langsam

**Lösung:**
1. Prüfe Ressourcen:
   ```bash
   # Linux
   free -h
   df -h
   top
   
   # Windows
   Get-CimInstance Win32_OperatingSystem | Select-Object TotalVisibleMemorySize,FreePhysicalMemory
   Get-PSDrive C
   ```

2. Prüfe Container-Ressourcen:
   ```bash
   podman stats
   ```

3. Erhöhe Container-Limits in `podman-compose.onpremise.yml`:
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '2'
         memory: 4G
   ```

### Problem: Out of Memory

**Lösung:**
1. Prüfe verfügbaren RAM
2. Reduziere Container-Anzahl (z.B. Whisper deaktivieren)
3. Erhöhe System-RAM oder füge Swap hinzu

## SMTP-Probleme

### Problem: E-Mails werden nicht versendet

**Lösung:**
1. Prüfe SMTP-Konfiguration in `.env`:
   ```
   SMTP_HOST=smtp.example.com
   SMTP_PORT=587
   SMTP_USERNAME=...
   SMTP_PASSWORD=...
   ```

2. Prüfe `SMTP_ENCRYPTION_KEY` (muss gesetzt sein!)

3. Teste SMTP-Verbindung:
   ```bash
   # Im Container
   podman exec -it pvs-onpremise-app bash
   # Teste SMTP-Verbindung (falls Tools verfügbar)
   ```

4. Prüfe Application-Logs:
   ```bash
   podman logs pvs-onpremise-app | grep -i smtp
   ```

## KBV-Service-Probleme

### Problem: KBV-Daten fehlen

**Lösung:**
```bash
# KBV-Daten initialisieren
podman-compose -f podman-compose.onpremise.yml --profile kbv-init up kbv-distrib-job

# Prüfe KBV-Volume
podman volume inspect pvs-onpremise_kbv-data
```

### Problem: KBV-Service startet nicht

**Lösung:**
1. Prüfe KBV-Datenbank:
   ```bash
   podman logs pvs-onpremise-kbv-db
   podman exec -it pvs-onpremise-kbv-db psql -U kbv -d kbv
   ```

2. Prüfe KBV-Service-Logs:
   ```bash
   podman logs pvs-onpremise-kbv-service
   ```

## Update-Probleme

### Problem: Container-Images werden nicht aktualisiert

**Lösung:**
```bash
# Images manuell aktualisieren
podman-compose -f podman-compose.onpremise.yml pull

# Container neu bauen (bei Code-Änderungen)
podman-compose -f podman-compose.onpremise.yml build

# Container neu starten
podman-compose -f podman-compose.onpremise.yml up -d
```

### Problem: Migration-Fehler nach Update

**Lösung:**
1. Prüfe Flyway-Logs:
   ```bash
   podman logs pvs-onpremise-app | grep -i flyway
   ```

2. Prüfe Datenbank-Schema:
   ```bash
   podman exec -it pvs-onpremise-postgres psql -U pvs -d pvs -c "\dt"
   ```

3. Manuelle Migration (falls nötig):
   ```bash
   # Backup erstellen!
   podman exec pvs-onpremise-postgres pg_dump -U pvs pvs > backup.sql
   
   # Migration manuell ausführen (siehe Flyway-Dokumentation)
   ```

## Logs und Debugging

### Logs anzeigen

```bash
# Alle Container-Logs
podman-compose -f podman-compose.onpremise.yml logs -f

# Spezifischer Container
podman logs -f pvs-onpremise-app

# Systemd-Logs (Linux)
sudo journalctl -u pvs-onpremise -f

# Letzte 100 Zeilen
podman logs --tail 100 pvs-onpremise-app
```

### Debug-Modus aktivieren

1. Bearbeite `.env`:
   ```
   SPRING_PROFILES_ACTIVE=onpremise,debug
   ```

2. Container neu starten:
   ```bash
   podman-compose -f podman-compose.onpremise.yml restart pvs-app
   ```

## Häufige Fehlermeldungen

### "Cannot connect to the Docker daemon"

**Ursache:** Podman-Service läuft nicht oder Berechtigungen fehlen.

**Lösung:**
```bash
# Linux
sudo systemctl start podman
sudo systemctl enable podman

# Prüfe Podman-Socket
podman info
```

### "Permission denied" bei Volume-Zugriff

**Ursache:** Falsche Dateiberechtigungen.

**Lösung:**
```bash
# Linux
sudo chown -R pvs:pvs /opt/pvs
sudo chmod 755 /opt/pvs
```

### "No space left on device"

**Ursache:** Festplatte voll.

**Lösung:**
```bash
# Prüfe Speicher
df -h

# Alte Container/Images löschen
podman system prune -a

# Logs löschen
podman-compose -f podman-compose.onpremise.yml logs --no-log-prefix > /dev/null
```

## Support erhalten

Wenn das Problem weiterhin besteht:

1. Sammle Informationen:
   ```bash
   # System-Informationen
   uname -a
   podman --version
   podman-compose --version
   
   # Container-Status
   podman-compose -f podman-compose.onpremise.yml ps
   
   # Logs
   podman-compose -f podman-compose.onpremise.yml logs > logs.txt
   ```

2. Erstelle ein Issue im Repository mit:
   - Beschreibung des Problems
   - Schritte zur Reproduktion
   - System-Informationen
   - Logs (ohne sensible Daten!)

