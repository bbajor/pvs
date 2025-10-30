# PowerShell Script für schnellen Start der lokalen Dev-Umgebung
# Startet docker-compose.dev.yml direkt ohne GitHub/GitHub Actions

$ErrorActionPreference = "Stop"

$COMPOSE_FILE = "docker-compose.dev.yml"
$ENV_FILE = "docker-compose.dev.env"
$COMPOSE_DIR = if ($env:PVS_LOCAL_PATH) { $env:PVS_LOCAL_PATH } else { Split-Path -Parent $PSScriptRoot | Split-Path -Parent }

Set-Location $COMPOSE_DIR
Write-Host "🚀 Starte PVS Dev-Umgebung..." -ForegroundColor Cyan
Write-Host "Verzeichnis: $COMPOSE_DIR" -ForegroundColor Gray

# Prüfe ob Docker verfügbar ist
try {
    $null = docker --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Docker nicht verfügbar" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Docker verfügbar" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker nicht gefunden. Bitte Docker Desktop installieren." -ForegroundColor Red
    exit 1
}

# Prüfe ob docker-compose.dev.env existiert
$envFilePath = Join-Path $COMPOSE_DIR $ENV_FILE
if (-not (Test-Path $envFilePath)) {
    Write-Host "⚠️  docker-compose.dev.env nicht gefunden - verwende Defaults" -ForegroundColor Yellow
    Write-Host "💡 Tipp: Kopiere docker-compose.dev.env.example zu docker-compose.dev.env für eigene Konfiguration" -ForegroundColor Gray
    
    # Erstelle .env File mit Defaults falls gewünscht
    $createEnv = Read-Host "Soll docker-compose.dev.env mit Defaults erstellt werden? (j/n)"
    if ($createEnv -eq "j" -or $createEnv -eq "J" -or $createEnv -eq "y" -or $createEnv -eq "Y") {
        Copy-Item "$($COMPOSE_DIR)\docker-compose.dev.env.example" $envFilePath
        Write-Host "✓ docker-compose.dev.env erstellt" -ForegroundColor Green
    }
}

# Baue lokale Images falls nötig
Write-Host "📦 Baue lokale Images (falls nötig)..." -ForegroundColor Cyan
try {
    if (Test-Path $envFilePath) {
        docker compose -f $COMPOSE_FILE --env-file $ENV_FILE build --pull 2>&1 | Out-Null
    } else {
        docker compose -f $COMPOSE_FILE build --pull 2>&1 | Out-Null
    }
    Write-Host "✓ Images bereit" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Build-Warnung (möglicherweise Images bereits vorhanden)" -ForegroundColor Yellow
}

# Starte Container
Write-Host "🚀 Starte Container..." -ForegroundColor Cyan
try {
    if (Test-Path $envFilePath) {
        docker compose -f $COMPOSE_FILE --env-file $ENV_FILE up -d
    } else {
        docker compose -f $COMPOSE_FILE up -d
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Container gestartet" -ForegroundColor Green
        Write-Host ""
        Write-Host "📊 Status:" -ForegroundColor Cyan
        if (Test-Path $envFilePath) {
            docker compose -f $COMPOSE_FILE --env-file $ENV_FILE ps
        } else {
            docker compose -f $COMPOSE_FILE ps
        }
        Write-Host ""
        Write-Host "🌐 App erreichbar unter: http://localhost:8130" -ForegroundColor Green
        Write-Host "🔍 Logs anzeigen mit: docker compose -f $COMPOSE_FILE logs -f" -ForegroundColor Gray
        Write-Host "🛑 Stoppen mit: docker compose -f $COMPOSE_FILE down" -ForegroundColor Gray
    } else {
        Write-Host "❌ Fehler beim Starten der Container" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Fehler: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "🔍 Prüfe Logs mit: docker compose -f $COMPOSE_FILE logs" -ForegroundColor Yellow
    exit 1
}

