# OpenFaaS Deployment für PVS Serverless Functions

OpenFaaS ermöglicht das Deployment von Spring Cloud Functions auf Hetzner-Servern.

## Voraussetzungen

- Podman oder Docker installiert
- OpenFaaS CLI installiert: `curl -sSL https://cli.openfaas.com | sudo sh`
- Kubernetes oder Docker Swarm (optional, für Production)

## Lokale Entwicklung

### OpenFaaS mit Docker Compose starten

```bash
cd docker/openfaas
docker-compose up -d
```

OpenFaaS UI: http://localhost:8080
Gateway: http://localhost:31112

## Deployment auf Hetzner

### 1. OpenFaaS auf Hetzner installieren

```bash
# Mit Docker Swarm
docker swarm init
kubectl apply -f https://raw.githubusercontent.com/openfaas/faas-netes/master/namespaces.yml
kubectl apply -f https://raw.githubusercontent.com/openfaas/faas-netes/master/yaml_arm64.yml

# Oder mit Podman Compose (siehe podman-compose.yml)
podman-compose up -d
```

### 2. Function deployen

```powershell
# Windows PowerShell
.\scripts\deployment\deploy-function.ps1 -ServiceName patient-service -FunctionName createPatient
```

### 3. Function testen

```bash
# Via OpenFaaS CLI
faas-cli invoke patient-service/createPatient

# Via HTTP
curl -X POST http://localhost:31112/function/patient-service-createPatient \
  -H "Content-Type: application/json" \
  -d '{"institutionId": 1, "firstName": "Max", "lastName": "Mustermann"}'
```

## Funktionen

- **Platform-agnostisch**: Spring Cloud Functions laufen auf jedem FaaS-Provider
- **Auto-Scaling**: OpenFaaS skaliert automatisch basierend auf Load
- **Monitoring**: Integriert mit Prometheus/Grafana
- **Security**: Built-in Authentication/Authorization


