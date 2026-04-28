# PowerShell Script zum Deployen von Spring Cloud Functions zu OpenFaaS
# Usage: .\scripts\deployment\deploy-function.ps1 -ServiceName patient-service -FunctionName createPatient

param(
    [Parameter(Mandatory=$true)]
    [string]$ServiceName,
    
    [Parameter(Mandatory=$true)]
    [string]$FunctionName,
    
    [string]$OpenFaaSGateway = "http://localhost:31112",
    
    [string]$OpenFaaSUser = "admin",
    
    [string]$OpenFaaSPassword = "",
    
    [switch]$Build = $true,
    
    [switch]$Push = $false
)

$ErrorActionPreference = "Stop"

Write-Host "🚀 Deploying function: $ServiceName/$FunctionName" -ForegroundColor Cyan

# Prüfe ob OpenFaaS CLI installiert ist
$faasCli = Get-Command faas-cli -ErrorAction SilentlyContinue
if (-not $faasCli) {
    Write-Host "❌ OpenFaaS CLI nicht gefunden. Installiere..." -ForegroundColor Yellow
    Write-Host "Installiere OpenFaaS CLI: curl -sSL https://cli.openfaas.com | sudo sh" -ForegroundColor Yellow
    Write-Host "Oder für Windows: choco install faas-cli" -ForegroundColor Yellow
    exit 1
}

# Function Image Name
$imageName = "pvs-$ServiceName-$FunctionName"
$imageTag = "latest"
$fullImageName = "$imageName`:$imageTag"

# Build Function JAR
if ($Build) {
    Write-Host "📦 Building function JAR..." -ForegroundColor Cyan
    $buildResult = & ./gradlew :$ServiceName:bootJar 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Build fehlgeschlagen!" -ForegroundColor Red
        Write-Host $buildResult
        exit 1
    }
    Write-Host "✅ Build erfolgreich" -ForegroundColor Green
}

# Build Docker Image
Write-Host "🐳 Building Docker image: $fullImageName" -ForegroundColor Cyan

$dockerfile = @"
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY $ServiceName/build/libs/*.jar app.jar
ENV SPRING_CLOUD_FUNCTION_DEFINITION=$FunctionName
ENV SPRING_PROFILES_ACTIVE=serverless
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
"@

$dockerfilePath = "$ServiceName/Dockerfile.function"
$dockerfile | Out-File -FilePath $dockerfilePath -Encoding UTF8

try {
    & docker build -f $dockerfilePath -t $fullImageName .
    if ($LASTEXITCODE -ne 0) {
        throw "Docker build failed"
    }
    Write-Host "✅ Docker image erstellt" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker build fehlgeschlagen: $_" -ForegroundColor Red
    exit 1
}

# Push to Registry (optional)
if ($Push) {
    Write-Host "📤 Pushing image to registry..." -ForegroundColor Cyan
    # TODO: Registry-URL konfigurieren
    Write-Host "⚠️  Push-Funktion noch nicht implementiert" -ForegroundColor Yellow
}

# Deploy to OpenFaaS
Write-Host "🚀 Deploying to OpenFaaS..." -ForegroundColor Cyan

# Erstelle OpenFaaS Stack File
$stackFile = @"
version: 1.0
provider:
  name: openfaas
  gateway: $OpenFaaSGateway
functions:
  $FunctionName:
    lang: dockerfile
    handler: .
    image: $fullImageName
    environment:
      SPRING_CLOUD_FUNCTION_DEFINITION: $FunctionName
      SPRING_PROFILES_ACTIVE: serverless
    secrets:
      - db-credentials
"@

$stackFilePath = "$ServiceName/stack.yml"
$stackFile | Out-File -FilePath $stackFilePath -Encoding UTF8

try {
    # Login to OpenFaaS (wenn Password gesetzt)
    if ($OpenFaaSPassword) {
        $env:OPENFAAS_URL = $OpenFaaSGateway
        $env:PASSWORD = $OpenFaaSPassword
        & faas-cli login --username $OpenFaaSUser --password-stdin
    }
    
    # Deploy function
    & faas-cli deploy -f $stackFilePath --image $fullImageName
    if ($LASTEXITCODE -ne 0) {
        throw "OpenFaaS deployment failed"
    }
    
    Write-Host "✅ Function deployed successfully!" -ForegroundColor Green
    Write-Host "🔗 Function URL: $OpenFaaSGateway/function/$FunctionName" -ForegroundColor Cyan
    
} catch {
    Write-Host "❌ Deployment fehlgeschlagen: $_" -ForegroundColor Red
    exit 1
}

Write-Host "✨ Deployment abgeschlossen!" -ForegroundColor Green


