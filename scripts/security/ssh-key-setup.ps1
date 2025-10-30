# SSH Key Setup für Hetzner
Write-Host "🔍 Suche nach SSH Keys..." -ForegroundColor Cyan

$sshDir = "$env:USERPROFILE\.ssh"
$keyName = "hetzner_deploy"

# Prüfe ob .ssh Verzeichnis existiert
if (-not (Test-Path $sshDir)) {
    Write-Host "📁 Erstelle .ssh Verzeichnis..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $sshDir -Force | Out-Null
}

# Prüfe ob Key bereits existiert
if (Test-Path "$sshDir\$keyName") {
    Write-Host "✅ SSH Key bereits vorhanden: $keyName" -ForegroundColor Green
    Write-Host ""
    Write-Host "Public Key:" -ForegroundColor Cyan
    Get-Content "$sshDir\$keyName.pub"
    Write-Host ""
    Write-Host "Private Key (für GitHub Secret):" -ForegroundColor Cyan
    Get-Content "$sshDir\$keyName"
} else {
    Write-Host "🔑 Erstelle neuen SSH Key..." -ForegroundColor Yellow
    ssh-keygen -t ed25519 -C "github-actions-hetzner" -f "$sshDir\$keyName" -N '""'
    
    Write-Host ""
    Write-Host "✅ SSH Key erstellt!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 Public Key (auf Server kopieren):" -ForegroundColor Cyan
    Get-Content "$sshDir\$keyName.pub"
    Write-Host ""
    Write-Host "🔐 Private Key (für GitHub Secret):" -ForegroundColor Red
    Get-Content "$sshDir\$keyName"
}

Write-Host ""
Write-Host "📝 Nächste Schritte:" -ForegroundColor Yellow
Write-Host "1. Public Key auf Server kopieren:"
Write-Host "   type `"$sshDir\$keyName.pub`" | ssh root@188.245.253.179 `"mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys`""
Write-Host ""
Write-Host "2. Private Key als GitHub Secret speichern (oben kopiert)"

