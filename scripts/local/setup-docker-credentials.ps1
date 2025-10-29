# Setup-Script für Docker Credentials (GitHub Container Registry)
# Führt durch das Setup von GitHub Personal Access Token

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Docker Credentials Setup für ghcr.io" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Prüfe ob schon gesetzt
if ($env:GITHUB_TOKEN -and $env:GITHUB_USERNAME) {
    Write-Host "[INFO] Environment-Variablen sind bereits gesetzt:" -ForegroundColor Green
    Write-Host "       GITHUB_USERNAME: $env:GITHUB_USERNAME" -ForegroundColor Gray
    Write-Host "       GITHUB_TOKEN: $($env:GITHUB_TOKEN.Substring(0,7))..." -ForegroundColor Gray
    Write-Host ""
    $continue = Read-Host "Moechtest du sie ueberschreiben? (j/N)"
    if ($continue -ne "j" -and $continue -ne "J") {
        Write-Host "[OK] Setup abgebrochen" -ForegroundColor Yellow
        exit 0
    }
}

Write-Host ""
Write-Host "Schritt 1: GitHub Personal Access Token erstellen" -ForegroundColor Yellow
Write-Host ""
Write-Host "OPTION A - Classic Token (empfohlen):" -ForegroundColor Cyan
Write-Host "1. Gehe zu: https://github.com/settings/tokens" -ForegroundColor White
Write-Host "2. Klicke auf 'Generate new token' -> 'Generate new token (classic)'" -ForegroundColor White
Write-Host "3. Note: z.B. 'Docker ghcr.io Pull'" -ForegroundColor White
Write-Host "4. Expiration: Waehle deine Praeferenz" -ForegroundColor White
Write-Host "5. Scopes:" -ForegroundColor White
Write-Host "   - Falls 'read:packages' sichtbar: Aktiviere 'read:packages'" -ForegroundColor Gray
Write-Host "   - Falls NICHT sichtbar: Aktiviere 'repo' (gewaehrt auch Packages-Zugriff)" -ForegroundColor Gray
Write-Host "6. Generate token und KOPIERE IHN SOFORT!" -ForegroundColor White
Write-Host ""
Write-Host "OPTION B - Fine-grained Token (wenn Classic nicht moeglich):" -ForegroundColor Cyan
Write-Host "1. Gehe zu: https://github.com/settings/tokens" -ForegroundColor White
Write-Host "2. Klicke auf 'Generate new token' -> 'Generate new token (fine-grained)'" -ForegroundColor White
Write-Host "3. Repository access: Waehle 'bbajor/pvs'" -ForegroundColor White
Write-Host "4. Permissions -> Contents: Setze auf 'Read-only'" -ForegroundColor White
Write-Host "5. Generate token und KOPIERE IHN SOFORT!" -ForegroundColor White
Write-Host ""
$githubUsername = Read-Host "GitHub Username"
$tokenPrompt = "GitHub Personal Access Token (ghp_... oder github_pat_...)"
$githubToken = Read-Host $tokenPrompt -AsSecureString

# Convert SecureString to plain string
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($githubToken)
$plainToken = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)

if (-not $plainToken) {
    Write-Host "[ERROR] Token darf nicht leer sein" -ForegroundColor Red
    exit 1
}

# Validiere Token-Format (Classic: ghp_..., Fine-grained: github_pat_...)
if (-not ($plainToken.StartsWith("ghp_") -or $plainToken.StartsWith("github_pat_"))) {
    Write-Host "[WARN] Token-Format ungewoehnlich (erwartet: ghp_... oder github_pat_...)" -ForegroundColor Yellow
    $continue = Read-Host "Trotzdem fortfahren? (j/N)"
    if ($continue -ne "j" -and $continue -ne "J") {
        Write-Host "[OK] Setup abgebrochen" -ForegroundColor Yellow
        exit 0
    }
}

Write-Host ""
Write-Host "Schritt 2: Environment-Variablen setzen" -ForegroundColor Yellow
Write-Host "Moechtest du die Variablen fuer:" -ForegroundColor White
Write-Host "1. Benutzer (empfohlen fuer Scheduled Tasks)" -ForegroundColor White
Write-Host "2. System (fuer alle Benutzer)" -ForegroundColor White
$scope = Read-Host "Auswahl (1/2)"

if ($scope -eq "2") {
    [System.Environment]::SetEnvironmentVariable('GITHUB_TOKEN', $plainToken, 'Machine')
    [System.Environment]::SetEnvironmentVariable('GITHUB_USERNAME', $githubUsername, 'Machine')
    Write-Host "[OK] System-Umgebungsvariablen gesetzt" -ForegroundColor Green
} else {
    [System.Environment]::SetEnvironmentVariable('GITHUB_TOKEN', $plainToken, 'User')
    [System.Environment]::SetEnvironmentVariable('GITHUB_USERNAME', $githubUsername, 'User')
    Write-Host "[OK] Benutzer-Umgebungsvariablen gesetzt" -ForegroundColor Green
}

# Setze auch für aktuelle Session
$env:GITHUB_TOKEN = $plainToken
$env:GITHUB_USERNAME = $githubUsername

Write-Host ""
Write-Host "Schritt 3: Docker Login testen" -ForegroundColor Yellow
Write-Host "[INFO] Teste Docker Login..." -ForegroundColor Cyan
try {
    $loginOutput = $plainToken | docker login ghcr.io -u $githubUsername --password-stdin 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Docker Login erfolgreich!" -ForegroundColor Green
    } else {
        Write-Host "[WARN] Docker Login fehlgeschlagen" -ForegroundColor Yellow
        Write-Host "       Output: $loginOutput" -ForegroundColor Gray
    }
} catch {
    Write-Host "[ERROR] Docker Login Fehler: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Setup abgeschlossen!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Naechste Schritte:" -ForegroundColor Yellow
Write-Host "1. PowerShell-Session neu starten (fuer Environment-Variablen)" -ForegroundColor White
Write-Host "2. Oder direkt testen:" -ForegroundColor White
Write-Host "   .\scripts\local\auto-update-dev.ps1" -ForegroundColor Gray
Write-Host ""
Write-Host "Wichtig: Token nicht ins Git committen!" -ForegroundColor Yellow

