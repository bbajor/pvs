# Private Docker Images - Setup für lokales Deployment

Falls dein GitHub Container Registry Image privat ist, musst du dich authentifizieren, um Images zu pullen.

## 🎯 Option 1: GitHub Personal Access Token (PAT) - Empfohlen

### Schritt 1: Personal Access Token erstellen

GitHub bietet zwei Arten von Tokens an. Für Container Registry gibt es unterschiedliche Optionen:

#### Option A: Classic Token (Empfohlen für Container Registry)

1. **Gehe zu GitHub:**
   - https://github.com/settings/tokens
   - Oder: GitHub → Profil → Settings → Developer settings → Personal access tokens → **Tokens (classic)**

2. **Neuen Classic Token erstellen:**
   - Klicke auf **"Generate new token"** → **"Generate new token (classic)"**
   - **Note:** z.B. `Docker ghcr.io Pull`
   - **Expiration:** Wähle deine Präferenz (z.B. 90 Tage oder No expiration)
   - **Scopes:** Aktiviere:
     - ✅ **`read:packages`** (wichtig! zum Pullen von privaten Images)
       - Hinweis: `read:packages` ist als verschachtelter Scope unter `write:packages` sichtbar
       - Einfach den Haken bei `read:packages` setzen (nicht das übergeordnete `write:packages`)
     - ❌ **`repo` Scope allein reicht NICHT** für ghcr.io Package-Zugriff
     - ✅ **`write:packages`** (optional, nur wenn du auch pushen willst)

3. **Token generieren:**
   - Klicke auf **"Generate token"**
   - ⚠️ **WICHTIG:** Kopiere den Token **SOFORT** (wird nur einmal angezeigt!)
   - Format: `ghp_...` (beginnt mit `ghp_`)

#### Option B: Fine-grained Token (wenn Classic nicht verfügbar)

Falls du nur Fine-grained Tokens siehst:

1. **Gehe zu:**
   - https://github.com/settings/tokens
   - Klicke auf **"Generate new token"** → **"Generate new token (fine-grained)"**

2. **Token konfigurieren:**
   - **Token name:** z.B. `Docker ghcr.io Pull`
   - **Expiration:** Wähle deine Präferenz
   - **Repository access:**
     - Wähle **"Only select repositories"** oder **"All repositories"**
     - Wähle das Repository: `bbajor/pvs`
   
3. **Berechtigungen (Permissions) setzen:**
   - Unter **"Repository permissions"** → **"Contents"**:
     - Setze auf **"Read-only"** (ermöglicht Zugriff auf Packages im Repository)
   - Optional: Unter **"Account permissions"** → **"Packages"** → **"Read"** (falls verfügbar)

4. **Token generieren:**
   - Klicke auf **"Generate token"**
   - ⚠️ **WICHTIG:** Kopiere den Token **SOFORT** (wird nur einmal angezeigt!)
   - Format: `github_pat_...` (beginnt mit `github_pat_`)

**Hinweis:** Classic Tokens funktionieren meist zuverlässiger für ghcr.io. Falls `read:packages` bei Classic nicht sichtbar ist, nutze stattdessen den `repo` Scope (gewährt auch Packages-Zugriff).

### Schritt 2: Docker mit Token einloggen

#### Variante A: Manuell (einmalig für interaktive Nutzung)

```powershell
# In PowerShell oder Command Prompt:
docker login ghcr.io -u USERNAME --password-stdin

# Dann Token eingeben (oder aus Zwischenablage einfügen)
# Username ist dein GitHub Username
```

**Oder als ein Befehl:**

```powershell
# PowerShell:
$token = "ghp_DEIN_TOKEN_HIER"
$token | docker login ghcr.io -u DEIN_GITHUB_USERNAME --password-stdin

# Oder aus Environment-Variable:
$env:GITHUB_TOKEN | docker login ghcr.io -u $env:GITHUB_USERNAME --password-stdin
```

#### Variante B: Automatisch im Script (für Scheduled Tasks)

Das Script `scripts/local/auto-update-dev.ps1` unterstützt automatisches Login:

**Environment-Variablen setzen:**

```powershell
# Benutzer-Umgebungsvariablen (empfohlen):
[System.Environment]::SetEnvironmentVariable('GITHUB_TOKEN', 'ghp_DEIN_TOKEN_HIER', 'User')
[System.Environment]::SetEnvironmentVariable('GITHUB_USERNAME', 'DEIN_GITHUB_USERNAME', 'User')

# System-Umgebungsvariablen (für alle Benutzer):
[System.Environment]::SetEnvironmentVariable('GITHUB_TOKEN', 'ghp_DEIN_TOKEN_HIER', 'Machine')
[System.Environment]::SetEnvironmentVariable('GITHUB_USERNAME', 'DEIN_GITHUB_USERNAME', 'Machine')
```

**Oder via GUI:**
1. Windows-Taste → "Umgebungsvariablen" → Enter
2. "Umgebungsvariablen..." → "Neu..." (unter "Benutzervariablen")
3. Variablen hinzufügen:
   - `GITHUB_TOKEN` = `ghp_DEIN_TOKEN`
   - `GITHUB_USERNAME` = `dein-github-username`

### Schritt 3: Test

```powershell
# Docker Login testen:
docker login ghcr.io -u $env:GITHUB_USERNAME --password-stdin
# Token eingeben oder aus Environment-Variable:
$env:GITHUB_TOKEN | docker login ghcr.io -u $env:GITHUB_USERNAME --password-stdin

# Image pullen testen:
docker pull ghcr.io/bbajor/pvs:dev-latest
```

---

## 🎯 Option 2: Docker Credential Helper (Windows)

Für noch bessere Sicherheit kannst du Windows Credential Manager nutzen:

### Setup:

1. **Docker Login (einmalig):**
   ```powershell
   docker login ghcr.io -u DEIN_GITHUB_USERNAME
   # Token eingeben
   ```

2. **Credentials werden in Windows Credential Manager gespeichert:**
   - Windows-Taste → "Anmeldeinformationsverwaltung" → Enter
   - Sollte `https://ghcr.io` unter "Windows-Anmeldeinformationen" erscheinen

3. **Script nutzt automatisch gespeicherte Credentials**

**Nachteil:** Credentials können beim Neustart/Update verloren gehen.

---

## 🎯 Option 3: Config-Datei (nicht empfohlen für Production)

Falls keine anderen Methoden funktionieren, kann das Token in einer lokalen Config-Datei gespeichert werden:

```powershell
# Erstelle: scripts/local/docker-credentials.env (nicht ins Git!)
# Inhalt:
GITHUB_TOKEN=ghp_DEIN_TOKEN
GITHUB_USERNAME=DEIN_USERNAME

# Script lädt automatisch (wenn vorhanden)
```

⚠️ **WICHTIG:** Diese Datei sollte **NIE** ins Git committed werden! (sollte in `.gitignore` sein)

---

## 📋 Setup-Checkliste für Private Images

- [ ] GitHub Personal Access Token erstellt
  - [ ] Classic Token: `read:packages` ODER `repo` Scope aktiviert
  - [ ] Fine-grained Token: `Contents: Read-only` Permission gesetzt
- [ ] Environment-Variablen gesetzt:
  - [ ] `GITHUB_TOKEN`
  - [ ] `GITHUB_USERNAME` (optional, falls anders als Repo-Owner)
- [ ] Docker Login getestet
- [ ] Image Pull getestet: `docker pull ghcr.io/bbajor/pvs:dev-latest`
- [ ] Script getestet: `.\scripts\local\auto-update-dev.ps1`

---

## 🔒 Sicherheit

### ✅ Best Practices:

1. **Token-Rotation:**
   - Regelmäßig neue Tokens generieren (alle 90 Tage)
   - Alte Tokens löschen

2. **Token-Berechtigungen minimal:**
   - Classic Token: Nur `read:packages` wenn verfügbar, sonst `repo` (read-only)
   - Fine-grained Token: Nur `Contents: Read-only` für das benötigte Repository
   - Nicht mehr Berechtigungen als nötig

3. **Classic vs Fine-grained:**
   - Classic Tokens funktionieren zuverlässiger für ghcr.io
   - Fine-grained Tokens bieten mehr Kontrolle, aber manchmal weniger kompatibel

3. **Token nicht committen:**
   - Nie in Git-Repository
   - Nie in öffentlichen Scripts
   - Nur in Environment-Variablen oder Credential Manager

4. **Für Scheduled Tasks:**
   - Environment-Variablen auf Benutzer-Ebene (nicht System)
   - Oder Windows Credential Manager nutzen

---

## 🔧 Troubleshooting

### "unauthorized" Fehler beim Pull

```powershell
# 1. Prüfe ob Environment-Variablen gesetzt sind:
$env:GITHUB_TOKEN
$env:GITHUB_USERNAME

# 2. Teste manuelles Login:
$env:GITHUB_TOKEN | docker login ghcr.io -u $env:GITHUB_USERNAME --password-stdin

# 3. Prüfe gespeicherte Credentials:
docker logout ghcr.io
# Dann neu einloggen
```

### Token abgelaufen

```powershell
# Neuen Token erstellen (siehe Schritt 1)
# Environment-Variable aktualisieren:
[System.Environment]::SetEnvironmentVariable('GITHUB_TOKEN', 'ghp_NEUER_TOKEN', 'User')

# PowerShell-Session neu starten oder:
$env:GITHUB_TOKEN = 'ghp_NEUER_TOKEN'
```

### Scheduled Task kann nicht pullen

```powershell
# Prüfe ob Task mit richtigen Credentials läuft:
# 1. Task Scheduler → Task → "Einstellungen"
# 2. "Unabhängig von der Benutzeranmeldung ausführen" deaktivieren
#    Oder System-Umgebungsvariablen verwenden statt Benutzer-Variablen

# Alternativ: Windows Credential Manager prüfen
# Windows-Taste → "Anmeldeinformationsverwaltung"
```

---

## 🎯 Empfehlung für Scheduled Tasks

**Für automatische Updates:**

1. ✅ **Environment-Variablen** (Benutzer-Ebene) nutzen
2. ✅ Token hat `read:packages` Scope
3. ✅ Script prüft automatisch Credentials
4. ✅ Token-Rotation alle 90 Tage

**Vorteile:**
- Funktioniert mit Scheduled Tasks
- Sicher (kein Token im Code)
- Einfach zu aktualisieren

