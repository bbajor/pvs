# Automatisches Dev-Deployment auf Windows

Wenn du zu `dev` Branch pushst, kann automatisch auf deinem lokalen Windows-Rechner deployed werden.

## 🎯 Optionen für Windows

### Option 1: Scheduled Task mit PowerShell Script (Einfachste Lösung) ✅

Das PowerShell-Script `scripts/local/auto-update-dev.ps1` prüft regelmäßig auf neue Images und deployed automatisch.

#### Setup-Schritte:

**1. Scheduled Task erstellen:**

1. **Task Scheduler öffnen:**
   - Windows-Taste drücken → "Aufgabengesteuerung" eingeben
   - Oder: `Win+R` → `taskschd.msc` → Enter

2. **Neue Aufgabe erstellen:**
   - Rechtsklick auf "Aufgabenplanungsbibliothek" → **"Aufgabe erstellen..."**

3. **Allgemein-Tab:**
   - **Name:** `PVS Dev Auto-Update`
   - **Beschreibung:** `Automatisches Update der lokalen PVS Dev-Umgebung nach Push zu dev Branch`
   - ✅ **Haken bei:** "Unabhängig von der Benutzeranmeldung ausführen"
   - ✅ **Haken bei:** "Mit höchsten Privilegien ausführen"
   - **Konfigurieren für:** Windows 10/11

4. **Trigger-Tab:**
   - **"Neu..."** → Auswählen:
     - ✅ **"Bei einem Ereignis"** → Protokoll: `Ereignisprotokoll: Microsoft-Windows-UserProfileService/Operational`
     - ✅ **"Bei Start des Computers"** → Verzögerung: `5 Minuten`
     - ✅ **"Wiederholung alle:"** → `5 Minuten` → Dauer: `Unbegrenzt`
   - Oder einfacher: **"Wiederholung alle:"** → `5 Minuten` → Dauer: `Unbegrenzt`

5. **Aktionen-Tab:**
   - **"Neu..."** → Auswählen:
     - **Aktion:** "Programm starten"
     - **Programm/Skript:** `powershell.exe`
     - **Argumente hinzufügen:**
       ```
       -ExecutionPolicy Bypass -File "D:\workspace\pvs\scripts\local\auto-update-dev.ps1"
       ```
     - **Starten in (optional):**
       ```
       D:\workspace\pvs
       ```

6. **Bedingungen-Tab:**
   - ✅ **"Aufgabe nur starten, wenn Computer im Netzbetrieb ausgeführt wird"** (deaktivieren wenn nicht gewünscht)
   - ✅ **"Computer zum Ausführen dieser Aufgabe aktivieren"** (aktivieren)

7. **Einstellungen-Tab:**
   - ✅ **"Aufgabe so schnell wie möglich nach einem verpassten Start ausführen"**
   - ✅ **"Aufgabe starten unabhängig davon, ob ein Benutzer angemeldet ist"**

8. **Speichern** → Passwort eingeben (falls nötig)

**2. Task testen:**

```powershell
# Task manuell ausführen
schtasks /run /tn "PVS Dev Auto-Update"

# Oder im Task Scheduler:
# Rechtsklick auf Task → "Ausführen"
```

**3. Logs prüfen:**

Das Script gibt Output direkt aus. Du kannst die Ausgabe auch in eine Datei umleiten:

```powershell
# In auto-update-dev.ps1 am Ende hinzufügen (optional):
$logFile = Join-Path $COMPOSE_DIR "pvs-auto-update.log"
# ... Ausgaben mit >> $logFile umleiten
```

**4. Docker Desktop voraussetzung:**

- Docker Desktop muss installiert und gestartet sein
- Docker Compose V2 muss verfügbar sein (Standard in neueren Docker Desktop-Versionen)

#### Funktionsweise:

1. **Push zu dev Branch** → GitHub Actions baut Image → `ghcr.io/bbajor/pvs:dev-latest`
2. **Scheduled Task** läuft alle 5 Minuten
3. Script pulled neues Image (falls verfügbar)
4. Vergleicht Image-IDs
5. Wenn neues Image: `docker compose up -d` → Deployment

---

### Option 2: Windows selbst-hosted GitHub Actions Runner (Erweitert)

GitHub Actions kann direkt auf deinem Windows-Rechner deployen.

#### Setup-Schritte:

**1. Runner herunterladen:**

```powershell
# PowerShell als Administrator
cd C:\
mkdir actions-runner; cd actions-runner

# Aktuelle Version herunterladen
# Prüfe: https://github.com/actions/runner/releases
Invoke-WebRequest -Uri https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-win-x64-2.311.0.zip -OutFile actions-runner.zip

# Entpacken
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD\actions-runner.zip", "$PWD")
```

**2. Runner-Token holen:**

1. Gehe zu: `https://github.com/bbajor/pvs/settings/actions/runners/new`
2. Wähle **Windows** aus
3. Kopiere den **Token** (wird nur einmal angezeigt!)

**3. Runner konfigurieren:**

```powershell
cd C:\actions-runner
.\config.cmd --url https://github.com/bbajor/pvs --token <DEIN_TOKEN>

# Antworten:
# → Runner name: [Enter] oder z.B. "pvs-windows-dev"
# → Labels: [Enter] oder z.B. "self-hosted,windows,dev"
# → Work folder: [Enter für Default: _work]
```

**4. Runner starten:**

```powershell
# Testweise im Vordergrund:
.\run.cmd

# Oder als Windows Service:
.\config.cmd --url https://github.com/bbajor/pvs --token <TOKEN> --runasservice
```

**5. Workflow anpassen:**

Der Workflow `.github/workflows/deploy-dev-local.yml` muss für Windows angepasst werden:

```yaml
runs-on: [self-hosted, windows]  # Statt: [self-hosted, linux]
```

Und die Befehle müssen für Windows angepasst werden (siehe Option 1 für PowerShell-Version).

**6. Runner in GitHub prüfen:**

- Gehe zu: `https://github.com/bbajor/pvs/settings/actions/runners`
- Runner sollte **"Online"** (grün) sein

---

### Option 3: WSL mit Linux Runner (Alternative)

Falls du WSL nutzt, kannst du einen Linux-Runner in WSL installieren (siehe `SELF_HOSTED_RUNNER_SETUP.md`).

---

## 🔧 Troubleshooting

### Scheduled Task läuft nicht

```powershell
# Task-Status prüfen
schtasks /query /tn "PVS Dev Auto-Update"

# Task-Historie prüfen
# Task Scheduler → Task → "Verlauf" Tab

# Manuell testen
schtasks /run /tn "PVS Dev Auto-Update"
```

### Docker nicht erreichbar vom Task

```powershell
# Docker Desktop muss laufen!
# Prüfe:
docker ps

# Falls Docker Desktop nicht automatisch startet:
# Windows-Einstellungen → Start-Apps → Docker Desktop aktivieren
```

### PowerShell Execution Policy

```powershell
# Als Administrator:
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Oder im Script Bypass verwenden (bereits enthalten):
# -ExecutionPolicy Bypass
```

### Image Pull fehlgeschlagen

```powershell
# Manuell testen:
docker pull ghcr.io/bbajor/pvs:dev-latest

# Falls Login erforderlich:
docker login ghcr.io -u USERNAME --password-stdin
# Token: GitHub → Settings → Developer settings → Personal access tokens → Packages:read
```

### docker-compose.dev.env fehlt

```powershell
# Kopiere Beispiel-Datei:
cd D:\workspace\pvs
Copy-Item docker-compose.dev.env.example docker-compose.dev.env

# Bearbeite mit deinen Werten:
notepad docker-compose.dev.env
```

---

## ⚙️ Konfiguration

### Environment-Variablen

Die folgenden Environment-Variablen können gesetzt werden (optional):

- `PVS_LOCAL_PATH`: Pfad zu deinem PVS-Verzeichnis (Default: `D:\workspace\pvs`)
- `GITHUB_REPO_OWNER`: Repository-Owner (Default: `bbajor`)

**Setzen in Windows:**

```powershell
# System-Umgebungsvariablen (für alle Benutzer)
[System.Environment]::SetEnvironmentVariable('PVS_LOCAL_PATH', 'D:\workspace\pvs', 'Machine')

# Benutzer-Umgebungsvariablen
[System.Environment]::SetEnvironmentVariable('PVS_LOCAL_PATH', 'D:\workspace\pvs', 'User')
```

Oder via GUI:
1. Windows-Taste → "Umgebungsvariablen" → Enter
2. "Umgebungsvariablen..." → "Neu..."

### Intervall anpassen

Im Task Scheduler:
- Trigger → Bearbeiten → Wiederholung ändern (z.B. alle 2 Minuten für schnellere Updates)

---

## 📋 Vergleich der Optionen

| Option | Pro | Contra |
|--------|-----|--------|
| **Scheduled Task** | ✅ Einfach<br>✅ Kein Runner nötig<br>✅ Funktioniert out-of-the-box | ❌ Polling (5 Min Verzörgerung)<br>❌ Läuft ständig |
| **Windows Runner** | ✅ Sofortiges Deployment<br>✅ Direkter Trigger | ❌ Setup komplexer<br>❌ Runner muss laufen |
| **WSL Runner** | ✅ Sofortiges Deployment<br>✅ Linux-Umgebung | ❌ WSL Setup erforderlich |

---

## ✅ Checkliste

- [ ] PowerShell-Script `auto-update-dev.ps1` vorhanden
- [ ] Docker Desktop installiert und läuft
- [ ] `docker-compose.dev.env` erstellt (aus `.example`)
- [ ] Scheduled Task erstellt und getestet
- [ ] Test-Push zu dev gemacht
- [ ] Deployment erfolgreich (prüfe: `docker compose -f docker-compose.dev.yml ps`)

---

## 🎯 Empfehlung

**Für Windows-Entwicklung:** **Scheduled Task mit PowerShell-Script** ✅
- Einfachster Setup
- Funktioniert zuverlässig
- 5 Minuten Verzögerung ist für Dev akzeptabel

**Für sofortiges Deployment:** Windows Runner (aber komplexerer Setup)

