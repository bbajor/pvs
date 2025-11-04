# Podman Dev - Schnellstart

Kurze Anleitung für lokales Dev-Deployment mit Podman-Containern.

## Voraussetzungen

1. **Podman installieren:**
   - Windows: [Podman Desktop](https://podman-desktop.io/downloads/windows)
   - Linux: `sudo apt-get install -y podman podman-compose`
   - macOS: `brew install podman`

2. **Podman Compose:**
   - `podman compose` (ab Podman 4.0+) oder
   - `podman-compose` (Python-Tool): `pip install podman-compose`

## Schnellstart

### 1. Environment-Datei erstellen

```bash
# Linux/macOS
cp podman-compose.dev.env.example podman-compose.dev.env

# Windows PowerShell
Copy-Item podman-compose.dev.env.example podman-compose.dev.env
```

### 2. Environment-Datei anpassen (optional)

```bash
# Linux/macOS
nano podman-compose.dev.env

# Windows
notepad podman-compose.dev.env
```

Standard-Werte:
- `POSTGRES_DB_DEV=pvs_dev`
- `POSTGRES_USER_DEV=pvs_user`
- `POSTGRES_PASSWORD_DEV=dev_password_local_only_123`

### 3. Container starten

**Bash (Linux/macOS):**
```bash
./scripts/local/start-dev.sh
```

**PowerShell (Windows):**
```powershell
.\scripts\local\start-dev.ps1
```

**Manuell:**
```bash
# Prüfe ob podman-compose oder podman compose verfügbar ist
podman compose version  # oder
podman-compose --version

# Starte Container
podman compose -f podman-compose.dev.yml --env-file podman-compose.dev.env up -d

# Oder mit podman-compose:
podman-compose -f podman-compose.dev.yml --env-file podman-compose.dev.env up -d
```

### 4. Status prüfen

```bash
podman compose -f podman-compose.dev.yml ps
# oder
podman-compose -f podman-compose.dev.yml ps
```

### 5. App öffnen

- **URL**: http://localhost:8130
- **PostgreSQL**: localhost:5434
- **Whisper**: localhost:9000

## Wichtige Befehle

### Container stoppen
```bash
podman compose -f podman-compose.dev.yml down
# oder
podman-compose -f podman-compose.dev.yml down
```

### Logs anzeigen
```bash
podman compose -f podman-compose.dev.yml logs -f
# oder
podman-compose -f podman-compose.dev.yml logs -f
```

### Container neu starten
```bash
podman compose -f podman-compose.dev.yml restart
# oder
podman-compose -f podman-compose.dev.yml restart
```

### Datenbank zurücksetzen
```bash
# Container stoppen
podman compose -f podman-compose.dev.yml down

# Volume löschen (⚠️ alle Daten gehen verloren!)
podman volume rm pvs_postgres-dev-data

# Container neu starten
podman compose -f podman-compose.dev.yml up -d
```

## Troubleshooting

### Podman nicht gefunden
```bash
podman --version
# Falls nicht installiert, siehe Installation oben
```

### Compose-Kommando nicht verfügbar
```bash
# Prüfe podman compose
podman compose version

# Falls nicht verfügbar, installiere podman-compose
pip install podman-compose
```

### Port bereits belegt
```bash
# Prüfe belegte Ports
podman ps -a

# Ändere Port in podman-compose.dev.yml oder stoppe anderen Container
```

### Container startet nicht
```bash
# Prüfe Logs
podman compose -f podman-compose.dev.yml logs

# Prüfe Podman-Status
podman info

# Prüfe Container-Status
podman ps -a
```

## Dev-Stages

### Lokal (H2) - ohne Container
- Kein Podman nötig
- Schnelle Tests ohne Datenmodell-Änderungen
- `ddl-auto: create-drop` (Daten gehen bei jedem Start verloren)

### Container Dev (Postgres) - mit Container
- Podman-Container mit Postgres
- Persistente Testdaten
- `ddl-auto: update` (Daten bleiben erhalten)
- Keine Neubefüllung nötig

## Weitere Infos

Siehe [PODMAN_DEPLOYMENT.md](./PODMAN_DEPLOYMENT.md) für detaillierte Informationen.

