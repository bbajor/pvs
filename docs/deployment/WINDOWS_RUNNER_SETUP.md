# Windows GitHub Actions Runner Setup

Dieser Guide erklärt, wie du einen Windows GitHub Actions Runner einrichtest, der automatisch dein lokal-dev Deployment ausführt.

## 🎯 Vorteile des Windows Runners

- ✅ **Direkt PowerShell**: Keine Konvertierung, native Windows-Umgebung
- ✅ **Sofortiges Deployment**: Läuft direkt nach GitHub Actions Build
- ✅ **Keine Scheduled Tasks nötig**: Runner triggert automatisch
- ✅ **Vollständige Logs**: Alle Ausgaben direkt in GitHub Actions sichtbar
- ✅ **Environment-Variablen**: GitHub Secrets direkt verfügbar

## 📋 Voraussetzungen

- Windows 10/11
- Docker Desktop installiert und läuft
- PowerShell 5.1+ oder PowerShell 7+
- Administrator-Rechte für Runner-Setup

## 🚀 Schritt-für-Schritt Setup

### Schritt 1: Runner herunterladen

**PowerShell als Administrator öffnen:**

```powershell
# Verzeichnis erstellen
cd C:\
New-Item -ItemType Directory -Path "actions-runner" -Force
cd actions-runner

# Aktuelle Runner-Version herunterladen
# Prüfe aktuelle Version: https://github.com/actions/runner/releases
$version = "2.311.0"
$url = "https://github.com/actions/runner/releases/download/v$version/actions-runner-win-x64-$version.zip"
$zipFile = "actions-runner.zip"

Write-Host "Lade Runner herunter..." -ForegroundColor Cyan
Invoke-WebRequest -Uri $url -OutFile $zipFile

# Entpacken
Write-Host "Entpacke Runner..." -ForegroundColor Cyan
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory((Resolve-Path $zipFile), $PWD)

Write-Host "Runner erfolgreich heruntergeladen!" -ForegroundColor Green
```

### Schritt 2: GitHub Token holen

**Du wirst den Token gleich eingeben können.** 

1. Gehe zu: `https://github.com/bbajor/pvs/settings/actions/runners/new`
2. Wähle **Windows**
3. Kopiere den **Token** (beginnt mit `A...`)

### Schritt 3: Runner konfigurieren

```powershell
cd C:\actions-runner

# Runner konfigurieren
.\config.cmd --url https://github.com/bbajor/pvs --token <DEIN_TOKEN>

# Antworten:
# → Runner name: [Enter] oder z.B. "pvs-windows-dev"
# → Labels: [Enter] oder z.B. "self-hosted,windows,dev"
# → Work folder: [Enter] für Default: _work
```

**WICHTIG:** Der Runner muss das Label `windows` haben, damit der Workflow ihn findet!

### Schritt 4: Runner testen

```powershell
# Runner im Vordergrund starten (zum Testen)
.\run.cmd
```

**Erwartete Ausgabe:**
```
√ Connected to GitHub

Runner listener started
```

### Schritt 5: Runner als Windows Service installieren

**Option 1: Automatische Service-Installation (falls beim ersten Setup funktioniert hat):**

```powershell
# Service installieren (muss als Administrator laufen)
cd C:\active-runners
.\config.cmd --url https://github.com/bbajor/pvs --token <TOKEN> --runasservice

# Oder manuell:
.\svc\install.cmd
.\svc\start.cmd
```

**Service als Benutzer installieren (EMPFOHLEN für Docker):**

```powershell
# PowerShell als Administrator
# Aber Service läuft als dein Benutzer-Konto
.\config.cmd --url https://github.com/bbajor/pvs --token <TOKEN> --runasservice --user "$env:USERDOMAIN\$env:USERNAME"
```

**Warum?** Docker Desktop, Environment-Variablen und Dateizugriff funktionieren nur im Benutzer-Kontext!

**Option 2: Nachträgliche Service-Installation (falls automatische Installation nicht funktioniert hat):**

```powershell
# PowerShell als Administrator
cd D:\workspace\pvs\scripts\local
.\setup-runner-service.ps1 -RunnerPath "C:\active-runners"

# Script führt dich durch:
# 1. Token-Eingabe (von GitHub: settings/actions/runners/new)
# 2. Service-Installation
# 3. Automatischer Start
```

**Option 3: Scheduled Task (Alternative falls Service-Probleme bestehen):**

```powershell
# PowerShell als Administrator
cd D:\workspace\pvs\scripts\local
.\setup-runner-task.ps1 -RunnerPath "C:\active-runners"

# Task startet automatisch:
# - Beim Windows-Start
# - Bei deiner Anmeldung
```

### Schritt 6: Runner prüfen

1. **In GitHub:**
   - Gehe zu: `https://github.com/bbajor/pvs/settings/actions/runners`
   - Runner sollte erscheinen mit Status "Online" (grün)
   - Labels sollten `self-hosted` und `windows` enthalten

2. **Lokal prüfen:**
```powershell
# Service-Status prüfen
Get-Service actions.runner.* | Select-Object Name, Status

# Oder über Task Manager → Services
```

## ✅ Testen

### Manueller Workflow-Trigger

1. Gehe zu: `https://github.com/bbajor/pvs/actions/workflows/deploy-dev-local-windows.yml`
2. Klicke "Run workflow"
3. Wähle Branch: `dev`
4. Klicke "Run workflow"

### Automatisch nach Push

Nach Push zu `dev`:
1. "Dev Branch CI & Build" läuft → baut Image
2. "Deploy Dev to Local Machine (Windows Runner)" läuft automatisch
3. Windows Runner führt PowerShell-Script aus
4. Container wird automatisch deployed

## 🔧 Konfiguration

### Environment-Variablen für Runner

Falls das PVS-Verzeichnis nicht standardmäßig `D:\workspace\pvs` ist:

**Option 1: System-Umgebungsvariable**
```powershell
[System.Environment]::SetEnvironmentVariable('PVS_LOCAL_PATH', 'D:\workspace\pvs', 'Machine')
# Runner-Service neu starten
Restart-Service actions.runner.*
```

**Option 2: Runner-Environment**
```powershell
# Edit Runner-Service
Get-Service actions.runner.* | ForEach-Object {
  $serviceName = $_.Name
  # Environment-Variablen setzen (via sc.exe oder GUI)
}
```

**Option 3: GitHub Secrets** (wird vom Workflow übergeben)

Der Workflow nutzt automatisch:
- `PVS_LOCAL_PATH` Environment-Variable (falls gesetzt)
- Fallback auf `D:\workspace\pvs`

## 🐛 Troubleshooting

### Runner erscheint nicht online

```powershell
# Runner-Status prüfen
cd C:\actions-runner
.\run.cmd  # Im Vordergrund starten, um Fehler zu sehen

# Logs prüfen
Get-Content .\_diag\Runner_*.log -Tail 50
```

### Runner läuft, aber Workflow wird nicht ausgeführt

**Prüfe Runner-Labels:**
1. GitHub → Settings → Actions → Runners
2. Runner → Edit
3. Labels prüfen: Muss `windows` enthalten sein!

**Prüfe Workflow:**
- `runs-on: [self-hosted, windows]` muss zu Runner-Labels passen

### Service läuft unter Network Service

**Problem:** Service läuft als "NT-AUTORITÄT\Netzwerkdienst" statt als Benutzer.

**Symptome:**
- Docker Desktop nicht erreichbar
- Environment-Variablen nicht verfügbar
- Dateizugriff-Fehler

**Lösung 1: Service neu einrichten mit Setup-Script:**

```powershell
# PowerShell als Administrator
cd D:\workspace\pvs\scripts\local

# 1. Token holen oder resetten:
# GitHub → Settings → Actions → Runners → Runner → Configure → Reset Token

# 2. Service neu einrichten (entfernt alten automatisch)
.\setup-runner-service.ps1 -RunnerPath "C:\active-runners"
```

**Lösung 2: Service manuell neu installieren:**

```powershell
# PowerShell als Administrator
cd C:\active-runners

# 1. Token holen oder resetten:
# GitHub → Settings → Actions → Runners → Runner → Configure → Reset Token

# 2. Service entfernen
.\config.cmd remove --token <ALTES_TOKEN>

# 3. Service als Benutzer neu installieren
.\config.cmd --url https://github.com/bbajor/pvs --token <NEUES_TOKEN> --runasservice --user "$env:USERDOMAIN\$env:USERNAME"
```

**Lösung 3: Scheduled Task als Alternative:**

Falls Service-Probleme weiterhin bestehen, nutze den Task Scheduler:

```powershell
# PowerShell als Administrator
cd D:\workspace\pvs\scripts\local
.\setup-runner-task.ps1 -RunnerPath "C:\active-runners"
```

### PowerShell-Script fehlschlägt

```powershell
# Script manuell testen
cd D:\workspace\pvs
.\scripts\local\auto-update-dev.ps1

# Prüfe Logs
Get-Content pvs-auto-update.log -Tail 50
```

### Docker nicht erreichbar

```powershell
# Docker Desktop muss laufen!
Get-Process "Docker Desktop" -ErrorAction SilentlyContinue

# Falls nicht: Docker Desktop starten
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
```

## 🔄 Runner aktualisieren

```powershell
cd C:\actions-runner

# Runner stoppen
Stop-Service actions.runner.*
# Oder: .\svc\stop.cmd

# Neue Version herunterladen
# (siehe Schritt 1)

# Service neu starten
Start-Service actions.runner.*
# Oder: .\svc\start.cmd
```

## 📝 Nützliche Befehle

```powershell
# Runner-Status
Get-Service actions.runner.* | Format-Table Name, Status, StartType

# Runner stoppen
Stop-Service actions.runner.*

# Runner starten
Start-Service actions.runner.*

# Runner entfernen
cd C:\actions-runner
.\config.cmd remove --token <TOKEN>
Stop-Service actions.runner.*
.\svc\uninstall.cmd
```

## ✅ Checkliste

- [ ] Runner heruntergeladen
- [ ] Token von GitHub geholt
- [ ] Runner konfiguriert mit Label `windows`
- [ ] Runner als Service installiert
- [ ] Runner erscheint online in GitHub
- [ ] `PVS_LOCAL_PATH` Environment-Variable gesetzt (optional)
- [ ] Test-Workflow manuell ausgeführt
- [ ] Automatischer Trigger nach Push getestet

## 🎯 Nächste Schritte

Nach erfolgreichem Setup:

1. **Teste manuell:**
   - GitHub Actions → "Deploy Dev to Local Machine (Windows Runner)" → Run workflow

2. **Teste automatisch:**
   - Push zu `dev` Branch
   - Workflow sollte automatisch laufen

3. **Prüfe Logs:**
   - GitHub Actions → Workflow → Logs prüfen
   - Lokal: `pvs-auto-update.log`

Fertig! 🎉 Jeder erfolgreiche Build deployed nun automatisch auf deinem Windows-Rechner.

