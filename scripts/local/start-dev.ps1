# PowerShell Script für schnellen Start der lokalen Dev-Umgebung
# Startet podman-compose.dev.yml direkt ohne GitHub/GitHub Actions

$COMPOSE_FILE = "podman-compose.dev.yml"
$ENV_FILE = "podman-compose.dev.env"
$COMPOSE_DIR = if ($env:PVS_LOCAL_PATH) { $env:PVS_LOCAL_PATH } else { Split-Path -Parent $PSScriptRoot | Split-Path -Parent }

Set-Location $COMPOSE_DIR
Write-Host "Starte PVS Dev-Umgebung..." -ForegroundColor Cyan

# Pruefe benoetigte Software
$missing = @()

# Pruefe Podman
$podmanCmd = Get-Command podman -ErrorAction SilentlyContinue
if (-not $podmanCmd) {
    $missing += "Podman"
} else {
    $null = podman --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        $missing += "Podman"
    }
}

# Pruefe podman-compose oder podman compose
$composeFound = $false
$COMPOSE_CMD = ""

$podmanComposeCmd = Get-Command podman-compose -ErrorAction SilentlyContinue
if ($podmanComposeCmd) {
    $null = podman-compose --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        $COMPOSE_CMD = "podman-compose"
        $composeFound = $true
    }
}

if (-not $composeFound) {
    $null = podman compose version 2>&1
    if ($LASTEXITCODE -eq 0) {
        $COMPOSE_CMD = "podman compose"
        $composeFound = $true
    } else {
        $missing += "podman-compose (pip install podman-compose) oder podman compose"
    }
}

# Ausgabe fehlender Software
if ($missing.Count -gt 0) {
    Write-Host "Fehlende Software:" -ForegroundColor Red
    foreach ($item in $missing) {
        Write-Host "   - $item" -ForegroundColor Red
    }
    exit 1
}

# Pruefe ob podman-compose.dev.env existiert
$envFilePath = Join-Path $COMPOSE_DIR $ENV_FILE
$envFileArg = if (Test-Path $envFilePath) { "--env-file $ENV_FILE" } else { "" }

# Baue Images sequenziell (umgeht parallele Ausgabe und asyncio-Problem)
Write-Host "Baue Images..." -ForegroundColor Cyan
$services = @("whisper", "pvs-app-dev")
foreach ($service in $services) {
    Write-Host "  -> Baue $service..." -ForegroundColor Gray
    $buildCmd = if ($envFileArg) { "$COMPOSE_CMD -f $COMPOSE_FILE $envFileArg build $service" } else { "$COMPOSE_CMD -f $COMPOSE_FILE build $service" }
    Invoke-Expression $buildCmd | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  Warnung: Build von $service fehlgeschlagen" -ForegroundColor Yellow
    }
}

# Starte Container ohne Build (da bereits gebaut)
Write-Host "Starte Container..." -ForegroundColor Cyan
$upCmd = if ($envFileArg) { "$COMPOSE_CMD -f $COMPOSE_FILE $envFileArg up -d --no-build" } else { "$COMPOSE_CMD -f $COMPOSE_FILE up -d --no-build" }
Invoke-Expression $upCmd

if ($LASTEXITCODE -eq 0) {
    Write-Host "Container gestartet" -ForegroundColor Green
    Write-Host ""
    $psCmd = if ($envFileArg) { "$COMPOSE_CMD -f $COMPOSE_FILE $envFileArg ps" } else { "$COMPOSE_CMD -f $COMPOSE_FILE ps" }
    Invoke-Expression $psCmd
    Write-Host ""
    Write-Host "App erreichbar unter: http://localhost:8130" -ForegroundColor Green
    Write-Host "Logs: $COMPOSE_CMD -f $COMPOSE_FILE logs -f" -ForegroundColor Gray
    Write-Host "Stoppen: $COMPOSE_CMD -f $COMPOSE_FILE down" -ForegroundColor Gray
} else {
    Write-Host "Fehler beim Starten der Container" -ForegroundColor Red
    exit 1
}

