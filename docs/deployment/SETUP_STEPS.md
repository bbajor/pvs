# Schritt-für-Schritt Setup Anleitung

## Phase 1: Code vorbereiten

### ✅ Schritt 1.1: Code zu GitHub pushen

```bash
git push origin master
```

**Warte bis:** Code ist auf GitHub

---

## Phase 2: Hetzner VPS erstellen

### ✅ Schritt 2.1: Hetzner Account & Server

1. Öffne [hetzner.com/cloud](https://www.hetzner.com/cloud)
2. Login oder Account erstellen
3. **"Add Server"** klicken
4. **Location**: Wähle "Nürnberg" oder "Falkenstein" (Deutschland)
5. **Image**: Ubuntu 22.04
6. **Type**: **CX21** (2 vCPU, 4GB RAM, 40GB SSD)
7. **SSH Key**: 
   - Neuen Key erstellen oder vorhandenen wählen
   - Oder später via SSH einloggen
8. **Name**: `pvs-server` (optional)
9. **"Create & Buy now"** → Server wird erstellt
10. **IP-Adresse notieren!** (z.B. `123.45.67.89`)

**Warte bis:** Server ist bereit (1-2 Minuten)

---

## Phase 3: Server Setup

### ✅ Schritt 3.1: SSH-Zugriff testen

```bash
ssh root@<DEINE_IP>
```

**Falls Fehler:** Prüfe, ob SSH Key korrekt ist, oder nutze Passwort-Login

### ✅ Schritt 3.2: Setup-Script ausführen

**Option A: Script von lokalem Rechner ausführen**

```bash
# Script auf Server kopieren
scp setup-server.sh root@<DEINE_IP>:/root/

# Auf Server ausführen
ssh root@<DEINE_IP> "chmod +x /root/setup-server.sh && /root/setup-server.sh"
```

**Option B: Kommandos manuell ausführen**

```bash
ssh root@<DEINE_IP>
# Dann alle Befehle aus setup-server.sh einzeln ausführen
```

**Warte bis:** Docker & Docker Compose sind installiert

### ✅ Schritt 3.3: Docker ohne sudo nutzen

```bash
# Gruppe neu laden
newgrp docker

# Test
docker --version
docker-compose --version
```

Sollte `docker ps` ohne sudo funktionieren.

---

## Phase 4: Projekt-Setup

### ✅ Schritt 4.1: Docker Compose File kopieren

```bash
# Von deinem lokalen Rechner
scp docker-compose.production.yml root@<DEINE_IP>:/opt/pvs/
```

**Warte bis:** Datei ist auf Server

### ✅ Schritt 4.2: PostgreSQL starten

```bash
ssh root@<DEINE_IP>
cd /opt/pvs
docker-compose -f docker-compose.production.yml up -d postgres
```

**Warte bis:** PostgreSQL läuft (ca. 10 Sekunden)

### ✅ Schritt 4.3: Datenbanken initialisieren

**Option A: Mit Script**

```bash
scp init-databases.sh root@<DEINE_IP>:/opt/pvs/
ssh root@<DEINE_IP> "cd /opt/pvs && chmod +x init-databases.sh && ./init-databases.sh"
```

**Option B: Manuell**

```bash
ssh root@<DEINE_IP>
cd /opt/pvs

# Warte bis PostgreSQL bereit ist
docker exec pvs-postgres pg_isready -U pvs_user

# Datenbanken erstellen
docker exec -i pvs-postgres psql -U pvs_user -d postgres <<EOF
CREATE DATABASE pvs_dev;
CREATE DATABASE pvs_test;
CREATE DATABASE pvs_prod;
EOF
```

**Erwartete Ausgabe:**
```
CREATE DATABASE
CREATE DATABASE
CREATE DATABASE
```

---

## Phase 5: GitHub Secrets konfigurieren

### ✅ Schritt 5.1: SSH Key für Deployment generieren

```bash
# Auf deinem lokalen Rechner
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/hetzner_deploy -N ""
```

**Wichtig:** Keine Passphrase eingeben (Enter drücken)

### ✅ Schritt 5.2: Public Key auf Server hinzufügen

```bash
cat ~/.ssh/hetzner_deploy.pub | ssh root@<DEINE_IP> "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys"
```

**Test:**
```bash
ssh -i ~/.ssh/hetzner_deploy root@<DEINE_IP> "echo 'SSH Key funktioniert!'"
```

Sollte ohne Passwort funktionieren.

### ✅ Schritt 5.3: Postgres Password abrufen

```bash
ssh root@<DEINE_IP> "grep POSTGRES_PASSWORD /opt/pvs/.env"
```

**Kopiere das Password!**

### ✅ Schritt 5.4: GitHub Secrets erstellen

GitHub Repository → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

**Folgende Secrets erstellen:**

| Secret Name | Wert | Beispiel |
|------------|------|----------|
| `HETZNER_HOST` | IP-Adresse | `123.45.67.89` |
| `HETZNER_USER` | `root` | `root` |
| `HETZNER_SSH_KEY` | Private Key Inhalt | OpenSSH Private Key (mit BEGIN/END Markern) |
| `PROD_DB_HOST` | `localhost` | `localhost` |
| `PROD_DB_NAME` | `pvs_prod` | `pvs_prod` |
| `PROD_DB_USER` | `pvs_user` | `pvs_user` |
| `PROD_DB_PASSWORD` | Postgres Password | `abc123...` |

**Private Key anzeigen:**
```bash
cat ~/.ssh/hetzner_deploy
```
→ Ganzen Inhalt kopieren (inkl. der BEGIN/END Marker)

**Erwartete Ausgabe:** ✅ Alle Secrets sind konfiguriert

---

## Phase 6: Erste Deployment testen

### ✅ Schritt 6.1: Workflow manuell auslösen

GitHub Repository → **Actions** → **"Build and Push Docker Images (Hetzner)"** → **Run workflow**

**Settings:**
- Branch: `master`
- Stage: `dev`
- Run workflow

**Warte bis:** Workflow ist erfolgreich (5-10 Minuten beim ersten Mal)

### ✅ Schritt 6.2: Deployment prüfen

```bash
ssh root@<DEINE_IP>
cd /opt/pvs

# Status prüfen
docker-compose -f docker-compose.production.yml ps

# Logs ansehen
docker-compose -f docker-compose.production.yml logs -f pvs-dev

# Health Check
curl http://localhost:8080/actuator/health
```

**Erwartete Ausgabe:**
```json
{"status":"UP"}
```

---

## Phase 7: Domain & SSL (Optional)

Falls du eine Domain hast:

### ✅ Schritt 7.1: DNS konfigurieren

Bei deinem Domain-Provider:

```
Type: A
Name: dev.pvs
Value: <DEINE_IP>
TTL: 3600

Type: A
Name: test.pvs
Value: <DEINE_IP>
TTL: 3600

Type: A
Name: pvs (oder @)
Value: <DEINE_IP>
TTL: 3600
```

### ✅ Schritt 7.2: docker-compose.production.yml anpassen

```bash
ssh root@<DEINE_IP>
cd /opt/pvs
nano docker-compose.production.yml
```

Ersetze `pvs.example.com` mit deiner Domain.

### ✅ Schritt 7.3: Traefik starten

```bash
docker-compose -f docker-compose.production.yml up -d traefik
```

**Warte 5 Minuten** → SSL wird automatisch eingerichtet!

---

## ✅ Fertig!

Deine App sollte jetzt erreichbar sein:
- **Dev**: `http://<DEINE_IP>:8080` (oder `https://dev.pvs.example.com`)
- **Test**: Wird bei Push auf `master` automatisch deployed
- **Prod**: Via GitHub Actions manuell deployen

## Troubleshooting

**Problem:** Workflow schlägt fehl
- Prüfe GitHub Actions Logs
- Prüfe SSH Key (Secret korrekt?)
- Prüfe Server: `docker ps`

**Problem:** Container startet nicht
```bash
docker logs pvs-dev
```

**Problem:** Database Connection Error
```bash
docker exec -it pvs-postgres psql -U pvs_user -l
```

Soll ich bei einem spezifischen Schritt helfen? 😊

