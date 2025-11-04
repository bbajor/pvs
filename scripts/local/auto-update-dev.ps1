# PowerShell Script für automatisches Update der lokalen Dev-Umgebung
# Kann als Scheduled Task genutzt werden
# 
# Setup als Scheduled Task:
# 1. Task Scheduler öffnen
# 2. Task erstellen -> "Erstellen Sie eine Aufgabe"
# 3. Allgemein:
#    - Name: "PVS Dev Auto-Update"
#    - "Mit höchsten Privilegien ausführen" aktivieren
# 4. Trigger:
#    - Neu -> "Beim Starten des Computers" + Verzögerung 5 Minuten
#    - Neu -> "Nach einem Ereignis" -> Benutzer-Anmeldung
#    - Neu -> "Wiederholen" -> Alle 5 Minuten
# 5. Aktionen:
#    - "Programm starten" -> PowerShell.exe
#    - Argumente: -ExecutionPolicy Bypass -File "D:\workspace\pvs\scripts\local\auto-update-dev.ps1"
#    - Starten in: D:\workspace\pvs

$ErrorActionPreference = "Stop"

$REPO_OWNER = if ($env:GITHUB_REPO_OWNER) { $env:GITHUB_REPO_OWNER } else { "bbajor" }
$IMAGE_NAME = "ghcr.io/${REPO_OWNER}/pvs:dev-latest"
$COMPOSE_FILE = "podman-compose.dev.yml"
$ENV_FILE = "podman-compose.dev.env"
$COMPOSE_DIR = if ($env:PVS_LOCAL_PATH) { $env:PVS_LOCAL_PATH } else { "D:\workspace\pvs" }
$LOG_FILE = Join-Path $COMPOSE_DIR "pvs-auto-update.log"

# Logging-Funktion
function Write-Log {
    param(
        [string]$Message,
        [string]$Level = "INFO"
    )
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logMessage = "[$timestamp] [$Level] $Message"
    
    # Console-Output mit Farben
    $color = switch ($Level) {
        "ERROR" { "Red" }
        "WARN" { "Yellow" }
        "OK" { "Green" }
        default { "White" }
    }
    Write-Host "[$Level] $Message" -ForegroundColor $color
    
    # Log-File (append)
    try {
        Add-Content -Path $LOG_FILE -Value $logMessage -Encoding UTF8 -ErrorAction SilentlyContinue
    } catch {
        # Ignore log errors
    }
}

if (-not (Test-Path $COMPOSE_DIR)) {
    Write-Log "Verzeichnis nicht gefunden: $COMPOSE_DIR" "ERROR"
    exit 1
}

Set-Location $COMPOSE_DIR
Write-Log "=== PVS Auto-Update gestartet ===" "INFO"
Write-Log "Verzeichnis: $COMPOSE_DIR" "INFO"
Write-Log "Pruefe auf neues dev Image: $IMAGE_NAME" "INFO"

# Prüfe ob Podman verfügbar ist
try {
    $null = podman --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Log "Podman nicht verfügbar" "ERROR"
        exit 1
    }
    Write-Log "Podman verfügbar" "OK"
    
    # Determine compose command
    $COMPOSE_CMD = "podman compose"
    try {
        $null = podman compose version 2>&1
        if ($LASTEXITCODE -ne 0) {
            $COMPOSE_CMD = "podman-compose"
        }
    } catch {
        $COMPOSE_CMD = "podman-compose"
    }
} catch {
    Write-Log "Podman nicht gefunden. Bitte Podman installieren." "ERROR"
    exit 1
}

# Prüfe ob Podman Login für private Images nötig ist
if ($env:GITHUB_TOKEN -and $env:GITHUB_USERNAME) {
    Write-Log "Podman Login für private Image..." "INFO"
    try {
        $loginOutput = $env:GITHUB_TOKEN | podman login ghcr.io -u $env:GITHUB_USERNAME --password-stdin 2>&1 | Out-String
        if ($LASTEXITCODE -eq 0) {
            Write-Log "Podman Login erfolgreich" "OK"
        } else {
            Write-Log "Podman Login fehlgeschlagen, versuche ohne Login..." "WARN"
        }
    } catch {
        Write-Log "Podman Login fehlgeschlagen: $($_.Exception.Message)" "WARN"
        Write-Log "Versuche Image-Pull ohne vorherigen Login..." "INFO"
    }
}

# Prüfe ob ein neues Image verfügbar ist
$currentImageId = $null
try {
    $currentImageId = podman images --format "{{.ID}}" $IMAGE_NAME 2>$null | Select-Object -First 1
    if ($null -eq $currentImageId -or $currentImageId -eq "") {
        $currentImageId = $null
    }
} catch {
    # Image existiert noch nicht lokal
    $currentImageId = $null
}

Write-Log "Pulling neues Image..." "INFO"
try {
    $pullOutput = podman pull $IMAGE_NAME 2>&1 | Out-String
    $pullExitCode = $LASTEXITCODE
    Write-Log "Pull Output: $pullOutput" "INFO"
} catch {
    $pullOutput = $_.Exception.Message
    $pullExitCode = 1
}

if ($pullExitCode -ne 0) {
    if ($pullOutput -match "unauthorized") {
        Write-Log "Image pull fehlgeschlagen - nicht autorisiert (ghcr.io Login erforderlich)" "WARN"
        Write-Log "Fuer oeffentliche Images: podman login ghcr.io" "INFO"
    } else {
        Write-Log "Image pull fehlgeschlagen (moeglicherweise noch nicht gebaut)" "WARN"
        Write-Log "Details: $pullOutput" "INFO"
    }
    Write-Log "=== Auto-Update beendet (keine Aenderung) ===" "INFO"
    exit 0
}

$newImageId = podman images --format "{{.ID}}" $IMAGE_NAME 2>$null | Select-Object -First 1
if ($null -eq $newImageId -or $newImageId -eq "") {
    $newImageId = $null
}

# Prüfe ob Container laufen
$containersRunning = $false
try {
    $runningContainers = & $COMPOSE_CMD -f $COMPOSE_FILE ps --format json 2>&1 | ConvertFrom-Json -ErrorAction SilentlyContinue
    if ($runningContainers) {
        $runningCount = ($runningContainers | Where-Object { $_.State -eq "running" }).Count
        $totalCount = $runningContainers.Count
        Write-Log "Container-Status: $runningCount von $totalCount laufen" "INFO"
        if ($runningCount -eq $totalCount -and $totalCount -gt 0) {
            $containersRunning = $true
        }
    }
} catch {
    # Keine Container oder Fehler - ignoriere
}

if ($currentImageId -eq $newImageId -and $null -ne $currentImageId) {
    Write-Log "Bereits neueste Version installiert (Image-ID: $currentImageId)" "OK"
    if ($containersRunning) {
        Write-Log "Alle Container laufen bereits" "OK"
        Write-Log "=== Auto-Update beendet (keine Aenderung) ===" "INFO"
        exit 0
    } else {
        Write-Log "Container laufen nicht - starte Container trotzdem..." "WARN"
        # Weiter mit Deployment
    }
}

Write-Log "Starte Deployment..." "INFO"

# Setze Environment-Variable für Image
$env:PVS_DEV_IMAGE = $IMAGE_NAME
Write-Log "Setze PVS_DEV_IMAGE=$IMAGE_NAME" "INFO"

# Prüfe ob podman-compose.dev.env existiert
$envFilePath = Join-Path $COMPOSE_DIR $ENV_FILE

# Stoppe und entferne alte Container (fuer sauberes Update)
Write-Log "Stoppe alte Container..." "INFO"
try {
    if (Test-Path $envFilePath) {
        & $COMPOSE_CMD -f $COMPOSE_FILE --env-file $ENV_FILE down 2>&1 | Out-String | Out-Null
    } else {
        & $COMPOSE_CMD -f $COMPOSE_FILE down 2>&1 | Out-String | Out-Null
    }
} catch {
    # Ignore errors (Container laufen vielleicht nicht)
}
Start-Sleep -Seconds 2

# Pull neueste Images via $COMPOSE_CMD (wichtig für Image-Updates!)
Write-Log "Pulling neueste Images via $COMPOSE_CMD..." "INFO"
try {
    if (Test-Path $envFilePath) {
        & $COMPOSE_CMD -f $COMPOSE_FILE --env-file $ENV_FILE pull 2>&1 | Out-String | Out-Null
    } else {
        & $COMPOSE_CMD -f $COMPOSE_FILE pull 2>&1 | Out-String | Out-Null
    }
    Write-Log "Image pull via $COMPOSE_CMD erfolgreich" "OK"
} catch {
    Write-Log "Image pull via $COMPOSE_CMD fehlgeschlagen, versuche trotzdem weiter..." "WARN"
}

if (-not (Test-Path $envFilePath)) {
    Write-Log "podman-compose.dev.env nicht gefunden - verwende Defaults" "WARN"
    Write-Log "Fuehre aus: $COMPOSE_CMD -f $COMPOSE_FILE up -d --force-recreate --pull always" "INFO"
    $composeOutput = & $COMPOSE_CMD -f $COMPOSE_FILE up -d --force-recreate --pull always 2>&1 | Out-String
    Write-Log "Compose Output: $composeOutput" "INFO"
} else {
    Write-Log "Nutze podman-compose.dev.env für Deployment" "INFO"
    Write-Log "Fuehre aus: $COMPOSE_CMD -f $COMPOSE_FILE --env-file $ENV_FILE up -d --force-recreate --pull always" "INFO"
    $composeOutput = & $COMPOSE_CMD -f $COMPOSE_FILE --env-file $ENV_FILE up -d --force-recreate --pull always 2>&1 | Out-String
    Write-Log "Compose Output: $composeOutput" "INFO"
}

if ($LASTEXITCODE -ne 0) {
    Write-Log "Deployment fehlgeschlagen! Exit-Code: $LASTEXITCODE" "ERROR"
    Write-Log "Details: $composeOutput" "ERROR"
    
    # Prüfe auf Port-Konflikt
    if ($composeOutput -match "bind.*access.*forbidden" -or $composeOutput -match "already in use" -or $composeOutput -match "Ports are not available") {
        Write-Log "Port-Konflikt erkannt - Port bereits belegt!" "WARN"
        
        # Versuche Prozess zu identifizieren
        try {
            $port5432 = Get-NetTCPConnection -LocalPort 5432 -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($port5432) {
                $procId = $port5432.OwningProcess
                $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
                if ($proc) {
                    Write-Log "Port 5432 wird genutzt von: $($proc.ProcessName) (PID: $procId)" "INFO"
                    if ($proc.ProcessName -match "postgres") {
                        Write-Log "Lokale PostgreSQL-Instanz gefunden - Container nutzt Port 5433" "INFO"
                        Write-Log "Moegliche Loesungen:" "INFO"
                        Write-Log "  1. Lokale PostgreSQL stoppen: Stop-Process -Id $procId" "INFO"
                        Write-Log "  2. Oder nutze bereits geaenderten Port 5433 in podman-compose.dev.yml" "INFO"
                    }
                }
            }
        } catch {
            # Ignore
        }
        
        Write-Log "Moegliche Loesung: Anderen Prozess auf Port beenden ODER Port in podman-compose.dev.yml aendern" "INFO"
    }
    
    Write-Log "=== Auto-Update fehlgeschlagen ===" "ERROR"
    exit 1
}

Write-Log "Deployment-Befehl erfolgreich ausgeführt" "OK"

Write-Log "Warte auf Health Check (30 Sekunden)..." "INFO"
Start-Sleep -Seconds 30

# Prüfe Status
Write-Log "Pruefe Container-Status..." "INFO"
$statusOutput = & $COMPOSE_CMD -f $COMPOSE_FILE ps 2>&1
$statusText = $statusOutput -join "`n"
Write-Log "Status Output: $statusText" "INFO"

# Prüfe alle Container
$psOutput = & $COMPOSE_CMD -f $COMPOSE_FILE ps --format json 2>&1 | ConvertFrom-Json -ErrorAction SilentlyContinue
if ($psOutput) {
    $allRunning = $true
    foreach ($container in $psOutput) {
        $state = $container.State
        $name = $container.Name
        Write-Log "Container $name : $state" "INFO"
        if ($state -ne "running" -and $state -ne "Up") {
            $allRunning = $false
        }
    }
    
    if ($allRunning) {
        Write-Log "Deployment erfolgreich - alle Container laufen" "OK"
        & $COMPOSE_CMD -f $COMPOSE_FILE ps
    } else {
        Write-Log "Deployment abgeschlossen, aber nicht alle Container laufen" "WARN"
        & $COMPOSE_CMD -f $COMPOSE_FILE ps
        Write-Log "Pruefe Logs..." "INFO"
        & $COMPOSE_CMD -f $COMPOSE_FILE logs --tail=50
    }
} elseif ($statusText -match "Up.*healthy") {
    Write-Log "Deployment erfolgreich" "OK"
    Invoke-Expression "$COMPOSE_CMD" -f $COMPOSE_FILE ps
} else {
    Write-Log "Deployment abgeschlossen, pruefe Status:" "WARN"
    Invoke-Expression "$COMPOSE_CMD" -f $COMPOSE_FILE ps
    Write-Log "Pruefe Logs (letzte 50 Zeilen):" "INFO"
    Invoke-Expression "$COMPOSE_CMD" -f $COMPOSE_FILE logs --tail=50
}

Write-Log "=== Auto-Update abgeschlossen ===" "INFO"
