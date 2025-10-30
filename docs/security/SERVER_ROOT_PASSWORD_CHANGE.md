# Root-Passwort auf Hetzner Server aendern

Diese Anleitung beschreibt, wie du das Root-Passwort auf dem Hetzner-Server aenderst und SSH-Key-Authentifizierung einrichtest.

---

## Schritt 1: SSH-Verbindung aufbauen

```bash
# Auf Server einloggen (mit aktuellen Credentials)
ssh root@<HETZNER_IP>

# Beispiel:
ssh root@188.245.253.179
```

Du wirst nach dem aktuellen Passwort gefragt.

---

## Schritt 2: Root-Passwort aendern

Nach dem Login wird automatisch gefragt (falls Passwort-Wechsel erzwungen):

```
(current) UNIX password: <AKTUELLES PASSWORT>
New password: <NEUES PASSWORT>
Retype new password: <NEUES PASSWORT NOCHMAL>
```

**ODER manuell aendern:**

```bash
# Passwort-Befehl ausfuehren
passwd

# Dann interaktiv:
# (current) UNIX password: <ALTES PASSWORT>
# New password: <NEUES PASSWORT>
# Retype new password: <NEUES PASSWORT>
```

**Wichtig:** 
- Starkes Passwort waehlen (min. 12 Zeichen, besser 16+)
- Am besten mit Buchstaben, Zahlen, Sonderzeichen
- Passwort sicher speichern (Passwort-Manager)

---

## Schritt 3: SSH-Key-Authentifizierung einrichten

Nach Passwort-Wechsel solltest du SSH-Key-Authentifizierung einrichten, um zukuenftig ohne Passwort einloggen zu koennen.

### 3.1 Public Key von lokalem Rechner kopieren

**Auf deinem lokalen Rechner:**

```bash
# Linux/Mac
cat ~/.ssh/hetzner_deploy.pub

# Windows PowerShell
type $env:USERPROFILE\.ssh\hetzner_deploy.pub

# Windows Git Bash
cat ~/.ssh/hetzner_deploy.pub
```

**Kopiere den kompletten Public Key** (sollte mit `ssh-ed25519` oder `ssh-rsa` beginnen).

### 3.2 Public Key auf Server hinzufuegen

**Auf dem Hetzner Server:**

```bash
# Verzeichnis erstellen (falls nicht vorhanden)
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# Public Key hinzufuegen
# Ersetze DEIN_PUBLIC_KEY_HIER mit dem tatsaechlichen Public Key
echo "DEIN_PUBLIC_KEY_HIER" >> ~/.ssh/authorized_keys

# Berechtigungen setzen
chmod 600 ~/.ssh/authorized_keys

# Verifizieren
cat ~/.ssh/authorized_keys
```

**WICHTIG**: 
- Ersetze `DEIN_PUBLIC_KEY_HIER` mit dem kompletten Public Key von deinem lokalen Rechner
- Der Public Key sollte in einer Zeile stehen (keine Zeilenumbrueche)

### 3.3 SSH-Key generieren (falls noch nicht vorhanden)

Falls du noch keinen SSH-Key hast:

```bash
# Auf lokalem Rechner
ssh-keygen -t ed25519 -C "github-actions-hetzner" -f ~/.ssh/hetzner_deploy

# Keine Passphrase eingeben (Enter druecken)
```

Siehe auch: [`docs/security/SSH_KEY_SETUP.md`](SSH_KEY_SETUP.md) für detaillierte Anleitung.

---

## Schritt 4: SSH-Verbindung mit Key testen

**Auf deinem lokalen Rechner:**

```bash
# Linux/Mac
ssh -i ~/.ssh/hetzner_deploy root@<HETZNER_IP> "echo 'SSH funktioniert ohne Passwort!'"

# Windows PowerShell
ssh -i $env:USERPROFILE\.ssh\hetzner_deploy root@<HETZNER_IP> "echo 'SSH funktioniert ohne Passwort!'"

# Windows Git Bash
ssh -i ~/.ssh/hetzner_deploy root@<HETZNER_IP> "echo 'SSH funktioniert ohne Passwort!'"
```

**Sollte jetzt ohne Passwort funktionieren!**

---

## Schritt 5: Passwort-Authentifizierung deaktivieren (optional, empfohlen)

Sobald SSH-Key-Authentifizierung funktioniert, kannst du Passwort-Login deaktivieren:

```bash
# Auf Server
nano /etc/ssh/sshd_config

# Finde und aendere:
# PasswordAuthentication yes
# zu:
PasswordAuthentication no

# SSH-Service neu starten
systemctl restart sshd

# Verifikation (von neuem Terminal aus)
# Sollte weiterhin mit Key funktionieren
```

**WICHTIG**: Teste vorher, dass SSH-Key funktioniert! Sonst kannst du dich nicht mehr einloggen.

---

## Alternative: Passwort-Wechsel ueber Hetzner Console

Falls SSH mit Passwort nicht funktioniert:

1. Gehe zu: https://console.hetzner.cloud
2. Waehle deinen Server
3. Klicke auf **"Console"** (VNC-Konsole)
4. Logge dich ein (mit altem Passwort)
5. Fuehre `passwd` aus
6. Dann SSH-Zugriff versuchen

---

## Troubleshooting

### "Permission denied (publickey)"

**Ursache**: Public Key nicht korrekt auf Server oder falsche Berechtigungen

**Lösung**:
```bash
# Auf Server prüfen
cat ~/.ssh/authorized_keys

# Berechtigungen prüfen
ls -la ~/.ssh/

# Sollte sein:
# drwx------ .ssh
# -rw------- authorized_keys
```

### "Too many authentication failures"

**Lösung**:
```bash
ssh -i ~/.ssh/hetzner_deploy -o IdentitiesOnly=yes root@<HETZNER_IP>
```

### Passwort vergessen

Falls du das Root-Passwort vergessen hast:

1. Nutze Hetzner Console (siehe Alternative oben)
2. ODER setze Passwort via Hetzner Rescue-Modus zurück

---

## Sicherheitshinweise

- ✅ **SSH-Key verwenden**: Deutlich sicherer als Passwort
- ✅ **Passwort trotzdem stark**: Falls Key nicht funktionieren sollte
- ✅ **Passwort-Authentifizierung deaktivieren**: Nach erfolgreichem Key-Setup
- ✅ **Regelmäßig Passwörter rotieren**: Alle 3-6 Monate
- ✅ **Zwei-Faktor-Authentifizierung**: Zusätzliche Sicherheit (optional)

---

## Checkliste

- [ ] Root-Passwort geaendert
- [ ] Neues Passwort sicher gespeichert
- [ ] SSH-Key generiert (falls nicht vorhanden)
- [ ] Public Key auf Server kopiert
- [ ] SSH-Verbindung mit Key getestet
- [ ] Passwort-Authentifizierung deaktiviert (optional)
- [ ] Verbindung funktioniert zuverlaessig

---

**Siehe auch:**
- [`docs/security/SSH_KEY_SETUP.md`](SSH_KEY_SETUP.md) - Detaillierte SSH-Key Anleitung
- [`docs/security/DATABASE_PASSWORD_CHANGE.md`](DATABASE_PASSWORD_CHANGE.md) - Datenbank-Passwort aendern

