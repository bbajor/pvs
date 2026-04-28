# PVS OnPremise Deinstaller für Windows
# Entfernt PVS OnPremise Installation mit interaktiven Optionen

param(
    [string]$InstallDir = "C:\Program Files\PVS"
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

# Tracking-Variablen
$script:RemovedServices = @()
$script:RemovedContainers = @()
$script:RemovedVolumes = @()
$script:RemovedDirs = @()
$script:KeptData = @()

Write-ColorOutput Blue "=== PVS OnPremise Deinstaller ==="
Write-Output ""
Write-ColorOutput Yellow "Dieses Skript entfernt die PVS OnPremise Installation."
Write-ColorOutput Yellow "WICHTIG: Datenbank-Daten können optional erhalten bleiben."
Write-Output ""

# Prüfe Administrator-Rechte
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-ColorOutput Red "Fehler: Dieses Skript muss als Administrator ausgeführt werden"
    exit 1
}

# Prüfe ob Installation existiert
if (-not (Test-Path $InstallDir)) {
    Write-ColorOutput Yellow "Installations-Verzeichnis $InstallDir nicht gefunden."
    Write-ColorOutput Yellow "PVS OnPremise scheint nicht installiert zu sein."
    exit 0
}

Write-ColorOutput Green "Installation gefunden in: $InstallDir"
Write-Output ""

# Funktion: Ja/Nein-Abfrage
function Ask-YesNo {
    param(
        [string]$Prompt,
        [string]$Default = "N"
    )
    
    $choices = @()
    if ($Default -eq "Y") {
        $choices = @("&Ja", "&Nein")
        $defaultChoice = 0
    } else {
        $choices = @("&Ja", "&Nein")
        $defaultChoice = 1
    }
    
    $title = $Prompt
    $message = $Prompt
    
    $result = $Host.UI.PromptForChoice($title, $message, $choices, $defaultChoice)
    return ($result -eq 0)
}

# 1. Scheduled Task entfernen
Write-ColorOutput Blue "=== 1. Scheduled Task ==="
$taskName = "PVS-OnPremise-Start"

$existingTask = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($existingTask) {
    Write-ColorOutput Yellow "Scheduled Task '$taskName' gefunden."
    if (Ask-YesNo "Scheduled Task entfernen?" "Y") {
        Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
        $script:RemovedServices += "Scheduled Task: $taskName"
        Write-ColorOutput Green "✓ Scheduled Task entfernt"
    } else {
        Write-ColorOutput Yellow "⚠ Scheduled Task wird nicht entfernt"
    }
} else {
    Write-ColorOutput Green "✓ Kein Scheduled Task gefunden"
}
Write-Output ""

# 2. Container stoppen und entfernen
Write-ColorOutput Blue "=== 2. Container ==="
$composeFile = Join-Path $InstallDir "podman-compose.onpremise.yml"

if ((Get-Command podman-compose -ErrorAction SilentlyContinue) -and (Test-Path $composeFile)) {
    Push-Location $InstallDir
    
    try {
        # Prüfe laufende Container
        $runningContainers = podman ps -a --filter "name=pvs-onpremise" --format "{{.Names}}" 2>$null
        
        if ($runningContainers) {
            Write-ColorOutput Yellow "Gefundene Container:"
            $runningContainers | ForEach-Object {
                Write-Output "  - $_"
            }
            Write-Output ""
            
            if (Ask-YesNo "Container stoppen und entfernen?" "Y") {
                Write-Output "Stoppe Container..."
                podman-compose -f $composeFile down 2>$null
                
                # Entferne einzelne Container falls noch vorhanden
                $runningContainers | ForEach-Object {
                    $container = $_.Trim()
                    if ($container) {
                        podman stop $container 2>$null
                        podman rm $container 2>$null
                        $script:RemovedContainers += $container
                    }
                }
                
                Write-ColorOutput Green "✓ Container entfernt"
            } else {
                Write-ColorOutput Yellow "⚠ Container werden nicht entfernt"
            }
        } else {
            Write-ColorOutput Green "✓ Keine Container gefunden"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-ColorOutput Yellow "podman-compose nicht verfügbar oder Konfiguration nicht gefunden"
}
Write-Output ""

# 3. Volumes (Datenbank-Daten!)
Write-ColorOutput Blue "=== 3. Daten-Volumes ==="
Write-ColorOutput Red "⚠️  WICHTIG: Volumes enthalten die Datenbank-Daten (inkl. IVOM-Behandlungsdaten)!"
Write-Output ""

if (Get-Command podman -ErrorAction SilentlyContinue) {
    $volumes = podman volume ls --filter "name=pvs-onpremise" --format "{{.Name}}" 2>$null
    
    if ($volumes) {
        Write-ColorOutput Yellow "Gefundene Volumes:"
        $volumes | ForEach-Object {
            Write-Output "  - $_"
        }
        Write-Output ""
        
        # Spezielle Warnung für Datenbank-Volumes
        $dbVolumes = $volumes | Where-Object { $_ -match "(postgres|kbv)" }
        if ($dbVolumes) {
            Write-ColorOutput Red "⚠️  KRITISCH: Die folgenden Volumes enthalten Datenbank-Daten:"
            $dbVolumes | ForEach-Object {
                Write-ColorOutput Red "  - $_"
            }
            Write-Output ""
            Write-ColorOutput Yellow "Diese enthalten:"
            Write-Output "  - IVOM-Behandlungsdaten"
            Write-Output "  - Patientendaten"
            Write-Output "  - Alle anderen Anwendungsdaten"
            Write-Output ""
            
            if (Ask-YesNo "Datenbank-Volumes LÖSCHEN? (Daten gehen VERLOREN!)" "N") {
                $dbVolumes | ForEach-Object {
                    $volume = $_.Trim()
                    if ($volume) {
                        $result = podman volume rm $volume 2>&1
                        if ($LASTEXITCODE -eq 0) {
                            $script:RemovedVolumes += $volume
                            Write-ColorOutput Green "✓ Volume $volume gelöscht"
                        }
                    }
                }
            } else {
                Write-ColorOutput Green "✓ Datenbank-Volumes werden BEHALTEN"
                $dbVolumes | ForEach-Object {
                    $script:KeptData += $_
                }
            }
            Write-Output ""
        }
        
        # Andere Volumes
        $otherVolumes = $volumes | Where-Object { $_ -notmatch "(postgres|kbv)" }
        if ($otherVolumes) {
            Write-ColorOutput Yellow "Weitere Volumes:"
            $otherVolumes | ForEach-Object {
                Write-Output "  - $_"
            }
            Write-Output ""
            
            if (Ask-YesNo "Diese Volumes entfernen?" "Y") {
                $otherVolumes | ForEach-Object {
                    $volume = $_.Trim()
                    if ($volume) {
                        $result = podman volume rm $volume 2>&1
                        if ($LASTEXITCODE -eq 0) {
                            $script:RemovedVolumes += $volume
                            Write-ColorOutput Green "✓ Volume $volume gelöscht"
                        }
                    }
                }
            } else {
                $otherVolumes | ForEach-Object {
                    $script:KeptData += $_
                }
            }
        }
    } else {
        Write-ColorOutput Green "✓ Keine Volumes gefunden"
    }
} else {
    Write-ColorOutput Yellow "Podman nicht verfügbar"
}
Write-Output ""

# 4. Installations-Verzeichnis
Write-ColorOutput Blue "=== 4. Installations-Verzeichnis ==="
Write-ColorOutput Yellow "Installations-Verzeichnis: $InstallDir"
Write-Output ""

if (Test-Path $InstallDir) {
    # Zeige Größe des Verzeichnisses
    $size = (Get-ChildItem $InstallDir -Recurse -ErrorAction SilentlyContinue | 
             Measure-Object -Property Length -Sum -ErrorAction SilentlyContinue).Sum
    $sizeMB = [math]::Round($size / 1MB, 2)
    Write-ColorOutput Yellow "Größe: $sizeMB MB"
    Write-Output ""
    
    # Zeige wichtige Dateien/Verzeichnisse
    Write-ColorOutput Yellow "Enthält:"
    if (Test-Path (Join-Path $InstallDir ".env")) {
        Write-Output "  - .env (Konfiguration)"
    }
    if (Test-Path (Join-Path $InstallDir "backups")) {
        Write-Output "  - backups\ (Backup-Dateien)"
    }
    if (Test-Path (Join-Path $InstallDir "podman-compose.onpremise.yml")) {
        Write-Output "  - podman-compose.onpremise.yml"
    }
    Write-Output ""
    
    if (Ask-YesNo "Installations-Verzeichnis komplett löschen?" "Y") {
        # Frage nach Backups
        $backupDir = Join-Path $InstallDir "backups"
        if ((Test-Path $backupDir) -and ((Get-ChildItem $backupDir -ErrorAction SilentlyContinue | Measure-Object).Count -gt 0)) {
            Write-Output ""
            Write-ColorOutput Yellow "Backup-Verzeichnis enthält Dateien:"
            Get-ChildItem $backupDir -ErrorAction SilentlyContinue | ForEach-Object {
                Write-Output "  - $($_.Name) ($([math]::Round($_.Length / 1KB, 2)) KB)"
            }
            Write-Output ""
            $keepBackups = -not (Ask-YesNo "Backup-Verzeichnis auch löschen?" "N")
        } else {
            $keepBackups = $false
        }
        
        # Lösche Verzeichnis
        if ($keepBackups -and (Test-Path $backupDir)) {
            # Verschiebe Backups temporär
            $tempBackup = Join-Path $env:TEMP "pvs-backups-$(Get-Date -Format 'yyyyMMddHHmmss')"
            Move-Item $backupDir $tempBackup -ErrorAction SilentlyContinue
            Remove-Item $InstallDir -Recurse -Force -ErrorAction SilentlyContinue
            New-Item -ItemType Directory -Path (Split-Path $InstallDir) -Force | Out-Null
            Move-Item $tempBackup $backupDir -ErrorAction SilentlyContinue
            Write-ColorOutput Green "✓ Installations-Verzeichnis gelöscht (Backups behalten)"
            $script:KeptData += $backupDir
        } else {
            Remove-Item $InstallDir -Recurse -Force -ErrorAction SilentlyContinue
            $script:RemovedDirs += $InstallDir
            Write-ColorOutput Green "✓ Installations-Verzeichnis gelöscht"
        }
    } else {
        Write-ColorOutput Yellow "⚠ Installations-Verzeichnis wird BEHALTEN"
        $script:KeptData += $InstallDir
    }
} else {
    Write-ColorOutput Green "✓ Installations-Verzeichnis existiert nicht"
}
Write-Output ""

# Zusammenfassung
Write-Output ""
Write-ColorOutput Blue "========================================"
Write-ColorOutput Blue "=== Deinstallations-Zusammenfassung ==="
Write-ColorOutput Blue "========================================"
Write-Output ""

Write-ColorOutput Green "✓ Entfernt:"
if ($script:RemovedServices.Count -gt 0) {
    Write-ColorOutput Green "  Services:"
    $script:RemovedServices | ForEach-Object {
        Write-Output "    - $_"
    }
}

if ($script:RemovedContainers.Count -gt 0) {
    Write-ColorOutput Green "  Container:"
    $script:RemovedContainers | ForEach-Object {
        Write-Output "    - $_"
    }
} elseif ($runningContainers) {
    Write-ColorOutput Yellow "  Container: (nicht entfernt)"
}

if ($script:RemovedVolumes.Count -gt 0) {
    Write-ColorOutput Green "  Volumes:"
    $script:RemovedVolumes | ForEach-Object {
        Write-Output "    - $_"
    }
}

if ($script:RemovedDirs.Count -gt 0) {
    Write-ColorOutput Green "  Verzeichnisse:"
    $script:RemovedDirs | ForEach-Object {
        Write-Output "    - $_"
    }
}

if ($script:RemovedServices.Count -eq 0 -and $script:RemovedContainers.Count -eq 0 -and 
    $script:RemovedVolumes.Count -eq 0 -and $script:RemovedDirs.Count -eq 0) {
    Write-ColorOutput Yellow "  (nichts entfernt)"
}

Write-Output ""

if ($script:KeptData.Count -gt 0) {
    Write-ColorOutput Yellow "⚠ Behalten (noch vorhanden):"
    $script:KeptData | ForEach-Object {
        Write-Output "    - $_"
    }
    Write-Output ""
    Write-ColorOutput Yellow "Hinweis: Diese Daten können manuell entfernt werden:"
    $script:KeptData | ForEach-Object {
        $item = $_
        if ($item -match "(postgres|kbv)") {
            Write-ColorOutput Yellow "  Volume: podman volume rm $item"
        } elseif (Test-Path $item) {
            Write-ColorOutput Yellow "  Verzeichnis: Remove-Item -Recurse -Force '$item'"
        }
    }
} else {
    Write-ColorOutput Green "✓ Keine Daten behalten"
}

Write-Output ""
Write-ColorOutput Blue "========================================"
Write-Output ""

# Finale Prüfung
$remainingContainers = podman ps -a --filter "name=pvs-onpremise" --format "{{.Names}}" 2>$null
if ($remainingContainers) {
    Write-ColorOutput Yellow "⚠ Es sind noch Container vorhanden."
    Write-ColorOutput Yellow "  Prüfe mit: podman ps -a --filter 'name=pvs-onpremise'"
}

$remainingVolumes = podman volume ls --filter "name=pvs-onpremise" --format "{{.Name}}" 2>$null
if ($remainingVolumes) {
    Write-ColorOutput Yellow "⚠ Es sind noch Volumes vorhanden."
    Write-ColorOutput Yellow "  Prüfe mit: podman volume ls --filter 'name=pvs-onpremise'"
}

if (Test-Path $InstallDir) {
    Write-ColorOutput Yellow "⚠ Installations-Verzeichnis existiert noch: $InstallDir"
}

Write-Output ""
Write-ColorOutput Green "Deinstallation abgeschlossen!"

