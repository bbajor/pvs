# Podman WSL-Setup mit Storage auf Laufwerk D:
# Löscht alte Podman-Daten und erstellt eine neue Maschine mit WSL-Backend
# Konfiguriert Storage-Pfad für Images, Container etc. auf D:\podman-storage

# UTF-8 Encoding für korrekte Umlaut-Darstellung
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$ErrorActionPreference = "Stop"

Write-Host "🔧 Podman WSL-Setup mit Storage auf Laufwerk D:" -ForegroundColor Cyan
Write-Host ""

# Prüfe ob Podman installiert ist
$podmanCmd = Get-Command podman -ErrorAction SilentlyContinue
if (-not $podmanCmd) {
    Write-Host "❌ Podman ist nicht installiert oder nicht im PATH" -ForegroundColor Red
    Write-Host "   Bitte installiere Podman Desktop oder Podman für Windows" -ForegroundColor Yellow
    exit 1
}

# Prüfe Podman-Version
Write-Host "📋 Prüfe Podman-Installation..." -ForegroundColor Cyan
$null = podman --version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Podman ist nicht funktionsfähig" -ForegroundColor Red
    exit 1
}
$podmanVersion = podman --version
Write-Host "   ✓ $podmanVersion" -ForegroundColor Green

# Prüfe ob Laufwerk D: existiert
Write-Host ""
Write-Host "📋 Prüfe Laufwerk D:..." -ForegroundColor Cyan
if (-not (Test-Path "D:\")) {
    Write-Host "❌ Laufwerk D: existiert nicht" -ForegroundColor Red
    exit 1
}
Write-Host "   ✓ Laufwerk D: verfügbar" -ForegroundColor Green

# Prüfe verfügbaren Speicher auf D:
$drive = Get-PSDrive D -ErrorAction SilentlyContinue
if ($drive) {
    $freeSpaceGB = [math]::Round($drive.Free / 1GB, 2)
    Write-Host "   ℹ️  Verfügbarer Speicher auf D:: $freeSpaceGB GB" -ForegroundColor Gray
}

# Definiere Storage-Pfad
$storagePath = "D:\podman-storage"
Write-Host ""
Write-Host "📁 Storage-Pfad: $storagePath" -ForegroundColor Cyan

# Liste vorhandene Podman-Maschinen
Write-Host ""
Write-Host "📋 Prüfe vorhandene Podman-Maschinen..." -ForegroundColor Cyan
$machines = podman machine list --format json 2>&1 | ConvertFrom-Json -ErrorAction SilentlyContinue
if ($machines -and $machines.Count -gt 0) {
    Write-Host "   Gefundene Maschinen:" -ForegroundColor Yellow
    foreach ($machine in $machines) {
        Write-Host "     - $($machine.Name) (Status: $($machine.Running))" -ForegroundColor Gray
    }
    
    Write-Host ""
    $response = Read-Host "⚠️  Vorhandene Maschinen werden gelöscht. Fortfahren? (j/N)"
    if ($response -ne "j" -and $response -ne "J" -and $response -ne "y" -and $response -ne "Y") {
        Write-Host "❌ Abgebrochen" -ForegroundColor Yellow
        exit 0
    }
    
    # Lösche alle vorhandenen Maschinen
    Write-Host ""
    Write-Host "🗑️  Lösche vorhandene Podman-Maschinen..." -ForegroundColor Cyan
    foreach ($machine in $machines) {
        Write-Host "   → Lösche Maschine: $($machine.Name)" -ForegroundColor Gray
        if ($machine.Running -eq "running") {
            podman machine stop $machine.Name 2>&1 | Out-Null
        }
        podman machine rm -f $machine.Name 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "     ✓ Gelöscht" -ForegroundColor Green
        } else {
            Write-Host "     ⚠️  Warnung beim Löschen (möglicherweise bereits gelöscht)" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "   ✓ Keine vorhandenen Maschinen gefunden" -ForegroundColor Green
}

# Lösche alten Storage-Ordner falls vorhanden
Write-Host ""
Write-Host "🗑️  Bereinige alten Storage-Ordner..." -ForegroundColor Cyan
if (Test-Path $storagePath) {
    Write-Host "   → Lösche: $storagePath" -ForegroundColor Gray
    try {
        Remove-Item -Path $storagePath -Recurse -Force -ErrorAction Stop
        Write-Host "     ✓ Gelöscht" -ForegroundColor Green
    } catch {
        Write-Host "     ⚠️  Warnung: Konnte nicht vollständig gelöscht werden: $($_.Exception.Message)" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ✓ Kein alter Storage-Ordner gefunden" -ForegroundColor Green
}

# Erstelle neuen Storage-Ordner
Write-Host ""
Write-Host "📁 Erstelle Storage-Ordner..." -ForegroundColor Cyan
try {
    New-Item -Path $storagePath -ItemType Directory -Force | Out-Null
    Write-Host "   ✓ Ordner erstellt: $storagePath" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Fehler beim Erstellen des Ordners: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Erstelle neue Podman-Maschine mit WSL-Backend
Write-Host ""
Write-Host "🚀 Erstelle neue Podman-Maschine mit WSL-Backend..." -ForegroundColor Cyan
$machineName = "podman-machine-wsl"

# Prüfe ob WSL verfügbar ist
$wslAvailable = Get-Command wsl -ErrorAction SilentlyContinue
if (-not $wslAvailable) {
    Write-Host "   ⚠️  WSL-Befehl nicht gefunden, verwende Standard-Backend" -ForegroundColor Yellow
}

# Erstelle Maschine mit WSL-Backend
# Hinweis: Podman für Windows verwendet standardmäßig WSL2, wenn verfügbar
Write-Host "   → Erstelle Maschine: $machineName" -ForegroundColor Gray
podman machine init --rootful $machineName 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "   ❌ Fehler beim Erstellen der Maschine" -ForegroundColor Red
    Write-Host "   💡 Tipp: Prüfe ob WSL2 installiert ist (wsl --status)" -ForegroundColor Yellow
    exit 1
}

Write-Host "     ✓ Maschine erstellt" -ForegroundColor Green

# Starte Maschine
Write-Host ""
Write-Host "🚀 Starte Podman-Maschine..." -ForegroundColor Cyan
podman machine start $machineName 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "   ❌ Fehler beim Starten der Maschine" -ForegroundColor Red
    exit 1
}

Write-Host "     ✓ Maschine gestartet" -ForegroundColor Green

# Warte kurz, damit Maschine vollständig hochgefahren ist
Start-Sleep -Seconds 3

# Konfiguriere Storage-Pfad
# Hinweis: Podman-Maschinen speichern Daten standardmäßig in WSL-Distribution
# Für Windows müssen wir die Maschinen-Konfiguration anpassen
Write-Host ""
Write-Host "⚙️  Konfiguriere Storage-Pfad..." -ForegroundColor Cyan

# Prüfe Podman-Konfiguration
$podmanConfigPath = "$env:USERPROFILE\.config\containers\containers.conf"
$podmanConfigDir = Split-Path -Parent $podmanConfigPath

if (-not (Test-Path $podmanConfigDir)) {
    New-Item -Path $podmanConfigDir -ItemType Directory -Force | Out-Null
}

# Erstelle/aktualisiere containers.conf mit Storage-Pfad
Write-Host "   → Konfiguriere containers.conf..." -ForegroundColor Gray

$configContent = "# Podman-Konfiguration - Storage auf D: konfiguriert`n"
$configContent += "# Generiert von setup-podman-wsl-d-drive.ps1`n"
$configContent += "`n"
$configContent += "[engine]`n"
$configContent += "# Storage-Pfad für Images, Container etc.`n"
$configContent += "graphroot = `"$storagePath`"`n"
$configContent += "`n"
$configContent += "[containers]`n"
$configContent += "# Weitere Container-Einstellungen können hier hinzugefügt werden`n"

try {
    Set-Content -Path $podmanConfigPath -Value $configContent -Force
    Write-Host "     ✓ Konfiguration gespeichert" -ForegroundColor Green
} catch {
    Write-Host "     ⚠️  Warnung: Konnte containers.conf nicht schreiben: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "     ℹ️  Du kannst die Konfiguration manuell in $podmanConfigPath anpassen" -ForegroundColor Gray
}

# Prüfe ob Maschine läuft
Write-Host ""
Write-Host "📋 Prüfe Maschinen-Status..." -ForegroundColor Cyan
$machineInfo = podman machine list --format json 2>&1 | ConvertFrom-Json -ErrorAction SilentlyContinue
if ($machineInfo) {
    $currentMachine = $machineInfo | Where-Object { $_.Name -eq $machineName }
    if ($currentMachine) {
        Write-Host "   Maschine: $($currentMachine.Name)" -ForegroundColor Green
        Write-Host "   Status: $($currentMachine.Running)" -ForegroundColor $(if ($currentMachine.Running -eq "running") { "Green" } else { "Yellow" })
        Write-Host "   CPU: $($currentMachine.CPUs)" -ForegroundColor Gray
        Write-Host "   Memory: $($currentMachine.Memory)" -ForegroundColor Gray
    }
}

# Teste Podman-Verbindung
Write-Host ""
Write-Host "🧪 Teste Podman-Verbindung..." -ForegroundColor Cyan
$null = podman ps 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "   ✓ Podman funktioniert" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Podman-Verbindungstest fehlgeschlagen" -ForegroundColor Yellow
    Write-Host "   💡 Stelle sicher, dass die Maschine läuft: podman machine start $machineName" -ForegroundColor Gray
}

Write-Host ""
Write-Host "✅ Setup abgeschlossen!" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Nächste Schritte:" -ForegroundColor Cyan
Write-Host "   1. Prüfe Maschinen-Status: podman machine list" -ForegroundColor Gray
Write-Host "   2. Starte Maschine falls nötig: podman machine start $machineName" -ForegroundColor Gray
Write-Host "   3. Teste mit: podman run hello-world" -ForegroundColor Gray
Write-Host "   4. Storage-Pfad: $storagePath" -ForegroundColor Gray
Write-Host ""
Write-Host "Hinweis: Fuer n8n, Ollama und weitere Container sollte jetzt genug Speicher auf D: verfuegbar sein." -ForegroundColor Yellow

