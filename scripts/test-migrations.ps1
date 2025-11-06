# Test Flyway migrations locally with H2 database
# This script tests the SQL migration scripts without needing to rebuild containers

param(
    [string]$DbUrl = "jdbc:h2:mem:testdb",
    [string]$DbUser = "sa",
    [string]$DbPassword = ""
)

Write-Host "Testing Flyway migrations locally..." -ForegroundColor Cyan
Write-Host "Database URL: $DbUrl" -ForegroundColor Gray
Write-Host "User: $DbUser" -ForegroundColor Gray
Write-Host ""

# Check if gradlew exists
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "Error: gradlew.bat not found. Please run this script from the project root." -ForegroundColor Red
    exit 1
}

# Test migrations with H2
Write-Host "Running Flyway migrations with H2..." -ForegroundColor Yellow
& .\gradlew.bat testMigrations -PtestDbUrl=$DbUrl -PtestDbUser=$DbUser -PtestDbPassword=$DbPassword

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nMigrations tested successfully!" -ForegroundColor Green
} else {
    Write-Host "`nMigration test failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

