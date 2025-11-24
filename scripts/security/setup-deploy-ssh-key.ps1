# SSH Key Setup für deploy-User auf Hetzner
Write-Host "🔑 SSH Key Setup für deploy-User" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""

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
    Write-Host "📋 Public Key (für Server):" -ForegroundColor Cyan
    Get-Content "$sshDir\$keyName.pub"
    Write-Host ""
    Write-Host "🔐 Private Key (für GitHub Secret):" -ForegroundColor Yellow
    Get-Content "$sshDir\$keyName"
} else {
    Write-Host "🔑 Erstelle neuen SSH Key..." -ForegroundColor Yellow
    ssh-keygen -t ed25519 -C "github-actions-hetzner-deploy" -f "$sshDir\$keyName" -N '""'
    
    Write-Host ""
    Write-Host "✅ SSH Key erstellt!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📋 Public Key (für Server):" -ForegroundColor Cyan
    Get-Content "$sshDir\$keyName.pub"
    Write-Host ""
    Write-Host "🔐 Private Key (für GitHub Secret):" -ForegroundColor Yellow
    Get-Content "$sshDir\$keyName"
}

Write-Host ""
Write-Host "📝 Nächste Schritte:" -ForegroundColor Yellow
Write-Host "====================" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. 🖥️  Führe auf dem Hetzner Server aus (als root):" -ForegroundColor Cyan
Write-Host "   curl -fsSL https://raw.githubusercontent.com/bbajor/pvs/master/scripts/deployment/setup-deploy-user.sh | bash"
Write-Host "   # Oder kopiere das Script und führe es aus"
Write-Host ""
Write-Host "2. 🔑 Public Key auf Server kopieren:" -ForegroundColor Cyan
Write-Host "   # Option A: Manuell kopieren"
Write-Host "   type `"$sshDir\$keyName.pub`" | clip"
Write-Host "   # Dann auf Server:"
Write-Host "   sudo -u deploy nano /home/deploy/.ssh/authorized_keys"
Write-Host "   # Einfügen, speichern, dann:"
Write-Host "   sudo chmod 600 /home/deploy/.ssh/authorized_keys"
Write-Host "   sudo chown deploy:deploy /home/deploy/.ssh/authorized_keys"
Write-Host ""
Write-Host "   # Option B: Direkt kopieren (wenn root-Zugriff vorhanden):"
Write-Host "   type `"$sshDir\$keyName.pub`" | ssh root@DEIN_HETZNER_HOST `"sudo -u deploy bash -c 'mkdir -p /home/deploy/.ssh && cat >> /home/deploy/.ssh/authorized_keys && chmod 600 /home/deploy/.ssh/authorized_keys && chown deploy:deploy /home/deploy/.ssh/authorized_keys'`""
Write-Host ""
Write-Host "3. 🧪 Teste die Verbindung:" -ForegroundColor Cyan
Write-Host "   ssh -i `"$sshDir\$keyName`" deploy@DEIN_HETZNER_HOST 'sudo -u pvs whoami'"
Write-Host "   # Sollte 'pvs' ausgeben"
Write-Host ""
Write-Host "4. 🔐 GitHub Secrets konfigurieren:" -ForegroundColor Cyan
Write-Host "   - Gehe zu: https://github.com/bbajor/pvs/settings/secrets/actions"
Write-Host "   - Bearbeite oder erstelle 'HETZNER_USER' = 'deploy'"
Write-Host "   - Bearbeite oder erstelle 'HETZNER_SSH_KEY' = Private Key (oben kopiert)"
Write-Host ""
Write-Host "5. ✅ Teste Deployment über GitHub Actions" -ForegroundColor Green
Write-Host ""





