# GitHub Runner als Windows Service einrichten
# Dieses Script richtet den GitHub Runner unter C:/active-runners als Windows Service ein
# damit er automatisch beim Boot startet und dauerhaft läuft.

param(
    [string]$RunnerPath = "C:\active-runners",
    [string]$ServiceName = "GitHubActionsRunner",
    [switch]$UseCurrentUser = $true
)

Write-Host "[SETUP] GitHub Runner Service Setup" -ForegroundColor Cyan
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
    Write-Host "   .\setup-runner-service.ps1 -RunnerPath 'C:\active-runners'" -ForegroundColor Gray
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

# Prüfe ob Service bereits existiert
$existingService = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($existingService) {
    Write-Host "[WARN] Service '$ServiceName' existiert bereits." -ForegroundColor Yellow
    $response = Read-Host "   Möchtest du ihn entfernen und neu einrichten? (j/n)"
    if ($response -ne "j" -and $response -ne "J") {
        Write-Host "   Abgebrochen." -ForegroundColor Gray
        exit 0
    }
    
    # Service entfernen
    Write-Host "   Entferne vorhandenen Service..." -ForegroundColor Yellow
    if ($existingService.Status -eq "Running") {
        Stop-Service -Name $ServiceName -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
    
    # Service entfernen (falls via svc installiert)
    $svcUninstall = Join-Path $RunnerPath "svc\uninstall.cmd"
    if (Test-Path $svcUninstall) {
        Write-Host "   Nutze Runner's uninstall.cmd..." -ForegroundColor Gray
        & $svcUninstall 2>&1 | Out-Null
    }
    
    # Fallback: Via sc.exe entfernen
    sc.exe delete $ServiceName 2>&1 | Out-Null
    Start-Sleep -Seconds 2
}

# Prüfe ob GitHub Runner eigene Service-Tools hat
$svcInstall = Join-Path $RunnerPath "svc\install.cmd"
$svcConfig = Join-Path $RunnerPath "config.cmd"

if ((Test-Path $svcInstall) -or (Test-Path $svcConfig)) {
    Write-Host "[METHOD] Nutze GitHub Runner's eigene Service-Installation..." -ForegroundColor Cyan
    Write-Host ""
    
    # Token für Service-Setup holen
    Write-Host "[TOKEN] Du benoetigst einen Runner-Registration-Token von GitHub:" -ForegroundColor Yellow
    Write-Host "   1. Gehe zu: https://github.com/bbajor/pvs/settings/actions/runners/new" -ForegroundColor Cyan
    Write-Host "   2. Waehle 'Windows' als Betriebssystem" -ForegroundColor Cyan
    Write-Host "   3. Kopiere den Token (beginnt mit A...)" -ForegroundColor Cyan
    Write-Host ""
    $token = Read-Host "   Token eingeben"
    
    if ([string]::IsNullOrWhiteSpace($token)) {
        Write-Host "[ERROR] Kein Token eingegeben. Abgebrochen." -ForegroundColor Red
        exit 1
    }
    
    # Service als Benutzer installieren (wichtig für Docker!)
    Write-Host ""
    Write-Host "[INSTALL] Installiere Service..." -ForegroundColor Cyan
    
    if ($UseCurrentUser) {
        $user = "$env:USERDOMAIN\$env:USERNAME"
        Write-Host "   Service läuft als: $user" -ForegroundColor Gray
        
        # Nutze config.cmd mit --runasservice
        if (Test-Path $svcConfig) {
            $configArgs = @(
                "--url", "https://github.com/bbajor/pvs",
                "--token", $token,
                "--runasservice",
                "--user", $user,
                "--replace"
            )
            
            Set-Location $RunnerPath
            & $svcConfig $configArgs
            $installSuccess = $LASTEXITCODE -eq 0
        } else {
            Write-Host "[ERROR] config.cmd nicht gefunden" -ForegroundColor Red
            $installSuccess = $false
        }
    } else {
        # Als System-Service installieren
        Write-Host "   Service laeuft als: SYSTEM" -ForegroundColor Gray
        if (Test-Path $svcConfig) {
            $configArgs = @(
                "--url", "https://github.com/bbajor/pvs",
                "--token", $token,
                "--runasservice",
                "--replace"
            )
            
            Set-Location $RunnerPath
            & $svcConfig $configArgs
            $installSuccess = $LASTEXITCODE -eq 0
        } elseif (Test-Path $svcInstall) {
            Set-Location $RunnerPath
            & $svcInstall
            $installSuccess = $LASTEXITCODE -eq 0
        } else {
            Write-Host "[ERROR] Keine Service-Installationsdatei gefunden" -ForegroundColor Red
            $installSuccess = $false
        }
    }
    
    if ($installSuccess) {
        Write-Host ""
        Write-Host "[OK] Service erfolgreich installiert!" -ForegroundColor Green
        
        # Service starten
        Write-Host "[START] Starte Service..." -ForegroundColor Cyan
        $serviceName = (Get-Service | Where-Object { $_.DisplayName -like "*Actions*Runner*" -or $_.Name -like "*actions*runner*" }).Name | Select-Object -First 1
        
        if ($serviceName) {
            Start-Service -Name $serviceName -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 3
            $service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
            if ($service -and $service.Status -eq "Running") {
                Write-Host "[OK] Service laeuft jetzt!" -ForegroundColor Green
            } else {
                Write-Host "[WARN] Service wurde installiert, laeuft aber noch nicht." -ForegroundColor Yellow
                Write-Host "   Starte manuell mit: Start-Service -Name '$serviceName'" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host ""
        Write-Host "[WARN] Service-Installation hat nicht funktioniert." -ForegroundColor Yellow
        Write-Host "   Versuche alternative Methode..." -ForegroundColor Yellow
        $installSuccess = $false
    }
    
} else {
    $installSuccess = $false
}

# Fallback: Eigene Service-Lösung mit sc.exe
if (-not $installSuccess) {
    Write-Host ""
    Write-Host "[SETUP] Nutze Windows Service Manager (sc.exe)..." -ForegroundColor Cyan
    Write-Host ""
    
    # PowerShell-Wrapper-Script erstellen
    $wrapperScript = Join-Path $RunnerPath "service-wrapper.ps1"
    $wrapperContent = @"
# Service-Wrapper fuer GitHub Runner
# Wird vom Windows Service ausgefuehrt

Set-Location '$RunnerPath'
& '$runCmdPath'
"@
    
    Write-Host "[WRAPPER] Erstelle Service-Wrapper..." -ForegroundColor Gray
    Set-Content -Path $wrapperScript -Value $wrapperContent -Encoding UTF8
    
    # Service mit sc.exe erstellen
    Write-Host "[CREATE] Erstelle Windows Service..." -ForegroundColor Cyan
    
    if ($UseCurrentUser) {
        # Als aktueller Benutzer
        $user = "$env:USERDOMAIN\$env:USERNAME"
        $password = Read-Host "   Gebe dein Windows-Passwort ein (für Service-Anmeldung)" -AsSecureString
        $passwordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($password))
        
        $binPath = "powershell.exe -ExecutionPolicy Bypass -File `"$wrapperScript`""
        $result = sc.exe create $ServiceName binPath= "$binPath" start= auto obj= $user password= "$passwordPlain" 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[ERROR] Service-Erstellung fehlgeschlagen:" -ForegroundColor Red
            Write-Host $result -ForegroundColor Red
            Write-Host ""
            Write-Host "[TIP] Alternative: Verwende Task Scheduler statt Service" -ForegroundColor Yellow
            exit 1
        }
    } else {
        # Als System-Service (weniger empfohlen für Docker)
        $binPath = "powershell.exe -ExecutionPolicy Bypass -File `"$wrapperScript`""
        $result = sc.exe create $ServiceName binPath= "$binPath" start= auto 2>&1
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[ERROR] Service-Erstellung fehlgeschlagen:" -ForegroundColor Red
            Write-Host $result -ForegroundColor Red
            exit 1
        }
    }
    
    Write-Host "[OK] Service erstellt!" -ForegroundColor Green
    
    # Service starten
    Write-Host "[START] Starte Service..." -ForegroundColor Cyan
    Start-Service -Name $ServiceName -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
    
    $service = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    if ($service) {
        if ($service.Status -eq "Running") {
            Write-Host "[OK] Service laeuft jetzt!" -ForegroundColor Green
        } else {
            Write-Host "[WARN] Service wurde erstellt, laeuft aber noch nicht." -ForegroundColor Yellow
            Write-Host "   Status: $($service.Status)" -ForegroundColor Gray
            Write-Host "   Starte manuell mit: Start-Service -Name '$ServiceName'" -ForegroundColor Gray
        }
    }
}

Write-Host ""
Write-Host "[STATUS] Service-Status:" -ForegroundColor Cyan
Get-Service | Where-Object { $_.DisplayName -like "*Actions*Runner*" -or $_.DisplayName -like "*GitHub*Runner*" -or $_.Name -eq $ServiceName } | Format-Table Name, DisplayName, Status, StartType -AutoSize

Write-Host ""
Write-Host "[OK] Setup abgeschlossen!" -ForegroundColor Green
Write-Host ""
Write-Host "[CMDS] Nuetzliche Befehle:" -ForegroundColor Cyan
Write-Host "   Service-Status:   Get-Service | Where-Object { `$_.Name -like '*actions*runner*' }" -ForegroundColor Gray
Write-Host "   Service starten:  Start-Service -Name '$ServiceName'" -ForegroundColor Gray
Write-Host "   Service stoppen:  Stop-Service -Name '$ServiceName'" -ForegroundColor Gray
Write-Host "   Service entfernen: sc.exe delete $ServiceName" -ForegroundColor Gray
Write-Host ""
Write-Host "[INFO] Pruefe Runner-Status auf GitHub:" -ForegroundColor Cyan
Write-Host "   https://github.com/bbajor/pvs/settings/actions/runners" -ForegroundColor Blue

