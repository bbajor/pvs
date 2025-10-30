# Nächste Schritte nach Token-Setup

Nachdem du ein GitHub Personal Access Token erstellt und als Benutzervariablen gesetzt hast, folge diesen Schritten:

## ✅ Schritt 1: Environment-Variablen in aktueller Session laden

**Option A: PowerShell neu starten** (empfohlen)
- Schließe die aktuelle PowerShell
- Öffne eine neue PowerShell
- Variablen sollten jetzt verfügbar sein

**Option B: Manuell in aktueller Session setzen**
```powershell
# Token und Username aus Benutzervariablen lesen (funktioniert nur wenn sie wirklich gesetzt sind):
$env:GITHUB_TOKEN = [System.Environment]::GetEnvironmentVariable('GITHUB_TOKEN', 'User')
$env:GITHUB_USERNAME = [System.Environment]::GetEnvironmentVariable('GITHUB_USERNAME', 'User')

# Oder manuell eingeben (wenn oben nicht funktioniert):
$env:GITHUB_TOKEN = 'ghp_DEIN_TOKEN_HIER'
$env:GITHUB_USERNAME = 'DEIN_GITHUB_USERNAME'
```

## ✅ Schritt 2: Docker Login testen

```powershell
cd D:\workspace\pvs

# Login testen:
$env:GITHUB_TOKEN | docker login ghcr.io -u $env:GITHUB_USERNAME --password-stdin
```

**Erwartete Ausgabe:**
```
Login Succeeded
```

**Falls Fehler:** Prüfe, ob Token und Username korrekt sind.

## ✅ Schritt 3: Image Pull testen

```powershell
# Teste, ob du das private Image pullen kannst:
docker pull ghcr.io/bbajor/pvs:dev-latest
```

**Erwartete Ausgabe:**
- Image wird heruntergeladen ODER
- "Error: image not found" (falls noch kein Image gebaut wurde - ist OK)

**Falls "unauthorized":**
- Token hat nicht die richtigen Berechtigungen
- Prüfe: Token hat `repo` Scope oder `read:packages` aktiviert?

## ✅ Schritt 4: Auto-Update Script testen

```powershell
cd D:\workspace\pvs
.\scripts\local\auto-update-dev.ps1
```

**Erwartete Ausgabe:**
- `[INFO] Docker Login für private Image...`
- `[OK] Docker Login erfolgreich`
- `[INFO] Pulling neues Image...`
- Entweder: `[OK] Bereits neueste Version installiert` ODER
- Oder: `[INFO] Neues Image gefunden - deploye...`

## ✅ Schritt 5: Scheduled Task einrichten (Optional, für automatische Updates)

Falls du möchtest, dass das Script automatisch läuft:

1. **Task Scheduler öffnen:**
   - `Win+R` → `taskschd.msc` → Enter

2. **Neue Aufgabe erstellen:**
   - Rechtsklick "Aufgabenplanungsbibliothek" → "Aufgabe erstellen..."

3. **Konfiguration:**
   - **Allgemein:**
     - Name: `PVS Dev Auto-Update`
     - ✅ "Mit höchsten Privilegien ausführen" aktivieren
   
   - **Trigger:**
     - Neu → Wiederholung alle 5 Minuten
   
   - **Aktionen:**
     - Programm: `powershell.exe`
     - Argumente: `-ExecutionPolicy Bypass -File "D:\workspace\pvs\scripts\local\auto-update-dev.ps1"`
     - Starten in: `D:\workspace\pvs`

4. **Testen:**
   ```powershell
   # Task manuell ausführen:
   schtasks /run /tn "PVS Dev Auto-Update"
   ```

## 🔧 Troubleshooting

### "Environment-Variable nicht gefunden"

**Lösung:**
- PowerShell komplett neu starten
- Oder: Variablen in aktueller Session manuell setzen (siehe Schritt 1)

### "Login Succeeded" aber "unauthorized" beim Pull

**Mögliche Ursachen:**
1. Token hat nicht die richtigen Scopes
   - Prüfe: Classic Token mit `repo` Scope?
2. Image ist noch nicht gebaut
   - Push zu `dev` Branch → GitHub Actions baut Image
3. Image ist wirklich privat und Token hat keinen Zugriff
   - Prüfe Repository-Berechtigungen

### Script läuft aber findet kein neues Image

**Normal wenn:**
- Image wurde noch nicht gebaut (nach Push zu dev)
- Bereits neueste Version installiert

**Test:**
```powershell
# Manuell prüfen ob Image existiert:
docker pull ghcr.io/bbajor/pvs:dev-latest
```

## ✅ Checkliste

- [ ] Environment-Variablen in PowerShell-Session verfügbar (prüfe: `$env:GITHUB_TOKEN`)
- [ ] Docker Login erfolgreich
- [ ] Image Pull funktioniert (oder "not found" wenn noch nicht gebaut)
- [ ] Auto-Update Script läuft ohne Fehler
- [ ] Scheduled Task eingerichtet (optional)

## 🎯 Nächste Schritte

Nach erfolgreichem Setup:

1. **Push zu dev Branch** → GitHub Actions baut automatisch Image
2. **Nach 5-10 Minuten** → Scheduled Task pulled neues Image
3. **Automatisches Deployment** → Container wird neu gestartet

Fertig! 🎉

