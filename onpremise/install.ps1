# PVS OnPremise Installer für Windows
# Installiert PVS auf einem Windows-System mit Podman

param(
    [string]$InstallDir = "C:\Program Files\PVS",
    [switch]$SkipService
)

$ErrorActionPreference = "Stop"

# Farben für Output (Windows-kompatibel)
function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

Write-ColorOutput Green "=== PVS OnPremise Installer ==="
Write-Output ""

# Prüfe Administrator-Rechte
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-ColorOutput Red "Fehler: Dieses Skript muss als Administrator ausgeführt werden"
    exit 1
}

# Prüfe Podman
Write-Output "Prüfe Podman-Installation..."
try {
    $podmanVersion = podman --version 2>&1
    Write-ColorOutput Green "✓ Podman gefunden: $podmanVersion"
} catch {
    Write-ColorOutput Yellow "Podman nicht gefunden. Bitte installiere Podman Desktop von:"
    Write-Output "  https://podman-desktop.io/"
    Write-Output ""
    Write-Output "Nach der Installation starte dieses Skript erneut."
    exit 1
}

# Prüfe podman-compose
Write-Output "Prüfe podman-compose..."
try {
    $composeVersion = podman-compose --version 2>&1
    Write-ColorOutput Green "✓ podman-compose gefunden: $composeVersion"
} catch {
    Write-ColorOutput Yellow "podman-compose nicht gefunden. Installiere podman-compose..."
    try {
        pip install podman-compose
        Write-ColorOutput Green "✓ podman-compose installiert"
    } catch {
        Write-ColorOutput Red "Fehler beim Installieren von podman-compose."
        Write-Output "Bitte manuell installieren mit: pip install podman-compose"
        exit 1
    }
}

# Erstelle Installations-Verzeichnis
Write-Output "Erstelle Installations-Verzeichnis..."
if (-not (Test-Path $InstallDir)) {
    New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
}
New-Item -ItemType Directory -Path "$InstallDir\backups" -Force | Out-Null
New-Item -ItemType Directory -Path "$InstallDir\logs" -Force | Out-Null

# Kopiere Dateien
Write-Output "Kopiere Installationsdateien..."
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Copy-Item "$ScriptDir\podman-compose.onpremise.yml" -Destination "$InstallDir\" -Force
Copy-Item "$ScriptDir\env.example" -Destination "$InstallDir\.env.example" -Force

# Erstelle .env falls nicht vorhanden
$envFile = "$InstallDir\.env"
if (-not (Test-Path $envFile)) {
    Write-Output "Erstelle .env-Datei..."
    Copy-Item "$InstallDir\.env.example" -Destination $envFile -Force
    
    # Generiere sichere Passwörter
    Write-Output "Generiere sichere Passwörter..."
    $postgresPassword = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 25 | ForEach-Object {[char]$_})
    $kbvPassword = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 25 | ForEach-Object {[char]$_})
    $smtpKey = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object {[char]$_})
    
    (Get-Content $envFile) -replace 'CHANGE_ME_SECURE_PASSWORD', $postgresPassword | Set-Content $envFile
    (Get-Content $envFile) -replace 'KBV_DB_PASSWORD=CHANGE_ME_SECURE_PASSWORD', "KBV_DB_PASSWORD=$kbvPassword" | Set-Content $envFile
    (Get-Content $envFile) -replace 'SMTP_ENCRYPTION_KEY=$', "SMTP_ENCRYPTION_KEY=$smtpKey" | Set-Content $envFile
    
    Write-ColorOutput Green "✓ .env-Datei erstellt mit generierten Passwörtern"
    Write-ColorOutput Yellow "⚠️  WICHTIG: Speichere die Passwörter sicher!"
    Write-Output "   POSTGRES_PASSWORD: $postgresPassword"
    Write-Output "   KBV_DB_PASSWORD: $kbvPassword"
    Write-Output "   SMTP_ENCRYPTION_KEY: $smtpKey"
} else {
    Write-ColorOutput Green "✓ .env-Datei existiert bereits"
}

# Erstelle Start-Skript
Write-Output "Erstelle Start-Skript..."
$startScript = @"
@echo off
cd /d "$InstallDir"
podman-compose -f podman-compose.onpremise.yml --env-file .env up -d
"@
$startScript | Out-File -FilePath "$InstallDir\start-pvs.bat" -Encoding ASCII

# Erstelle Stop-Skript
Write-Output "Erstelle Stop-Skript..."
$stopScript = @"
@echo off
cd /d "$InstallDir"
podman-compose -f podman-compose.onpremise.yml down
"@
$stopScript | Out-File -FilePath "$InstallDir\stop-pvs.bat" -Encoding ASCII

# Erstelle Status-Skript
Write-Output "Erstelle Status-Skript..."
$statusScript = @"
@echo off
cd /d "$InstallDir"
podman-compose -f podman-compose.onpremise.yml ps
"@
$statusScript | Out-File -FilePath "$InstallDir\status-pvs.bat" -Encoding ASCII

# Erstelle Windows Task für Auto-Start (optional)
if (-not $SkipService) {
    Write-Output "Erstelle Windows Task für Auto-Start..."
    $taskName = "PVS-OnPremise-Start"
    
    # Entferne existierende Task falls vorhanden
    $existingTask = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    if ($existingTask) {
        Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
    }
    
    # Erstelle neue Task
    $action = New-ScheduledTaskAction -Execute "cmd.exe" -Argument "/c `"$InstallDir\start-pvs.bat`""
    $trigger = New-ScheduledTaskTrigger -AtStartup
    $principal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Highest
    $settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable
    
    Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Description "Startet PVS OnPremise Container nach Systemstart" | Out-Null
    
    Write-ColorOutput Green "✓ Windows Task für Auto-Start erstellt"
}

Write-Output ""
Write-ColorOutput Green "=== Installation abgeschlossen ==="
Write-Output ""
Write-Output "Nächste Schritte:"
Write-Output "1. Bearbeite $InstallDir\.env und passe die Konfiguration an"
Write-Output "2. Starte PVS mit: $InstallDir\start-pvs.bat"
Write-Output "3. Prüfe den Status mit: $InstallDir\status-pvs.bat"
Write-Output "4. Stoppe PVS mit: $InstallDir\stop-pvs.bat"
Write-Output ""
Write-Output "Die Anwendung ist nach dem Start erreichbar unter:"
Write-Output "  http://localhost:8080"
Write-Output ""

