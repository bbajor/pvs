# GitHub Runner als Windows Scheduled Task einrichten
# Alternative zu Service, falls Service-Setup Probleme macht
# Task läuft beim Anmelden und bleibt aktiv

param(
    [string]$RunnerPath = "C:\active-runners",
    [string]$TaskName = "GitHubActionsRunner"
)

Write-Host "[SETUP] GitHub Runner Task Scheduler Setup" -ForegroundColor Cyan
Write-Host ""

# Prüfe Administrator-Rechte
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "[ERROR] Dieses Script benoetigt Administrator-Rechte!" -ForegroundColor Red
    Write-Host "   Bitte PowerShell als Administrator starten und erneut ausfuehren." -ForegroundColor Yellow
    exit 1
}

# Prüfe ob Runner-Verzeichnis existiert
if (-not (Test-Path $RunnerPath)) {
    Write-Host "[ERROR] Runner-Verzeichnis nicht gefunden: $RunnerPath" -ForegroundColor Red
    Write-Host "   Bitte gib den richtigen Pfad an, z.B.:" -ForegroundColor Yellow
    Write-Host "   .\setup-runner-task.ps1 -RunnerPath 'C:\active-runners'" -ForegroundColor Gray
    exit 1
}

# Prüfe ob run.cmd existiert
$runCmdPath = Join-Path $RunnerPath "run.cmd"
if (-not (Test-Path $runCmdPath)) {
    Write-Host "[ERROR] run.cmd nicht gefunden in: $RunnerPath" -ForegroundColor Red
    Write-Host "   Stelle sicher, dass der Runner korrekt installiert ist." -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] Runner-Verzeichnis gefunden: $RunnerPath" -ForegroundColor Green
Write-Host ""

# Prüfe ob Task bereits existiert
$existingTask = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
if ($existingTask) {
    Write-Host "[WARN] Task '$TaskName' existiert bereits." -ForegroundColor Yellow
    $response = Read-Host "   Möchtest du ihn entfernen und neu einrichten? (j/n)"
    if ($response -ne "j" -and $response -ne "J") {
        Write-Host "   Abgebrochen." -ForegroundColor Gray
        exit 0
    }
    
    Write-Host "   Entferne vorhandenen Task..." -ForegroundColor Yellow
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue
}

# Task-Action erstellen
Write-Host "[CREATE] Erstelle Scheduled Task..." -ForegroundColor Cyan

$action = New-ScheduledTaskAction -Execute "cmd.exe" `
    -Argument "/c `"$runCmdPath`"" `
    -WorkingDirectory $RunnerPath

# Task-Trigger: Beim Systemstart und bei Anmeldung
$trigger1 = New-ScheduledTaskTrigger -AtStartup
$trigger2 = New-ScheduledTaskTrigger -AtLogOn

# Task-Einstellungen
$settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -RunOnlyIfNetworkAvailable:$false `
    -RestartCount 3 `
    -RestartInterval (New-TimeSpan -Minutes 1)

# Task-Principal: Als aktueller Benutzer ausführen
$principal = New-ScheduledTaskPrincipal `
    -UserId "$env:USERDOMAIN\$env:USERNAME" `
    -LogonType Interactive `
    -RunLevel Highest

# Task registrieren
try {
    Register-ScheduledTask `
        -TaskName $TaskName `
        -Action $action `
        -Trigger @($trigger1, $trigger2) `
        -Settings $settings `
        -Principal $principal `
        -Description "GitHub Actions Runner - Läuft automatisch im Hintergrund" `
        -Force | Out-Null
    
    Write-Host "[OK] Task erfolgreich erstellt!" -ForegroundColor Green
    
    # Task sofort starten
    Write-Host "[START] Starte Task..." -ForegroundColor Cyan
    Start-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
    
    $task = Get-ScheduledTask -TaskName $TaskName
    if ($task.State -eq "Running") {
        Write-Host "[OK] Task laeuft jetzt!" -ForegroundColor Green
    } else {
        Write-Host "[WARN] Task wurde erstellt, Status: $($task.State)" -ForegroundColor Yellow
        Write-Host "   Starte manuell mit: Start-ScheduledTask -TaskName '$TaskName'" -ForegroundColor Gray
    }
    
} catch {
    Write-Host "[ERROR] Fehler beim Erstellen des Tasks:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[STATUS] Task-Status:" -ForegroundColor Cyan
Get-ScheduledTask -TaskName $TaskName | Format-Table TaskName, State, LastRunTime, NextRunTime -AutoSize

Write-Host ""
Write-Host "[OK] Setup abgeschlossen!" -ForegroundColor Green
Write-Host ""
Write-Host "[CMDS] Nuetzliche Befehle:" -ForegroundColor Cyan
Write-Host "   Task-Status:   Get-ScheduledTask -TaskName '$TaskName'" -ForegroundColor Gray
Write-Host "   Task starten:  Start-ScheduledTask -TaskName '$TaskName'" -ForegroundColor Gray
Write-Host "   Task stoppen:  Stop-ScheduledTask -TaskName '$TaskName'" -ForegroundColor Gray
Write-Host "   Task entfernen: Unregister-ScheduledTask -TaskName '$TaskName' -Confirm:`$false" -ForegroundColor Gray
Write-Host ""
Write-Host "[INFO] Der Task startet automatisch:" -ForegroundColor Cyan
Write-Host "   - Beim Windows-Start" -ForegroundColor Gray
Write-Host "   - Bei deiner Anmeldung" -ForegroundColor Gray
Write-Host ""
Write-Host "[INFO] Pruefe Runner-Status auf GitHub:" -ForegroundColor Cyan
Write-Host "   https://github.com/bbajor/pvs/settings/actions/runners" -ForegroundColor Blue

