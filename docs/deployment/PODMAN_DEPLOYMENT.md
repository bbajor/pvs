# Podman Deployment

Die Anwendung wurde vollständig auf Podman umgestellt. Alle Docker-Referenzen wurden entfernt.

## Lizenzprüfung

Podman ist unter der Apache 2.0-Lizenz veröffentlicht und ohne kommerzielle Einschränkungen. Keine Lizenzkosten oder Einschränkungen für den kommerziellen Betrieb.

## Dev-Stages

Die Anwendung unterstützt zwei Dev-Stages:

### 1. Lokal (H2)
- **Zweck**: Schnelle lokale Tests ohne Datenmodell-Änderungen
- **Datenbank**: H2 in-memory (kein Container nötig)
- **ddl-auto**: `create-drop`
- **Start**: Einfach die Anwendung starten (keine Container nötig)
- **Verwendung**: Für schnelle Tests, wenn das Datenmodell nicht angefasst wird

### 2. Container Dev (Postgres)
- **Zweck**: Arbeit mit persistenter Postgres-Datenbank
- **Datenbank**: Postgres-Container (persistente Testdaten)
- **ddl-auto**: `update` (Testdaten bleiben erhalten)
- **Start**: `podman-compose.dev.yml` starten
- **Verwendung**: Wenn Testdaten erhalten bleiben sollen, keine Neubefüllung nötig

## Installation

### Podman installieren

**Windows:**
```powershell
# Podman Desktop herunterladen von https://podman-desktop.io/downloads/windows
# Installer ausführen
```

**Linux:**
```bash
sudo apt-get install -y podman podman-compose
```

**macOS:**
```bash
brew install podman
```

### Podman Compose

Podman unterstützt zwei Varianten:
- `podman compose` (ab Podman 4.0+)
- `podman-compose` (Python-Tool)

Die Scripts versuchen automatisch `podman compose` zuerst, falls nicht verfügbar wird `podman-compose` verwendet.

## Deployment

### Lokale Dev-Umgebung starten

**Bash:**
```bash
./scripts/local/start-dev.sh
```

**PowerShell:**
```powershell
.\scripts\local\start-dev.ps1
```

### Auto-Update Dev-Umgebung

**Bash:**
```bash
./scripts/local/auto-update-dev.sh
```

**PowerShell:**
```powershell
.\scripts\local\auto-update-dev.ps1
```

### Production Deployment

```bash
podman login ghcr.io -u <username> --password-stdin
export DOCKER_REGISTRY=ghcr.io
export DOCKER_IMAGE=<repository>
podman compose -f podman-compose.production.yml --profile prod pull
podman compose -f podman-compose.production.yml --profile prod up -d
```

## Konfiguration

### Dev-Stages unterscheiden

**H2 (lokal):**
- Keine `DATABASE_URL` setzen
- `ddl-auto: create-drop` (Standard)

**Postgres-Container:**
- `DATABASE_URL` setzen (automatisch via podman-compose.dev.yml)
- `ddl-auto: update` (via ENV-Variable `SPRING_JPA_HIBERNATE_DDL_AUTO`)

### Whisper Container

Der Whisper-Container wird automatisch via Podman gestartet:
- Compose-Datei: `docker/whisper/podman-compose.yml`
- Container-Name: `pvs-whisper`
- Port: `9000`

## Unterschiede zu Docker

1. **Socket**: Podman verwendet `/run/podman/podman.sock` (rootless) oder `/var/run/podman/podman.sock` (root)
2. **Netzwerk**: Automatisch via `cni` oder `netavark` (keine explizite `bridge`-Config nötig)
3. **Compose**: `podman compose` oder `podman-compose` (Python-Tool)
4. **Daemon**: Kein Daemon nötig (daemonlos)

## Troubleshooting

### Podman verwendet `docker-compose.exe` auf Windows

**Problem:** Podman Compose verwendet automatisch `docker-compose.exe` aus dem Windows App Store, wenn es im PATH verfügbar ist.

**Lösung:**

1. **`docker-compose.exe` entfernen** (empfohlen):
   - Windows Settings > Apps > docker-compose > Deinstallieren
   - Oder aus dem PATH entfernen

2. **`podman-compose` (Python-Tool) verwenden**:
   ```powershell
   pip install podman-compose
   podman-compose -f podman-compose.dev.yml up -d
   ```

3. **Podman Desktop verwenden** (verwendet native `podman compose`):
   - Podman Desktop installieren und starten
   - Native `podman compose` wird automatisch verwendet

### Podman nicht gefunden

```bash
# Prüfe Installation
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

### Container startet nicht

```bash
# Prüfe Podman-Status
podman info

# Prüfe Container-Logs
podman logs pvs-whisper

# Prüfe Container-Status
podman ps -a
```

## GitHub Actions

Die GitHub Actions wurden auf Podman umgestellt:
- Build: Docker Buildx (OCI-kompatibel, funktioniert mit Podman)
- Deploy: Podman-Befehle statt Docker

## Weitere Informationen

- [Podman Dokumentation](https://docs.podman.io/)
- [Podman Compose](https://github.com/containers/podman-compose)

