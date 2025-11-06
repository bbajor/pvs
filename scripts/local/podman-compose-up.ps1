# Wrapper für podman-compose up, umgeht asyncio CancelledError
# Verwendung: .\scripts\local\podman-compose-up.ps1 [--build] [--no-build]

param(
    [switch]$Build,
    [switch]$NoBuild
)

$ErrorActionPreference = "Stop"

$COMPOSE_FILE = "podman-compose.dev.yml"
$ENV_FILE = "podman-compose.dev.env"
$COMPOSE_DIR = if ($env:PVS_LOCAL_PATH) { $env:PVS_LOCAL_PATH } else { Split-Path -Parent $PSScriptRoot | Split-Path -Parent }

Set-Location $COMPOSE_DIR

# Determine compose command
$COMPOSE_CMD = "podman-compose"
try {
    $null = podman-compose --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        $null = podman compose version 2>&1
        if ($LASTEXITCODE -eq 0) {
            $COMPOSE_CMD = "podman compose"
        }
    }
} catch {
    Write-Host "❌ Podman compose nicht verfügbar" -ForegroundColor Red
    exit 1
}

$envFilePath = Join-Path $COMPOSE_DIR $ENV_FILE
$envFileArg = if (Test-Path $envFilePath) { "--env-file $ENV_FILE" } else { "" }

# Wenn --build angegeben oder nicht --no-build, baue Images einzeln
# Standard: baue Images (umgeht asyncio-Problem)
if ($Build -or (-not $NoBuild)) {
    Write-Host "🔨 Baue Images nacheinander (umgeht asyncio-Problem)..." -ForegroundColor Cyan
    
    $services = @("whisper", "pvs-app-dev")
    foreach ($service in $services) {
        Write-Host "  → Baue $service..." -ForegroundColor Gray
        try {
            if ($envFileArg) {
                Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE $envFileArg build $service"
            } else {
                Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE build $service"
            }
            if ($LASTEXITCODE -ne 0) {
                Write-Host "⚠️  Build von $service fehlgeschlagen, fahre trotzdem fort..." -ForegroundColor Yellow
            }
        } catch {
            Write-Host "⚠️  Fehler beim Bauen von $service : $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
}

# Starte Container ohne Build (da bereits gebaut)
Write-Host "🚀 Starte Container..." -ForegroundColor Cyan
try {
    if ($envFileArg) {
        Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE $envFileArg up -d"
    } else {
        Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE up -d"
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Container gestartet" -ForegroundColor Green
    } else {
        Write-Host "❌ Fehler beim Starten der Container" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Fehler: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

