# Cloud Deployment Workflow

Schritt-für-Schritt Anleitung für Deployment von lokal → dev → test → prod.

## Übersicht

```
┌─────────┐     ┌──────┐     ┌──────┐     ┌──────┐
│  Lokal  │ --> │ dev  │ --> │ test │ --> │ prod │
│  Tests  │     │      │     │      │     │      │
└─────────┘     └──────┘     └──────┘     └──────┘
```

## 1. Lokale Tests

### Voraussetzungen

- Alle Cloud-Features lokal getestet
- Tests erfolgreich
- Code auf Branch `cloud-ready-and-multi-tenancy`

### Testen

```bash
# Lokale Tests durchführen
./gradlew test

# Cloud-Features lokal testen
# Siehe: docs/LOCAL_TESTING.md
podman-compose -f podman-compose.production.yml --profile test up -d
curl http://localhost:8081/actuator/health
```

## 2. Merge in dev

### Branch vorbereiten

```bash
# Aktueller Stand von dev holen
git checkout dev
git pull origin dev

# Feature-Branch mergen
git merge cloud-ready-and-multi-tenancy

# Konflikte lösen (falls vorhanden)
# ...

# Commits prüfen
git log --oneline -10
```

### Merge durchführen

```bash
# Push zu dev
git push origin dev

# Oder via Pull Request (empfohlen)
# 1. Erstelle PR: cloud-ready-and-multi-tenancy → dev
# 2. Review durchführen
# 3. Merge PR
```

### Nach Merge

```bash
# Branch aufräumen (optional)
git branch -d cloud-ready-and-multi-tenancy
git push origin --delete cloud-ready-and-multi-tenancy
```

## 3. Merge in test

### Branch vorbereiten

```bash
# Test-Branch auschecken
git checkout test
git pull origin test

# Dev mergen
git merge dev

# Konflikte lösen (falls vorhanden)
# ...

# Push zu test
git push origin test
```

### Deployment auf Test-Instanz

#### 3.1 Server vorbereiten (einmalig)

```bash
# Auf Hetzner Server (als root)
curl -fsSL https://raw.githubusercontent.com/bbajor/pvs/master/scripts/deployment/setup-server.sh | bash

# Firewall konfigurieren
sudo ./scripts/deployment/setup-firewall.sh

# Als Benutzer 'pvs' einloggen
su - pvs
cd /opt/pvs

# Repository klonen
git clone https://github.com/bbajor/pvs.git .

# Environment-Datei erstellen
cp .env.example .env
nano .env  # Passwörter anpassen
```

#### 3.2 Deployment via GitHub Actions

1. GitHub → Actions → "Cloud Deployment (Hetzner)"
2. Run workflow
3. Environment: `test`
4. Starte Deployment

#### 3.3 Manuelles Deployment

```bash
# Auf Server (als Benutzer 'pvs')
cd /opt/pvs
git pull origin test

# Image pullen und deployen
./scripts/deployment/deploy-hetzner.sh test latest
```

#### 3.4 SSH-Tunnel einrichten

```bash
# Lokal (auf deinem Rechner)
./scripts/deployment/ssh-tunnel-test.sh user@hetzner-server.example.com

# Test-Instanz erreichbar unter:
# http://localhost:8081
```

#### 3.5 Test-Instanz testen

```bash
# Health Check
curl http://localhost:8081/actuator/health

# Login testen
# Browser: http://localhost:8081

# Logs prüfen
ssh user@hetzner-server.example.com
podman logs -f pvs-test
```

## 4. Merge in master (Production)

### Voraussetzungen

- Test-Instanz läuft stabil
- Alle Tests auf Test-Instanz erfolgreich
- Keine kritischen Bugs

### Branch vorbereiten

```bash
# Master-Branch auschecken
git checkout master
git pull origin master

# Test mergen
git merge test

# Konflikte lösen (falls vorhanden)
# ...

# Push zu master
git push origin master
```

### Deployment auf Production

#### 4.1 Via GitHub Actions

1. GitHub → Actions → "Cloud Deployment (Hetzner)"
2. Run workflow
3. Environment: `prod`
4. Starte Deployment

#### 4.2 Manuelles Deployment

```bash
# Auf Server (als Benutzer 'pvs')
cd /opt/pvs
git pull origin master

# Production deployen
./scripts/deployment/deploy-hetzner.sh prod latest
```

#### 4.3 Production testen

```bash
# Health Check
curl https://pvs.example.com/actuator/health

# Browser
# https://pvs.example.com
```

## 5. Rollback-Strategie

### Test-Instanz Rollback

```bash
# Auf Server
cd /opt/pvs

# Liste verfügbare Images
podman images | grep pvs | grep test

# Altes Image taggen
podman tag ghcr.io/bbajor/pvs:test-backup-YYYYMMDD-HHMMSS \
           ghcr.io/bbajor/pvs:test-latest

# Container neu starten
podman-compose -f podman-compose.production.yml --profile test restart
```

### Production Rollback

```bash
# Auf Server
cd /opt/pvs

# Backup-Image verwenden
podman tag ghcr.io/bbajor/pvs:prod-backup-YYYYMMDD-HHMMSS \
           ghcr.io/bbajor/pvs:prod-latest

# Container neu starten
podman-compose -f podman-compose.production.yml --profile prod restart
```

## 6. Monitoring nach Deployment

### Health Checks

```bash
# Test-Instanz (via SSH-Tunnel)
curl http://localhost:8081/actuator/health

# Production
curl https://pvs.example.com/actuator/health
```

### Logs prüfen

```bash
# Test-Instanz
ssh user@hetzner-server.example.com
podman logs -f pvs-test | tail -100

# Production
podman logs -f pvs-prod | tail -100
```

### Metrics prüfen

```bash
# Prometheus Metrics
curl http://localhost:8081/actuator/prometheus | grep http_server_requests

# Circuit Breaker Status
curl http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state
```

## 7. Checkliste

### Vor Merge in dev

- [ ] Lokale Tests erfolgreich
- [ ] Code-Review durchgeführt
- [ ] Dokumentation aktualisiert
- [ ] Keine kritischen Bugs

### Vor Merge in test

- [ ] Dev-Branch getestet
- [ ] Test-Server vorbereitet
- [ ] Environment-Variablen konfiguriert
- [ ] SSH-Tunnel funktioniert

### Vor Merge in master

- [ ] Test-Instanz läuft stabil
- [ ] Alle Features getestet
- [ ] Performance akzeptabel
- [ ] Keine bekannten Bugs
- [ ] Backup-Strategie aktiv

### Nach Production-Deployment

- [ ] Health Checks erfolgreich
- [ ] Logs zeigen keine Fehler
- [ ] Monitoring aktiv
- [ ] Backup läuft
- [ ] Dokumentation aktualisiert

## 8. Troubleshooting

### Merge-Konflikte

```bash
# Konflikte auflösen
git merge dev
# Konflikte in betroffenen Dateien lösen
git add .
git commit -m "Merge dev: Konflikte gelöst"
```

### Deployment-Fehler

```bash
# Logs prüfen
podman logs pvs-test
podman logs pvs-prod

# Health Check
curl http://localhost:8081/actuator/health

# Container-Status
podman ps -a
```

### SSH-Tunnel-Probleme

Siehe [SSH_TUNNEL_SETUP.md](./SSH_TUNNEL_SETUP.md) für Troubleshooting.

## 9. Best Practices

1. **Immer lokal testen** vor Merge
2. **Test-Instanz** vor Production-Deployment testen
3. **Backup** vor jedem Production-Deployment
4. **Monitoring** nach Deployment aktiv prüfen
5. **Rollback-Plan** bereithalten
6. **Dokumentation** aktuell halten

## 10. Nächste Schritte

Nach erfolgreichem Production-Deployment:

- [ ] Monitoring einrichten (Prometheus, Grafana)
- [ ] Alerting konfigurieren
- [ ] Backup-Verifikation
- [ ] Performance-Optimierung
- [ ] Security-Audit

