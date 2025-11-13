# PowerShell Script zum separaten Bauen der Dev-Images
# Umgeht asyncio CancelledError bei podman-compose durch sequentielles Bauen

$ErrorActionPreference = "Stop"

$COMPOSE_FILE = "podman-compose.dev.yml"
$ENV_FILE = "podman-compose.dev.env"
$COMPOSE_DIR = if ($env:PVS_LOCAL_PATH) { $env:PVS_LOCAL_PATH } else { Split-Path -Parent $PSScriptRoot | Split-Path -Parent }

Set-Location $COMPOSE_DIR
Write-Host "🔨 Baue PVS Dev-Images..." -ForegroundColor Cyan
Write-Host "Verzeichnis: $COMPOSE_DIR" -ForegroundColor Gray

# Determine compose command
$COMPOSE_CMD = "podman-compose"
try {
    $null = podman-compose --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        $null = podman compose version 2>&1
        if ($LASTEXITCODE -eq 0) {
            $COMPOSE_CMD = "podman compose"
        } else {
            Write-Host "❌ Weder podman-compose noch podman compose verfügbar" -ForegroundColor Red
            exit 1
        }
    }
} catch {
    Write-Host "❌ Podman compose nicht verfügbar" -ForegroundColor Red
    exit 1
}

$envFilePath = Join-Path $COMPOSE_DIR $ENV_FILE
$envFileArg = if (Test-Path $envFilePath) { "--env-file $ENV_FILE" } else { "" }

# Baue Services einzeln nacheinander, um asyncio CancelledError zu vermeiden
$services = @("whisper", "pvs-app-dev")

foreach ($service in $services) {
    Write-Host ""
    Write-Host "📦 Baue Service: $service" -ForegroundColor Cyan
    try {
        if ($envFileArg) {
            Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE $envFileArg build $service"
        } else {
            Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE build $service"
        }
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ $service erfolgreich gebaut" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Build von $service mit Exit-Code $LASTEXITCODE" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "❌ Fehler beim Bauen von $service : $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "✅ Alle Images erfolgreich gebaut" -ForegroundColor Green

