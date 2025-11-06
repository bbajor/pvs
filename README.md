# PVS (Praxis-Verwaltungs-System)

## Lizenzierung
- Code steht unter Business Source License 1.1 (BUSL-1.1) mit Parametern in `LICENSE.md`.
- Interner Betrieb in eigenen Praxen erlaubt; Angebot als SaaS nur mit separater Hosting-Lizenz (`HOSTING-LIZENZ-DE.md`).
- Change Date: 2028-10-27 → Wechsel auf Apache-2.0.

## Prerequisites

- Java 17 or higher
- Maven 3.8+ oder Gradle
- Podman oder Docker für Container-basierte Entwicklung
- OpenSC for eGK card reading functionality (optional)
- eGK-Tool (available from your KV/gematik, optional)

## 🚀 Schnellstart - Lokale Entwicklung

### Voraussetzungen

1. **Podman installieren:**
   - Windows: [Podman Desktop](https://podman-desktop.io/downloads/windows)
   - Linux: `sudo apt-get install -y podman podman-compose`
   - macOS: `brew install podman`

2. **Podman Compose:**
   - `podman compose` (ab Podman 4.0+) oder
   - `podman-compose` (Python-Tool): `pip install podman-compose`

### Setup

1. **Environment-Datei erstellen:**
   ```bash
   # Linux/macOS
   cp podman-compose.dev.env.example podman-compose.dev.env
   
   # Windows PowerShell
   Copy-Item podman-compose.dev.env.example podman-compose.dev.env
   ```

2. **Container starten:**
   ```powershell
   # Windows PowerShell
   .\scripts\local\start-dev.ps1
   
   # Linux/macOS
   ./scripts/local/start-dev.sh
   ```

3. **App öffnen:**
   - **URL**: http://localhost:8130
   - **PostgreSQL**: localhost:5434
   - **Whisper**: localhost:9000

### Wichtige Befehle

```bash
# Container stoppen
podman compose -f podman-compose.dev.yml down

# Logs anzeigen
podman compose -f podman-compose.dev.yml logs -f

# Container neu starten
podman compose -f podman-compose.dev.yml restart

# Datenbank zurücksetzen
podman compose -f podman-compose.dev.yml down
podman volume rm pvs_postgres-dev-data
podman compose -f podman-compose.dev.yml up -d
```

### Troubleshooting

**Podman verwendet `docker-compose.exe` auf Windows:**
```powershell
# Prüfe ob docker-compose.exe im PATH ist
Get-Command docker-compose.exe -ErrorAction SilentlyContinue

# Falls gefunden, entferne es aus dem PATH oder deinstalliere es
```

**Gradle Build-Fehler im Container:**
```powershell
# Baue lokal vor dem Container-Build
./gradlew clean build -x test

# Dann starte Container
.\scripts\local\start-dev.ps1
```

## 🏗️ Deployment - Hetzner Server

### Schnellstart (10 Minuten)

1. **Hetzner VPS erstellen:**
   - Gehe zu [hetzner.com/cloud](https://www.hetzner.com/cloud)
   - **Type**: CX21 (2 vCPU, 4GB RAM) - **5€/Monat**
   - **Location**: Nürnberg oder Falkenstein (DSGVO-konform)
   - **Image**: Ubuntu 22.04 LTS
   - **IP-Adresse notieren!**

2. **Server-Setup ausführen:**
   ```bash
   # Auf dem Hetzner Server (als root)
   cd /root
   curl -fsSL https://raw.githubusercontent.com/bbajor/pvs/master/scripts/deployment/setup-server.sh | bash
   ```

3. **Environment-Datei erstellen:**
   ```bash
   cd /opt/pvs
   nano .env
   ```
   
   **Minimale .env Datei:**
   ```bash
   POSTGRES_DB_DEV=pvs_dev
   POSTGRES_USER_DEV=pvs_user
   POSTGRES_PASSWORD_DEV=$(openssl rand -base64 32)
   
   POSTGRES_DB_TEST=pvs_test
   POSTGRES_USER_TEST=pvs_user
   POSTGRES_PASSWORD_TEST=$(openssl rand -base64 32)
   
   POSTGRES_DB_PROD=pvs_prod
   POSTGRES_USER_PROD=pvs_user
   POSTGRES_PASSWORD_PROD=$(openssl rand -base64 32)
   ```

4. **Datenbanken initialisieren:**
   ```bash
   cd /opt/pvs
   curl -fsSL https://raw.githubusercontent.com/bbajor/pvs/master/scripts/deployment/init-databases.sh | bash
   ```

5. **SSH Key für GitHub Actions einrichten:**
   ```bash
   # Auf lokalem Rechner
   ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/hetzner_deploy -N ""
   
   # Public Key auf Server kopieren
   cat ~/.ssh/hetzner_deploy.pub | ssh root@<HETZNER_IP> "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
   
   # Private Key für GitHub Secret kopieren
   cat ~/.ssh/hetzner_deploy
   ```

6. **GitHub Secrets konfigurieren:**
   - GitHub → Settings → Secrets → Actions → New Secret:
   ```
   HETZNER_HOST=<HETZNER_IP>
   HETZNER_USER=root
   HETZNER_SSH_KEY=<Private Key Inhalt>
   PROD_DB_HOST=localhost
   PROD_DB_NAME=pvs_prod
   PROD_DB_USER=pvs_user
   PROD_DB_PASSWORD=<aus .env kopieren>
   ```

7. **Deployment testen:**
   - GitHub → Actions → "Build and Push Docker Images (Hetzner)" → Run workflow → Stage: dev

## 🔒 Security

### SSH Key Setup

1. **SSH Key generieren:**
   ```bash
   ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/hetzner_deploy -N ""
   ```

2. **Public Key auf Server kopieren:**
   ```bash
   cat ~/.ssh/hetzner_deploy.pub | ssh root@<HETZNER_IP> "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
   ```

3. **SSH-Zugriff testen:**
   ```bash
   ssh -i ~/.ssh/hetzner_deploy root@<HETZNER_IP>
   ```

4. **GitHub Secret konfigurieren:**
   - Private Key kopieren: `cat ~/.ssh/hetzner_deploy`
   - GitHub → Settings → Secrets → Actions → New Secret: `HETZNER_SSH_KEY`

### Passwort-Änderungen

**Datenbank-Passwort ändern:**
1. Neues Passwort in `.env` Datei setzen
2. Container neu starten: `docker-compose restart postgres`
3. GitHub Secret `PROD_DB_PASSWORD` aktualisieren

**Server Root-Passwort ändern:**
```bash
# Auf Server
passwd root
```

## 📋 Branch-Strategie

Das Projekt nutzt eine Drei-Branch-Strategie für Development, Testing und Production:

### Branches

- **`dev`**: Entwicklungs-Branch für lokale Entwicklung
  - Schnelles Testing: `./gradlew bootRun` mit H2 In-Memory
  - Realistisches Testing: Podman-Container mit PostgreSQL
  - CI: Vollständiger Build + Tests bei Push/PR nach `dev`
  - Keine Server-Deployments

- **`test`**: Staging-Branch für realistisches Testing auf Server
  - Verwendet PostgreSQL mit persistenter Datenbank auf Hetzner
  - Daten bleiben über Deployments hinweg erhalten
  - CI: Build + Push Image → automatisches Deployment auf Test-Hetzner
  - Nur über VPN erreichbar

- **`master`**: Production-ready Code
  - Nur stabile Releases nach ausgiebigem Testing
  - Verwendet PostgreSQL Production-Datenbank
  - CI: Build + Push Image → automatisches Deployment auf Prod-Hetzner
  - Öffentlich erreichbar über Traefik/HTTPS

### Workflow

```
feature/* → dev → test → master
```

1. **Feature-Entwicklung**: Neue Features werden als Feature-Branches von `dev` abgezweigt
   - Agenten: `cursor/<agent>/<topic>`, PRs ausschließlich nach `dev`
2. **Pull Request zu `dev`**: Feature-Branch wird in `dev` gemergt nach Review
3. **Testing in `test`**: Nach erfolgreicher Validierung wird `dev` in `test` gemergt
4. **Production Release**: Nach finaler Validierung wird `test` in `master` gemergt

### Wichtige Regeln

- ✅ **Merge-Richtung**: Immer nur in eine Richtung mergen (dev→test→master), nie zurück
- ✅ **Hotfixes**: Bei dringenden Fixes von `master` abzweigen, dann in alle Branches zurückmergen
- ✅ **Version Tags**: Bei jedem Merge zu `master` ein neues Version-Tag erstellen (v0.1.1, v0.2.0, etc.)
- ✅ **Branch Protection**: `master` erfordert Pull Request Reviews und erfolgreiche CI-Tests

## 🗄️ Datenbank-Architektur

### Separate Container pro Environment (Empfohlen)

**Vorteile:**
- ✅ Isolation: Dev/Test/Prod komplett getrennt
- ✅ Einfaches Backup/Restore: Volume-Snapshots
- ✅ Portabel: Gleiche Config überall
- ✅ Einfaches Upgrade: Container-Image wechseln

**Konfiguration:**
```yaml
postgres-dev:    Port 5433, Volume: postgres-data-dev
postgres-test:   Port 5434, Volume: postgres-data-test  
postgres-prod:   Port 5435, Volume: postgres-data-prod
```

## 🛠️ Projektstruktur

```
pvs/
├── src/
│   ├── main/
│   │   ├── java/de/bbajor/pvs/
│   │   │   ├── base/        # Base entities and services
│   │   │   ├── config/      # Configuration classes
│   │   │   └── ui/         # Vaadin UI components
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── scripts/
│   ├── deployment/         # Deployment-Scripts
│   │   ├── setup-server.sh
│   │   └── init-databases.sh
│   ├── security/          # Security-Scripts
│   │   └── cleanup-ssh-key.sh
│   └── local/             # Lokale Entwicklung
│       └── start-dev.ps1
├── podman-compose.dev.yml  # Development Compose
├── podman-compose.production.yml  # Production Compose
└── pom.xml
```

## 🔧 Troubleshooting

### eGK Card Reading Issues
- Verify OpenSC installation: `opensc-tool --version`
- Check if card reader is recognized: `opensc-tool --list-readers`
- Ensure egk-tool.exe path is correctly set in application.properties

### Container-Probleme

**Container startet nicht:**
```bash
# Logs prüfen
podman compose -f podman-compose.dev.yml logs

# Podman-Status prüfen
podman info

# Container-Status prüfen
podman ps -a
```

**Port bereits belegt:**
```bash
# Prüfe belegte Ports
podman ps -a

# Ändere Port in podman-compose.dev.yml oder stoppe anderen Container
```

**Datenbank-Verbindungsfehler:**
```bash
# Prüfe ob PostgreSQL läuft
podman ps | grep postgres

# Prüfe Logs
podman compose -f podman-compose.dev.yml logs postgres
```

## 📚 Weitere Ressourcen

- [GitHub Repository](https://github.com/bbajor/pvs)
- [Hosting-Lizenz (DE)](./HOSTING-LIZENZ-DE.md)
- [BUSL 1.1 Text](https://mariadb.com/bsl11/)
- [Vaadin Documentation](https://vaadin.com/docs)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)

## 📝 Notizen

- **Entwicklungsumgebung**: Test-Credentials und Sample-Users sind in dev/test-Profilen erlaubt
- **Production**: NULL-Toleranz für hardcodierte Credentials
- **Security-First**: Alle neuen Features müssen Security-Review durchlaufen
