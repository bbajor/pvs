# SSH-Tunnel für PVS Test-Instanz (PowerShell)
# Usage: .\ssh-tunnel-test.ps1 [-Server "user@server"] [-SshKey "path/to/key"] [-LocalPort 8081]

param(
    [string]$Server = $env:HETZNER_TEST_SERVER,
    [string]$SshKey = $env:HETZNER_SSH_KEY,
    [int]$LocalPort = 8081,
    [int]$RemotePort = 8081,
    [string]$LogFile = "$env:TEMP\ssh-tunnel-test.log"
)

if (-not $Server) {
    $Server = Read-Host "Server (user@hostname)"
}

Write-Host "🔗 PVS Test SSH-Tunnel Setup" -ForegroundColor Cyan
Write-Host "=============================" -ForegroundColor Cyan
Write-Host "Server: $Server"
Write-Host "Local Port: $LocalPort"
Write-Host "Remote Port: $RemotePort"
Write-Host ""

# Prüfe ob Port bereits belegt ist
$portInUse = Get-NetTCPConnection -LocalPort $LocalPort -ErrorAction SilentlyContinue
if ($portInUse) {
    Write-Host "⚠️  Port $LocalPort ist bereits belegt" -ForegroundColor Yellow
    Write-Host "Bestehender Prozess:"
    $portInUse | Format-Table -AutoSize
    $response = Read-Host "Prozess beenden? (j/n)"
    if ($response -eq "j" -or $response -eq "J") {
        $process = Get-Process -Id $portInUse.OwningProcess -ErrorAction SilentlyContinue
        if ($process) {
            Stop-Process -Id $process.Id -Force
            Start-Sleep -Seconds 2
            Write-Host "✅ Prozess beendet" -ForegroundColor Green
        }
    } else {
        Write-Host "❌ Abgebrochen" -ForegroundColor Red
        exit 1
    }
}

# SSH-Optionen
$sshOpts = "-o ServerAliveInterval=60 -o ServerAliveCountMax=3 -o ExitOnForwardFailure=yes -o StrictHostKeyChecking=accept-new"
if ($SshKey) {
    $sshOpts = "$sshOpts -i `"$SshKey`""
}

# Prüfe SSH-Verbindung
Write-Host "🔍 Prüfe SSH-Verbindung..." -ForegroundColor Yellow
$testConnection = & ssh $sshOpts -o ConnectTimeout=5 "$Server" "echo 'SSH-Verbindung OK'" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ SSH-Verbindung fehlgeschlagen" -ForegroundColor Red
    Write-Host "Bitte prüfe:"
    Write-Host "  - SSH-Key ist auf Server hinterlegt"
    Write-Host "  - Server ist erreichbar"
    Write-Host "  - Firewall erlaubt Port 22"
    exit 1
}
Write-Host "✅ SSH-Verbindung OK" -ForegroundColor Green

# Prüfe ob Test-Instanz läuft
Write-Host "🔍 Prüfe Test-Instanz auf Server..." -ForegroundColor Yellow
$healthCheck = & ssh $sshOpts "$Server" "curl -f http://localhost:$RemotePort/actuator/health" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Test-Instanz scheint nicht zu laufen" -ForegroundColor Yellow
    Write-Host "Bitte starte die Test-Instanz auf dem Server:"
    Write-Host "  podman-compose -f podman-compose.production.yml --profile test up -d"
    $response = Read-Host "Trotzdem fortfahren? (j/n)"
    if ($response -ne "j" -and $response -ne "J") {
        exit 1
    }
} else {
    Write-Host "✅ Test-Instanz läuft" -ForegroundColor Green
}

# SSH-Tunnel starten
Write-Host ""
Write-Host "🚀 Starte SSH-Tunnel..." -ForegroundColor Yellow
Write-Host "Log-Datei: $LogFile"
Write-Host ""

# Start-Job für SSH-Tunnel
$scriptBlock = {
    param($Server, $LocalPort, $RemotePort, $SshOpts, $LogFile)
    & ssh -L "${LocalPort}:localhost:${RemotePort}" -N $SshOpts "$Server" *> $LogFile
}

$job = Start-Job -ScriptBlock $scriptBlock -ArgumentList $Server, $LocalPort, $RemotePort, $sshOpts, $LogFile

# Warten bis Tunnel etabliert ist
Start-Sleep -Seconds 3

# Tunnel-Verifikation
$tunnelActive = Get-NetTCPConnection -LocalPort $LocalPort -ErrorAction SilentlyContinue
if ($tunnelActive) {
    Write-Host "✅ SSH-Tunnel erfolgreich erstellt" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 Test-Instanz erreichbar unter:" -ForegroundColor Cyan
    Write-Host "   http://localhost:$LocalPort" -ForegroundColor White
    Write-Host ""
    Write-Host "🔍 Health Check:" -ForegroundColor Yellow
    try {
        $health = Invoke-WebRequest -Uri "http://localhost:$LocalPort/actuator/health" -UseBasicParsing -TimeoutSec 5
        Write-Host "✅ Health Check erfolgreich" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  Health Check fehlgeschlagen (Instanz startet möglicherweise noch)" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "📝 Logs: Get-Content $LogFile -Tail 20 -Wait" -ForegroundColor Cyan
    Write-Host "🛑 Beenden: Stop-Job -Id $($job.Id); Remove-Job -Id $($job.Id)" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Job-ID: $($job.Id)" -ForegroundColor Gray
} else {
    Write-Host "❌ SSH-Tunnel konnte nicht erstellt werden" -ForegroundColor Red
    Write-Host "Logs:"
    Get-Content $LogFile -Tail 20
    Stop-Job -Id $job.Id -ErrorAction SilentlyContinue
    Remove-Job -Id $job.Id -ErrorAction SilentlyContinue
    exit 1
}

