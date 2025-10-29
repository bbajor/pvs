# PowerShell Execution Policy - Setup

Wenn du den Fehler "Die Ausführung von Skripts auf diesem System deaktiviert ist" bekommst, musst du die PowerShell Execution Policy anpassen.

## 🚀 Schnellste Lösung (für einmalige Ausführung)

Führe das Script mit Bypass-Parameter aus:

```powershell
# Von D:\workspace\pvs aus:
powershell.exe -ExecutionPolicy Bypass -File ".\scripts\local\setup-windows-runner.ps1"
```

## ⚙️ Dauerhafte Lösung (empfohlen)

### Option 1: RemoteSigned für aktuellen Benutzer (sicher)

```powershell
# PowerShell als Administrator öffnen
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Script ausführen
.\scripts\local\setup-windows-runner.ps1
```

**Was bedeutet das?**
- Lokale Scripts können ausgeführt werden
- Downloads aus Internet müssen signiert sein
- Gilt nur für deinen Benutzer (nicht System-weit)
- ✅ **Empfohlen für Entwicklung**

### Option 2: Bypass nur für dieses Script (temporär)

```powershell
# Temporär für aktuelle Session
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process

# Script ausführen
.\scripts\local\setup-windows-runner.ps1
```

**Nachteil:** Muss bei jeder neuen PowerShell-Session neu gesetzt werden.

### Option 3: Unrestricted (nicht empfohlen)

```powershell
# PowerShell als Administrator
Set-ExecutionPolicy -ExecutionPolicy Unrestricted -Scope CurrentUser
```

**Warnung:** Erlaubt alle Scripts ohne Warnung - weniger sicher!

## 🔍 Aktuelle Policy prüfen

```powershell
Get-ExecutionPolicy -List
```

**Ausgabe erklärt:**
- `Scope: MachinePolicy` - GPO-Policy (meist nicht gesetzt)
- `Scope: UserPolicy` - Benutzer-Policy (meist nicht gesetzt)
- `Scope: Process` - Aktuelle Session
- `Scope: CurrentUser` - Dein Benutzer (empfohlen)
- `Scope: LocalMachine` - Alle Benutzer (benötigt Admin)

## ✅ Empfohlene Einstellung für Entwicklung

```powershell
# PowerShell als Administrator öffnen
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Prüfen
Get-ExecutionPolicy
# Sollte ausgeben: RemoteSigned
```

## 📝 Für dieses Projekt

Alle Scripts in `scripts/local/` sind lokal erstellt und können sicher ausgeführt werden.

**Für Setup-Scripts:**
```powershell
# Script direkt mit Bypass:
powershell.exe -ExecutionPolicy Bypass -File ".\scripts\local\setup-windows-runner.ps1"
```

**Oder dauerhaft ändern:**
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
.\scripts\local\setup-windows-runner.ps1
```

## 🔒 Sicherheit

**Warum gibt es Execution Policies?**
- Schützt vor bösartigen Scripts
- Verhindert versehentliche Ausführung

**RemoteSigned bedeutet:**
- Lokale Scripts: ✅ Ausführbar
- Downloads: ⚠️ Nur wenn signiert
- Beste Balance zwischen Sicherheit und Funktionalität

## 🐛 Troubleshooting

### "Set-ExecutionPolicy: Access Denied"

**Lösung:** PowerShell als Administrator öffnen

```powershell
# Rechtsklick auf PowerShell → "Als Administrator ausführen"
# Dann:
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Policy wird nicht gespeichert

**Prüfe Gruppenrichtlinien:**
```powershell
Get-ExecutionPolicy -List
```

Wenn `MachinePolicy` oder `UserPolicy` gesetzt ist, kann die Group Policy überschreiben.

### Script läuft trotzdem nicht

```powershell
# Prüfe tatsächliche Policy
Get-ExecutionPolicy -List | Where-Object { $_.ExecutionPolicy -ne 'Undefined' }

# Oder mit Bypass direkt ausführen
powershell.exe -ExecutionPolicy Bypass -File ".\scripts\local\setup-windows-runner.ps1"
```

