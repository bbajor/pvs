# Neuer SSH-Key Generator für Hetzner Deployment (PowerShell)
# Erstellt einen neuen SSH-Key und bereitet ihn für GitHub Secrets vor

Write-Host "🔑 Neuer SSH-Key Generator" -ForegroundColor Cyan
Write-Host "==========================" -ForegroundColor Cyan
Write-Host ""

# Prüfe ob .ssh Verzeichnis existiert
$sshDir = "$env:USERPROFILE\.ssh"
if (-not (Test-Path $sshDir)) {
    Write-Host "📁 Erstelle .ssh Verzeichnis..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $sshDir -Force | Out-Null
    icacls $sshDir /inheritance:r
    icacls $sshDir /grant "${env:USERNAME}:F"
}

# Generiere neuen SSH-Key
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$keyName = "hetzner_deploy_$timestamp"
$keyPath = "$sshDir\$keyName"

Write-Host "🔧 Generiere neuen SSH-Key..." -ForegroundColor Yellow
Write-Host "Key-Name: $keyName" -ForegroundColor Gray
Write-Host "Key-Pfad: $keyPath" -ForegroundColor Gray
Write-Host ""

# SSH-Key generieren (ohne Passphrase für GitHub Actions)
try {
    ssh-keygen -t ed25519 -C "github-actions-hetzner-$timestamp" -f $keyPath -N '""' 2>&1 | Out-Null
    
    Write-Host "✅ SSH-Key erfolgreich generiert!" -ForegroundColor Green
}
catch {
    Write-Host "❌ Fehler beim Generieren des SSH-Keys: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "📋 Public Key (für Server):" -ForegroundColor Cyan
Write-Host "============================" -ForegroundColor Cyan
Get-Content "$keyPath.pub" | Write-Host -ForegroundColor White
Write-Host ""

Write-Host "🔐 Private Key (für GitHub Secret):" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Get-Content $keyPath | Write-Host -ForegroundColor White
Write-Host ""

# Zeige den Private Key nochmal in einem Copy-freundlichen Format
Write-Host "📋 Private Key zum Kopieren:" -ForegroundColor Yellow
Write-Host "----------------------------" -ForegroundColor Yellow
$privateKeyContent = Get-Content $keyPath -Raw
Write-Host $privateKeyContent -ForegroundColor Gray
Write-Host ""

Write-Host "📝 Nächste Schritte:" -ForegroundColor Cyan
Write-Host "====================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. 🔑 Public Key auf Server kopieren:" -ForegroundColor Yellow
Write-Host "   ssh-copy-id -i ${keyPath}.pub root@188.245.253.179" -ForegroundColor Gray
Write-Host "   ODER manuell:" -ForegroundColor Gray
Write-Host "   Get-Content ${keyPath}.pub | ssh root@188.245.253.179 'mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys'" -ForegroundColor Gray
Write-Host ""
Write-Host "2. 🔐 Private Key als GitHub Secret speichern:" -ForegroundColor Yellow
Write-Host "   - Gehe zu: https://github.com/bbajor/pvs/settings/secrets/actions" -ForegroundColor Gray
Write-Host "   - Bearbeite 'HETZNER_SSH_KEY'" -ForegroundColor Gray
Write-Host "   - Kopiere den Private Key von oben und füge ihn ein" -ForegroundColor Gray
Write-Host ""
Write-Host "3. 🧪 Teste die Verbindung:" -ForegroundColor Yellow
Write-Host "   ssh -i $keyPath root@188.245.253.179 'echo \"SSH funktioniert!\"'" -ForegroundColor Gray
Write-Host ""
Write-Host "4. 🗑️  Alten Key entfernen (falls vorhanden):" -ForegroundColor Yellow
Write-Host "   Remove-Item ~\.ssh\hetzner_deploy* -ErrorAction SilentlyContinue" -ForegroundColor Gray
Write-Host ""

# Erstelle eine .env Datei für lokale Tests
Write-Host "💾 Erstelle .env Datei für lokale Tests..." -ForegroundColor Yellow
$envContent = @"
# SSH Key für Hetzner Deployment
HETZNER_SSH_KEY_PATH=$keyPath
HETZNER_HOST=188.245.253.179
HETZNER_USER=root
"@

$envContent | Out-File -FilePath ".env.ssh-key" -Encoding UTF8
Write-Host "✅ .env.ssh-key Datei erstellt!" -ForegroundColor Green
Write-Host ""

# Prüfe ob .gitignore existiert und füge .env.ssh-key hinzu
if (Test-Path ".gitignore") {
    $gitignoreContent = Get-Content ".gitignore" -Raw
    if ($gitignoreContent -notmatch "\.env\.ssh-key") {
        Write-Host "📝 Füge .env.ssh-key zu .gitignore hinzu..." -ForegroundColor Yellow
        Add-Content -Path ".gitignore" -Value "`n.env.ssh-key"
        Write-Host "✅ .gitignore aktualisiert!" -ForegroundColor Green
    }
}
else {
    Write-Host "📝 Erstelle .gitignore mit .env.ssh-key..." -ForegroundColor Yellow
    ".env.ssh-key" | Out-File -FilePath ".gitignore" -Encoding UTF8
    Write-Host "✅ .gitignore erstellt!" -ForegroundColor Green
}

Write-Host ""
Write-Host "⚠️  Wichtig:" -ForegroundColor Yellow
Write-Host "- Bewahre den Private Key sicher auf" -ForegroundColor Gray
Write-Host "- Füge .env.ssh-key zu .gitignore hinzu" -ForegroundColor Gray
Write-Host "- Der alte SSH-Key sollte als kompromittiert betrachtet werden" -ForegroundColor Gray
Write-Host ""

Write-Host "🎉 Setup abgeschlossen! Der neue SSH-Key ist bereit für das Deployment." -ForegroundColor Green

