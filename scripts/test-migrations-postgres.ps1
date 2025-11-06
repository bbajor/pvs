# Test Flyway migrations locally with PostgreSQL database
# This script tests the SQL migration scripts using a PostgreSQL container
# without needing to rebuild the full application container

param(
    [switch]$Clean = $false,
    [switch]$KeepRunning = $false
)

$ErrorActionPreference = "Continue"

Write-Host "Testing Flyway migrations with PostgreSQL..." -ForegroundColor Cyan
Write-Host ""

# Check if podman-compose exists
$podmanCompose = Get-Command podman-compose -ErrorAction SilentlyContinue
if (-not $podmanCompose) {
    Write-Host "Error: podman-compose not found. Please install podman-compose first." -ForegroundColor Red
    exit 1
}

# Check if gradlew exists
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "Error: gradlew.bat not found. Please run this script from the project root." -ForegroundColor Red
    exit 1
}

$composeFile = "podman-compose.test-migrations.yml"

# Start PostgreSQL container for testing
Write-Host "Starting PostgreSQL container for migration tests..." -ForegroundColor Yellow
if ($Clean) {
    Write-Host "Cleaning up existing containers and volumes..." -ForegroundColor Gray
    try {
        $null = podman compose -f $composeFile down -v 2>&1 | Out-String
    } catch {
        # Ignore errors if containers don't exist
    }
}

$output = podman compose -f $composeFile up -d 2>&1 | Out-String
$exitCode = $LASTEXITCODE

# Check if container was actually created
$containerExists = podman ps -a --filter name=pvs-postgres-test-migrations --format "{{.Names}}" 2>&1
if ($containerExists -notmatch "pvs-postgres-test-migrations") {
    Write-Host "Error: Failed to start PostgreSQL container" -ForegroundColor Red
    Write-Host $output
    exit 1
}

# Wait for PostgreSQL to be ready
Write-Host "Waiting for PostgreSQL to be ready..." -ForegroundColor Yellow
$maxRetries = 30
$retryCount = 0
$ready = $false

while ($retryCount -lt $maxRetries -and -not $ready) {
    Start-Sleep -Seconds 1
    $result = podman exec pvs-postgres-test-migrations pg_isready -U pvs_user -d pvs_test_migrations 2>&1
    if ($LASTEXITCODE -eq 0) {
        $ready = $true
    } else {
        $retryCount++
    }
}

if (-not $ready) {
    Write-Host "Error: PostgreSQL container did not become ready in time" -ForegroundColor Red
    podman compose -f $composeFile down
    exit 1
}

Write-Host "PostgreSQL is ready!" -ForegroundColor Green
Write-Host ""

# Test migrations with PostgreSQL
Write-Host "Running Flyway migrations with PostgreSQL..." -ForegroundColor Yellow
$dbUrl = "jdbc:postgresql://localhost:5435/pvs_test_migrations"
$dbUser = "pvs_user"
$dbPassword = "test_password"

& .\gradlew.bat testMigrations "-PtestDbUrl=$dbUrl" "-PtestDbUser=$dbUser" "-PtestDbPassword=$dbPassword"

$migrationExitCode = $LASTEXITCODE

if ($migrationExitCode -eq 0) {
    Write-Host "`nMigrations tested successfully!" -ForegroundColor Green
    
    # Show migration info
    Write-Host "`nMigration status:" -ForegroundColor Cyan
    podman exec pvs-postgres-test-migrations psql -U pvs_user -d pvs_test_migrations -c "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank;"
} else {
    Write-Host "`nMigration test failed!" -ForegroundColor Red
}

# Cleanup
if (-not $KeepRunning) {
    Write-Host "`nStopping PostgreSQL container..." -ForegroundColor Yellow
    podman compose -f $composeFile down
    if ($Clean) {
        Write-Host "Removing volumes..." -ForegroundColor Gray
        podman compose -f $composeFile down -v
    }
} else {
    Write-Host "`nPostgreSQL container is still running for manual inspection." -ForegroundColor Yellow
    Write-Host "Connection: jdbc:postgresql://localhost:5435/pvs_test_migrations" -ForegroundColor Gray
    Write-Host "User: pvs_user, Password: test_password" -ForegroundColor Gray
    Write-Host "Stop with: podman compose -f $composeFile down" -ForegroundColor Gray
}

exit $migrationExitCode

