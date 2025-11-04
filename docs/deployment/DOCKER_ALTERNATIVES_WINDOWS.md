# Docker-Alternativen für Windows 11

Diese Übersicht zeigt dir kostenfreie Alternativen zu Docker Desktop für die lokale Entwicklung unter Windows 11. Alle Tools sind Open Source und ohne Lizenzbeschränkungen für Development! 🎉

## 🎯 Übersicht der Alternativen

| Tool | Lizenz | Windows-Support | Docker-kompatibel | GUI | Beste für |
|------|--------|----------------|-------------------|-----|-----------|
| **Podman Desktop** | Apache 2.0 | ✅ Native | ✅ 100% | ✅ | Docker-Drop-in-Replacement |
| **Podman + WSL2** | Apache 2.0 | ✅ Via WSL2 | ✅ 100% | ❌ | Minimal-Installation |
| **Rancher Desktop** | Apache 2.0 | ✅ Native | ✅ Ja | ✅ | Kubernetes + Docker |
| **Dockge** | AGPL-3.0 | ✅ Web-UI | ✅ Ja | ✅ Web | Compose-Management |
| **Lazydocker** | MIT | ✅ TUI | ✅ Ja | ✅ Terminal | Schlanke Terminal-UI |
| **CasaOS** | Apache 2.0 | ⚠️ Linux/VM | ⚠️ Limited | ✅ Web | Home-Server-Style |
| **Docker Engine (WSL2)** | Apache 2.0 | ✅ Via WSL2 | ✅ Native | ❌ | Siehe DOCKER_ENGINE_WINDOWS.md |

---

## 🥇 Option 1: Podman Desktop ⭐ **EMPFOHLEN**

**Lizenz:** Apache 2.0 - 100% kostenfrei & keine Beschränkungen! 🎉

### Was ist Podman?

Podman ist ein daemonloser Container-Runtime, der **100% kompatibel** zu Docker ist. Du kannst deine Dockerfiles, docker-compose.yml und alle Docker-Befehle 1:1 verwenden!

### Vorteile

- ✅ **Voller Docker-Drop-in-Replacement** - `docker` → `podman` alias nutzen
- ✅ **Daemonlos** - Sicherer, kein ständig laufender Daemon
- ✅ **Rootless Container** - Läuft ohne Root-Rechte
- ✅ **Native Windows-App** - Wie Docker Desktop
- ✅ **Apache 2.0 Lizenz** - Keine Kommerz-Beschränkungen
- ✅ **100% OCI-kompatibel** - Standard-Container-Images

### Installation

**1. Podman Desktop herunterladen:**

```
https://podman-desktop.io/downloads/windows
```

**2. Installer ausführen:**

- Standard-Installation (keine Custom-Config nötig)
- WSL2 wird automatisch vorbereitet

**3. Erste Schritte:**

```powershell
# Podman prüfen
podman --version

# Test-Container
podman run hello-world

# Docker-Alias erstellen (optional)
# Füge in deine PowerShell-Profile hinzu:
Set-Alias docker podman
```

### Docker-Compose kompatibel

Podman nutzt standardmäßig `podman-compose` oder du installierst `docker-compose` separat:

```powershell
# Podman Compose verwenden
podman-compose up -d

# Oder docker-compose installieren (funktioniert mit Podman)
pip install docker-compose
```

### Web-UI: Podman Desktop bietet eingebautes UI

- ✅ Container-Übersicht
- ✅ Images verwalten
- ✅ Volumes & Networks
- ✅ Logs ansehen
- ✅ Interaktives Terminal

**Podman Desktop ist der beste Ersatz für Docker Desktop!** 🚀

---

## 🥈 Option 2: Rancher Desktop

**Lizenz:** Apache 2.0 - Kostenfrei!

### Was ist Rancher Desktop?

Rancher Desktop bietet Docker **und** Kubernetes in einer GUI-App. Perfekt, wenn du später mit Kubernetes arbeiten möchtest.

### Vorteile

- ✅ **Docker + Kubernetes** - Beide Container-Runtimes
- ✅ **Native Windows-App** - GUI wie Docker Desktop
- ✅ **Automated Testing** - Integrierte Tools
- ✅ **Container Runtime wählen** - Docker oder containerd
- ✅ **Apache 2.0** - Open Source

### Installation

```
https://rancherdesktop.io/getting-started/?platform=windows
```

### Besonderheiten

- Container können mit Docker oder containerd laufen
- Kubernetes-Cluster lokal testen
- Automatisierte Tools für Testing

**Gut für:** Entwickler, die Kubernetes lernen möchten!

---

## 🥉 Option 3: Dockge (Web-UI für Compose)

**Lizenz:** AGPL-3.0 - Open Source & kostenfrei!

### Was ist Dockge?

Dockge ist ein modernes Web-UI speziell für Docker Compose. Perfekt, wenn du mehrere `docker-compose.yml` Files verwaltest.

### Vorteile

- ✅ **Focus auf Compose** - Optimiert für docker-compose.yml
- ✅ **Git-Integration** - Compose-Files aus Git laden
- ✅ **Logs & Monitoring** - Übersichtliche Logs
- ✅ **Web-basiert** - Läuft im Browser
- ✅ **AGPL-3.0** - Open Source

### Installation

**Via Docker (WSL2 oder Docker Engine):**

```bash
cd /opt/stacks  # Oder ein anderes Verzeichnis
git clone https://github.com/louislam/dockge.git
cd dockge

# docker-compose.yml erstellen
cat > docker-compose.yml << EOF
version: '3.8'
services:
  dockge:
    image: louislam/dockge:latest
    restart: unless-stopped
    ports:
      - 5001:5001
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - ./data:/app/data
      - ./stacks:/opt/stacks
    environment:
      - DOCKGE_STACKS_DIR=/opt/stacks
EOF

docker compose up -d
```

**Dockge aufrufen:**

```
http://localhost:5001
```

**Perfekt für:** Compose-Management ohne komplexe Features! 📝

---

## 🎨 Option 4: Lazydocker (Terminal-UI)

**Lizenz:** MIT - Kostenfrei!

### Was ist Lazydocker?

Eine schlanke Terminal-basierte UI für Docker. Minimal-Overhead, läuft direkt im Terminal.

### Vorteile

- ✅ **Ultra-schlank** - Minimaler Ressourcen-Verbrauch
- ✅ **Schnell** - Keine Web-Server-Overhead
- ✅ **Vollständig** - Container, Images, Logs, Volumes
- ✅ **MIT-Lizenz** - Sehr permissiv
- ✅ **Einfach** - Ein Befehl installiert

### Installation

**Via Scoop (Windows):**

```powershell
scoop install lazydocker
```

**Via Chocolatey:**

```powershell
choco install lazydocker
```

**Via go install:**

```powershell
go install github.com/jesseduffield/lazydocker@latest
```

### Nutzung

```powershell
lazydocker
```

**Hotkeys:**
- `d` - Container löschen
- `r` - Container neu starten
- `e` - Logs ansehen
- `s` - Container stoppen
- `'` - Images verwalten

**Perfekt für:** Terminal-Liebhaber! 🖥️

---

## 🏠 Option 5: CasaOS (Home-Server-Style)

**Lizenz:** Apache 2.0 - Open Source!

### Was ist CasaOS?

Ein Home-Server-Management-System mit Docker-Support. Nicht primär für Development, aber für Server-Management interessant.

### Vorteile

- ✅ **All-in-One** - Server-Management-Suite
- ✅ **App Store** - Vorkonfigurierte Container-Apps
- ✅ **Web-UI** - Moderne Oberfläche
- ✅ **Apache 2.0** - Open Source
- ⚠️ **Linux/VM nötig** - Nicht nativ auf Windows

### Installation

CasaOS läuft auf Linux. Für Windows:

1. WSL2 Ubuntu installieren
2. CasaOS in WSL2 installieren

```bash
# In WSL2
curl -fsSL https://get.casaos.io | sudo bash
```

**Gut für:** Home-Server, nicht primär für Development! 🏡

---

## 🛠️ Option 6: Docker Engine + WSL2 + Portainer CE

**Siehe:** [DOCKER_ENGINE_WINDOWS.md](DOCKER_ENGINE_WINDOWS.md)

Die zuvor erstellte Anleitung zeigt, wie du Docker Desktop entfernst und nur die Engine nutzt.

**Kombination:**
- Docker Engine (WSL2)
- Portainer CE (Web-UI)

**Ergebnis:**
- ✅ Docker ohne Desktop-App
- ✅ Kostenfreies Web-UI
- ✅ Minimaler Overhead

---

## 🤔 Welche Option für dich?

### Wenn du **Docker Desktop komplett ersetzen** möchtest:

**→ Podman Desktop** ⭐
- Native Windows-App
- 100% Docker-kompatibel
- Eingebautes UI
- Apache 2.0 Lizenz

### Wenn du **Kubernetes** lernst:

**→ Rancher Desktop**
- Docker + Kubernetes
- Native Windows-App
- Learning-Tools

### Wenn du **mehrere Compose-Files** verwaltest:

**→ Dockge**
- Focus auf Compose
- Git-Integration
- Web-basiert

### Wenn du **Terminal-UI** bevorzugst:

**→ Lazydocker**
- Ultra-schlank
- Schnell
- MIT-Lizenz

### Wenn du **maximalen Docker-Support** brauchst:

**→ Docker Engine + WSL2 + Portainer**
- Docker-native
- Bewährt
- Portainer CE (Apache 2.0)

---

## 🔄 Migration von Docker Desktop zu Podman Desktop

### Schritt-für-Schritt

**1. Docker Desktop sichern:**

```powershell
# Container-Liste exportieren
docker ps -a > docker-containers.txt

# Images-Liste exportieren
docker images > docker-images.txt
```

**2. Podman Desktop installieren:**

- Herunterladen: https://podman-desktop.io/downloads/windows
- Installer ausführen
- WSL2 wird automatisch konfiguriert

**3. Container/Images migrieren:**

```powershell
# Docker Desktop stoppen
# Services → Docker Desktop stoppen

# Images von Docker Hub erneut pullen (funktioniert mit Podman)
podman pull postgres:16
podman pull nginx:alpine

# Compose-Files testen
podman-compose up -d
```

**4. Docker-Alias erstellen (optional):**

```powershell
# PowerShell Profile öffnen
notepad $PROFILE

# Füge hinzu:
Set-Alias docker podman
Set-Alias docker-compose podman-compose
```

**5. Docker Desktop deinstallieren:**

- Windows → Apps → Docker Desktop → Deinstallieren

**✅ Fertig! Du nutzt jetzt Podman Desktop!** 🎉

---

## 🧪 Kompatibilitätstest

### Dockerfile-Kompatibilität

Alle Tools sind OCI-kompatibel - deine Dockerfiles funktionieren:

```dockerfile
# Dein bestehendes Dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

✅ Funktioniert mit Podman, Rancher, Docker Engine

### docker-compose.yml Kompatibilität

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: pvs
      POSTGRES_USER: pvs_user
      POSTGRES_PASSWORD: pvs_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

✅ Funktioniert mit allen Tools

---

## 📊 Vergleichstabelle

| Feature | Podman Desktop | Rancher Desktop | Dockge | Lazydocker | Docker Engine + Portainer |
|---------|---------------|----------------|--------|------------|---------------------------|
| **Lizenz** | Apache 2.0 | Apache 2.0 | AGPL-3.0 | MIT | Apache 2.0 |
| **Windows** | ✅ Native | ✅ Native | ✅ Web | ✅ Native | ✅ Via WSL2 |
| **Docker-kompatibel** | ✅ 100% | ✅ Ja | ✅ Ja | ✅ Ja | ✅ Native |
| **GUI** | ✅ Native | ✅ Native | ✅ Web | ✅ Terminal | ✅ Web (Portainer) |
| **Daemon** | ❌ Daemonlos | ✅ Docker/containerd | ✅ Docker | ✅ Docker | ✅ Docker |
| **Rootless** | ✅ Ja | ⚠️ Optional | ✅ Mit Podman | ✅ Mit Podman | ❌ Nein |
| **Kubernetes** | ❌ Nein | ✅ Ja | ❌ Nein | ❌ Nein | ❌ Nein |
| **WSL2 nötig** | ✅ Ja | ✅ Ja | ✅ Ja | ✅ Ja | ✅ Ja |
| **Setup-Aufwand** | ⭐⭐ Leicht | ⭐⭐ Leicht | ⭐⭐⭐ Mittel | ⭐⭐⭐ Mittel | ⭐⭐⭐⭐ Schwer |
| **Best für** | Docker-Ersatz | Docker+K8s | Compose-UI | Terminal | Minimal |

---

## 🚀 Empfehlung für PVS-Entwicklung

### Für dich als Entwickler:

**→ Podman Desktop** ⭐

**Warum:**
1. ✅ **Apache 2.0 Lizenz** - Keine Kommerz-Beschränkungen (wie bei Docker Desktop)
2. ✅ **100% Docker-kompatibel** - Deine bestehenden Compose-Files funktionieren
3. ✅ **Native Windows-App** - Wie Docker Desktop, nur besser
4. ✅ **Geringeres Risiko** - Daemonlos, rootless
5. ✅ **Keine Lizenz-Sorgen** - 100% Open Source

**Setup:**
1. Podman Desktop installieren
2. Bestehende `docker-compose.dev.yml` testen
3. `docker` → `podman` Alias setzen (optional)
4. Docker Desktop deinstallieren

**Ergebnis:**
- Gleiche Workflow wie mit Docker Desktop
- Keine Lizenzbeschränkungen
- Bessere Performance & Sicherheit

---

## 📚 Weiterführende Links

- **Podman:** https://podman.io/
- **Podman Desktop:** https://podman-desktop.io/
- **Rancher Desktop:** https://rancherdesktop.io/
- **Dockge:** https://dockge.kuma.pet/
- **Lazydocker:** https://github.com/jesseduffield/lazydocker
- **CasaOS:** https://casaos.io/
- **Docker Engine + Portainer:** Siehe [DOCKER_ENGINE_WINDOWS.md](DOCKER_ENGINE_WINDOWS.md)

---

## ✅ Zusammenfassung

**Du hast jetzt mehrere kostenfreie Alternativen:**

1. **Podman Desktop** - Drop-in-Replacement für Docker Desktop ⭐
2. **Rancher Desktop** - Docker + Kubernetes
3. **Dockge** - Compose-Management
4. **Lazydocker** - Terminal-UI
5. **Docker Engine + Portainer** - Minimal-Setup

**Alle Tools sind Open Source und ohne Kommerz-Beschränkungen!** 🎉

**Meine Empfehlung:** Probiere Podman Desktop aus - es ist der beste Ersatz für Docker Desktop ohne Lizenz-Stress! 😊

