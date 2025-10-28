# 🚀 Erstes Deployment starten

## Option 1: Via GitHub Actions (Empfohlen)

1. **GitHub Repository öffnen:**
   - https://github.com/bbajor/pvs

2. **"Actions" Tab** klicken

3. **"Build and Push Docker Images (Hetzner)"** Workflow auswählen

4. **"Run workflow"** Button (rechts oben)

5. **Einstellungen:**
   - Branch: `master`
   - Stage: `dev`
   - "Run workflow" klicken

6. **Workflow läuft jetzt:**
   - Build des Docker Images
   - Push zu GitHub Container Registry
   - Deployment zu Hetzner Server
   - Dauer: ~5-10 Minuten

## Option 2: Manuell testen (Schnell)

```powershell
# Prüfe ob Docker Image gebaut werden kann (lokal)
docker build -t pvs-test .

# Oder warte auf GitHub Actions
```

## Status prüfen

**Während des Deployments:**
- GitHub → Actions → Klicke auf laufenden Workflow → Siehe Logs

**Nach dem Deployment:**
```powershell
ssh -i $env:USERPROFILE\.ssh\hetzner_deploy root@188.245.253.179 "cd /opt/pvs && docker compose ps"
```

**Health Check:**
```powershell
ssh -i $env:USERPROFILE\.ssh\hetzner_deploy root@188.245.253.179 "curl http://localhost:8080/actuator/health"
```

## Troubleshooting

**Workflow schlägt fehl?**
- Prüfe GitHub Actions Logs
- Prüfe SSH-Zugriff: `ssh -i $env:USERPROFILE\.ssh\hetzner_deploy root@188.245.253.179`
- Prüfe Server: `docker ps` und `docker logs pvs-dev`

