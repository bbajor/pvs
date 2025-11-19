# Migrate WSL distributions to D: drive
# Exports existing WSL distributions and imports them to D:\wsl
# Optionally removes the old distribution after successful import

$ErrorActionPreference = "Stop"

Write-Host "WSL Distribution Migration to D: Drive" -ForegroundColor Cyan
Write-Host ""

# Check if WSL is available
$wslCmd = Get-Command wsl -ErrorAction SilentlyContinue
if (-not $wslCmd) {
    Write-Host "ERROR: WSL is not installed or not in PATH" -ForegroundColor Red
    Write-Host "   Please install WSL2: wsl --install" -ForegroundColor Yellow
    exit 1
}

# Check WSL status
Write-Host "Checking WSL installation..." -ForegroundColor Cyan
$wslStatus = wsl --status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "   WARNING: Could not retrieve WSL status" -ForegroundColor Yellow
} else {
    Write-Host "   OK: WSL available" -ForegroundColor Green
    $wslStatusLines = $wslStatus -join "`n   "
    Write-Host "   $wslStatusLines" -ForegroundColor Gray
}

# Check if D: drive exists
Write-Host ""
Write-Host "Checking D: drive..." -ForegroundColor Cyan
if (-not (Test-Path "D:\")) {
    Write-Host "ERROR: D: drive does not exist" -ForegroundColor Red
    exit 1
}
Write-Host "   OK: D: drive available" -ForegroundColor Green

# Check available space on D:
$drive = Get-PSDrive D -ErrorAction SilentlyContinue
if ($drive) {
    $freeSpaceGB = [math]::Round($drive.Free / 1GB, 2)
    Write-Host "   Available space on D:: $freeSpaceGB GB" -ForegroundColor Gray
}

# Define target path for WSL distributions
$wslTargetPath = "D:\wsl"
Write-Host ""
Write-Host "Target path for WSL distributions: $wslTargetPath" -ForegroundColor Cyan

# Create target folder if it doesn't exist
if (-not (Test-Path $wslTargetPath)) {
    Write-Host "   Creating target folder..." -ForegroundColor Gray
    try {
        New-Item -Path $wslTargetPath -ItemType Directory -Force | Out-Null
        Write-Host "     OK: Folder created" -ForegroundColor Green
    } catch {
        Write-Host "     ERROR: Failed to create folder: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# List existing WSL distributions
Write-Host ""
Write-Host "Listing existing WSL distributions..." -ForegroundColor Cyan
$distributions = wsl --list --verbose 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "   ERROR: Failed to retrieve distributions" -ForegroundColor Red
    Write-Host "   $distributions" -ForegroundColor Yellow
    exit 1
}

# Parse distributions (Format: NAME STATE VERSION)
$distList = @()
$lines = $distributions | Where-Object { $_ -match "^\s+\S+" }
foreach ($line in $lines) {
    $parts = $line -split "\s+", 4
    if ($parts.Count -ge 2) {
        $distList += @{
            Name = $parts[1]
            State = if ($parts.Count -ge 3) { $parts[2] } else { "Unknown" }
            Version = if ($parts.Count -ge 4) { $parts[3] } else { "Unknown" }
        }
    }
}

if ($distList.Count -eq 0) {
    Write-Host "   WARNING: No WSL distributions found" -ForegroundColor Yellow
    Write-Host "   Install a distribution: wsl --install -d Ubuntu" -ForegroundColor Gray
    exit 0
}

Write-Host "   Found distributions:" -ForegroundColor Yellow
for ($i = 0; $i -lt $distList.Count; $i++) {
    $dist = $distList[$i]
    $distMsg = "     $($i + 1). $($dist.Name) (Status: $($dist.State), Version: $($dist.Version))"
    Write-Host $distMsg -ForegroundColor Gray
}

# User selects distribution
Write-Host ""
$selectedIndex = -1
while ($selectedIndex -lt 0 -or $selectedIndex -ge $distList.Count) {
    $input = Read-Host "Which distribution should be migrated? (1-$($distList.Count))"
    if ([int]::TryParse($input, [ref]$selectedIndex)) {
        $selectedIndex = $selectedIndex - 1
        if ($selectedIndex -lt 0 -or $selectedIndex -ge $distList.Count) {
            Write-Host "   WARNING: Invalid selection. Please choose a number between 1 and $($distList.Count)" -ForegroundColor Yellow
            $selectedIndex = -1
        }
    } else {
        Write-Host "   WARNING: Please enter a number" -ForegroundColor Yellow
    }
}

$selectedDist = $distList[$selectedIndex]
Write-Host ""
Write-Host "Selected distribution: $($selectedDist.Name)" -ForegroundColor Cyan

# Check if distribution is running
if ($selectedDist.State -eq 'Running') {
    Write-Host "   WARNING: Distribution is running. Stopping it before export..." -ForegroundColor Yellow
    Write-Host "   Stopping distribution..." -ForegroundColor Gray
    wsl --terminate $selectedDist.Name 2>&1 | Out-Null
    Start-Sleep -Seconds 2
    Write-Host "     OK: Distribution stopped" -ForegroundColor Green
}

# Define export and import paths
$exportFile = Join-Path $env:TEMP "$($selectedDist.Name)-export-$(Get-Date -Format 'yyyyMMdd-HHmmss').tar"
$importPath = Join-Path $wslTargetPath $selectedDist.Name

Write-Host ""
Write-Host "Exporting distribution..." -ForegroundColor Cyan
Write-Host "   Export file: $exportFile" -ForegroundColor Gray

# Export distribution
wsl --export $selectedDist.Name $exportFile 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "   ERROR: Failed to export distribution" -ForegroundColor Red
    Write-Host "   Make sure the distribution is stopped" -ForegroundColor Yellow
    exit 1
}

# Check export file
if (-not (Test-Path $exportFile)) {
    Write-Host "   ERROR: Export file was not created" -ForegroundColor Red
    exit 1
}

$exportSizeGB = [math]::Round((Get-Item $exportFile).Length / 1GB, 2)
$exportSizeText = "$exportSizeGB GB"
$exportMsg = "     OK: Export completed ($exportSizeText)"
Write-Host $exportMsg -ForegroundColor Green

# Check if target path already exists
if (Test-Path $importPath) {
    Write-Host ""
    Write-Host "WARNING: Target path already exists: $importPath" -ForegroundColor Yellow
    $response = Read-Host 'Should the existing folder be deleted? (y/N)'
    if ($response -eq 'y' -or $response -eq 'Y') {
        Write-Host "   Deleting existing folder..." -ForegroundColor Gray
        try {
            Remove-Item -Path $importPath -Recurse -Force -ErrorAction Stop
            Write-Host "     OK: Deleted" -ForegroundColor Green
        } catch {
            Write-Host "     ERROR: Failed to delete: $($_.Exception.Message)" -ForegroundColor Red
            Write-Host "   Please delete the folder manually: $importPath" -ForegroundColor Yellow
            exit 1
        }
    } else {
        Write-Host "   Cancelled" -ForegroundColor Yellow
        exit 0
    }
}

Write-Host ""
Write-Host "Importing distribution to D:..." -ForegroundColor Cyan
Write-Host "   Target path: $importPath" -ForegroundColor Gray

# Import distribution
wsl --import $selectedDist.Name $importPath $exportFile --version 2 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "   ERROR: Failed to import distribution" -ForegroundColor Red
    Write-Host "   Check if enough space is available on D:" -ForegroundColor Yellow
    exit 1
}

Write-Host "     OK: Import completed" -ForegroundColor Green

# Check if new distribution is running
Write-Host ""
Write-Host "Testing new distribution..." -ForegroundColor Cyan
Start-Sleep -Seconds 2
$newDistStatus = wsl --list --verbose | Select-String $selectedDist.Name
if ($newDistStatus) {
    Write-Host "   OK: Distribution successfully imported" -ForegroundColor Green
    Write-Host "   $newDistStatus" -ForegroundColor Gray
} else {
    Write-Host "   WARNING: Could not retrieve distribution status" -ForegroundColor Yellow
}

# Delete export file
Write-Host ""
Write-Host "Cleaning up temporary files..." -ForegroundColor Cyan
try {
    Remove-Item -Path $exportFile -Force -ErrorAction Stop
    Write-Host "   OK: Export file deleted" -ForegroundColor Green
} catch {
    Write-Host "   WARNING: Could not delete export file: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "   Please delete manually: $exportFile" -ForegroundColor Gray
}

# Ask if old distribution should be deleted
Write-Host ""
Write-Host "WARNING: Old distribution on C: is still present" -ForegroundColor Yellow
Write-Host '   The new distribution on D: is now active' -ForegroundColor Gray
$response = Read-Host 'Should the old distribution be deleted? (y/N)'
$shouldDelete = ($response -eq 'y') -or ($response -eq 'Y')
if ($shouldDelete) {
    Write-Host ""
    Write-Host "Deleting old distribution..." -ForegroundColor Cyan
    
    Write-Host "   Unregistering old distribution..." -ForegroundColor Gray
    wsl --unregister $selectedDist.Name 2>&1 | Out-Null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "     OK: Old distribution unregistered" -ForegroundColor Green
        Write-Host "   The VHDX file on C: can be deleted manually if desired" -ForegroundColor Gray
    } else {
        Write-Host "     WARNING: Could not unregister old distribution" -ForegroundColor Yellow
        Write-Host "   You can delete it manually: wsl --unregister $($selectedDist.Name)" -ForegroundColor Gray
    }
} else {
    Write-Host "   Old distribution will remain" -ForegroundColor Gray
    Write-Host "   You can delete it later: wsl --unregister $($selectedDist.Name)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Migration completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "   Distribution: $($selectedDist.Name)" -ForegroundColor Gray
Write-Host "   New location: $importPath" -ForegroundColor Gray
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
$testMsg = "   1. Test the distribution: wsl -d $($selectedDist.Name)"
Write-Host $testMsg -ForegroundColor Gray
Write-Host '   2. Check status: wsl --list --verbose' -ForegroundColor Gray
Write-Host '   3. If issues occur, restart WSL: wsl --shutdown' -ForegroundColor Gray
Write-Host ""
