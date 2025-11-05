# PowerShell Script für schnellen Start der lokalen Dev-Umgebung
# Startet podman-compose.dev.yml direkt ohne GitHub/GitHub Actions

$ErrorActionPreference = "Stop"

$COMPOSE_FILE = "podman-compose.dev.yml"
$ENV_FILE = "podman-compose.dev.env"
$COMPOSE_DIR = if ($env:PVS_LOCAL_PATH) { $env:PVS_LOCAL_PATH } else { Split-Path -Parent $PSScriptRoot | Split-Path -Parent }

Set-Location $COMPOSE_DIR
Write-Host "🚀 Starte PVS Dev-Umgebung..." -ForegroundColor Cyan
Write-Host "Verzeichnis: $COMPOSE_DIR" -ForegroundColor Gray

# Prüfe ob Podman verfügbar ist
try {
    $null = podman --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Podman nicht verfügbar" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Podman verfügbar" -ForegroundColor Green
    
    # Warnung wenn docker-compose.exe im PATH ist (Windows)
    $dockerCompose = Get-Command docker-compose.exe -ErrorAction SilentlyContinue
    if ($dockerCompose) {
        Write-Host "⚠️  WARNUNG: docker-compose.exe gefunden im PATH" -ForegroundColor Yellow
        Write-Host "   Podman könnte docker-compose.exe verwenden statt native compose" -ForegroundColor Yellow
        Write-Host "   Lösung: Deinstalliere docker-compose.exe oder verwende podman-compose (Python-Tool)" -ForegroundColor Gray
    }
    
    # Determine compose command - prefer podman-compose (Python) auf Windows um docker-compose.exe zu vermeiden
    $COMPOSE_CMD = "podman-compose"
    try {
        $null = podman-compose --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ podman-compose (Python) gefunden" -ForegroundColor Green
        } else {
            # Fallback zu podman compose (native)
            $null = podman compose version 2>&1
            if ($LASTEXITCODE -eq 0) {
                $COMPOSE_CMD = "podman compose"
                Write-Host "✓ podman compose (native) gefunden" -ForegroundColor Green
            } else {
                Write-Host "❌ Weder podman-compose noch podman compose verfügbar" -ForegroundColor Red
                Write-Host "   Installiere: pip install podman-compose" -ForegroundColor Gray
                exit 1
            }
        }
    } catch {
        # Fallback zu podman compose (native)
        try {
            $null = podman compose version 2>&1
            if ($LASTEXITCODE -eq 0) {
                $COMPOSE_CMD = "podman compose"
                Write-Host "✓ podman compose (native) gefunden" -ForegroundColor Green
            } else {
                Write-Host "❌ Weder podman-compose noch podman compose verfügbar" -ForegroundColor Red
                exit 1
            }
        } catch {
            Write-Host "❌ Podman compose nicht verfügbar" -ForegroundColor Red
            exit 1
        }
    }
} catch {
    Write-Host "❌ Podman nicht gefunden. Bitte Podman installieren." -ForegroundColor Red
    exit 1
}

# Prüfe ob podman-compose.dev.env existiert
$envFilePath = Join-Path $COMPOSE_DIR $ENV_FILE
if (-not (Test-Path $envFilePath)) {
    Write-Host "⚠️  podman-compose.dev.env nicht gefunden - verwende Defaults" -ForegroundColor Yellow
    Write-Host "💡 Tipp: Kopiere podman-compose.dev.env.example zu podman-compose.dev.env für eigene Konfiguration" -ForegroundColor Gray
    
    # Erstelle .env File mit Defaults falls gewünscht
    $createEnv = Read-Host "Soll podman-compose.dev.env mit Defaults erstellt werden? (j/n)"
    if ($createEnv -eq "j" -or $createEnv -eq "J" -or $createEnv -eq "y" -or $createEnv -eq "Y") {
        Copy-Item "$($COMPOSE_DIR)\podman-compose.dev.env.example" $envFilePath
        Write-Host "✓ podman-compose.dev.env erstellt" -ForegroundColor Green
    }
}

# Baue lokale Images falls nötig
Write-Host "📦 Baue lokale Images (falls nötig)..." -ForegroundColor Cyan
try {
    if (Test-Path $envFilePath) {
        Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE --env-file $ENV_FILE build --pull" 2>&1 | Out-Null
    } else {
        Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE build --pull" 2>&1 | Out-Null
    }
    Write-Host "✓ Images bereit" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Build-Warnung (möglicherweise Images bereits vorhanden)" -ForegroundColor Yellow
}

# Starte Container
Write-Host "🚀 Starte Container..." -ForegroundColor Cyan
try {
    if (Test-Path $envFilePath) {
        Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE --env-file $ENV_FILE up -d"
    } else {
        Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE up -d"
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Container gestartet" -ForegroundColor Green
        Write-Host ""
        Write-Host "📊 Status:" -ForegroundColor Cyan
        if (Test-Path $envFilePath) {
            Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE --env-file $ENV_FILE ps"
        } else {
            Invoke-Expression "$COMPOSE_CMD -f $COMPOSE_FILE ps"
        }
        Write-Host ""
        Write-Host "🌐 App erreichbar unter: http://localhost:8130" -ForegroundColor Green
        Write-Host "🔍 Logs anzeigen mit: $COMPOSE_CMD -f $COMPOSE_FILE logs -f" -ForegroundColor Gray
        Write-Host "🛑 Stoppen mit: $COMPOSE_CMD -f $COMPOSE_FILE down" -ForegroundColor Gray
    } else {
        Write-Host "❌ Fehler beim Starten der Container" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Fehler: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "🔍 Prüfe Logs mit: $COMPOSE_CMD -f $COMPOSE_FILE logs" -ForegroundColor Yellow
    exit 1
}

