# Passwort-Wechsel auf Hetzner Server

## Schritt 1: SSH-Verbindung aufbauen

```powershell
ssh root@188.245.253.179
```

Du wirst nach dem aktuellen Passwort gefragt.

## Schritt 2: Passwort ändern

Nach dem Login wird automatisch gefragt:
```
(current) UNIX password: <AKTUELLES PASSWORT>
New password: <NEUES PASSWORT>
Retype new password: <NEUES PASSWORT NOCHMAL>
```

**Wichtig:** 
- Starkes Passwort wählen (min. 12 Zeichen)
- Am besten mit Buchstaben, Zahlen, Sonderzeichen

## Schritt 3: Public Key hinzufügen

Nach erfolgreichem Passwort-Wechsel:

```bash
# Verzeichnis erstellen (falls nicht vorhanden)
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# Public Key hinzufügen
echo "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIIUfmtBvnplytzesXcinl9XScn2XKJfpFWDXzIPpD6/1 github-actions-hetzner" >> ~/.ssh/authorized_keys

# Berechtigungen setzen
chmod 600 ~/.ssh/authorized_keys

# Verifizieren
cat ~/.ssh/authorized_keys
```

## Schritt 4: Verbindung testen

Auf deinem lokalen Rechner (neue PowerShell):

```powershell
ssh -i $env:USERPROFILE\.ssh\hetzner_deploy root@188.245.253.179 "echo 'SSH funktioniert ohne Passwort!'"
```

**Sollte jetzt ohne Passwort funktionieren!**

## Alternative: Passwort-Wechsel ohne SSH

Falls SSH mit Passwort nicht funktioniert:
1. Hetzner Console öffnen (im Dashboard → Server → Console)
2. Dort Passwort ändern
3. Dann SSH-Zugriff versuchen

