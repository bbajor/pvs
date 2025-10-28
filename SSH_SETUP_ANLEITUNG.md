# SSH Key Setup - Anleitung

## ✅ Was bereits erledigt ist:

- SSH Key wurde erstellt: `hetzner_deploy`
- Key ist unter: `C:\Users\bjoer\.ssh\hetzner_deploy`

## 🔧 Schritt 1: Public Key auf Server kopieren

### Option A: Manuell (Empfohlen)

1. **Public Key anzeigen:**
   ```powershell
   type $env:USERPROFILE\.ssh\hetzner_deploy.pub
   ```

2. **SSH auf Server** (wird nach Passwort fragen):
   ```powershell
   ssh root@188.245.253.179
   ```

3. **Auf Server ausführen:**
   ```bash
   mkdir -p ~/.ssh
   nano ~/.ssh/authorized_keys
   ```

4. **Public Key einfügen** (aus Schritt 1) und speichern (Ctrl+X, Y, Enter)

5. **Berechtigungen setzen:**
   ```bash
   chmod 600 ~/.ssh/authorized_keys
   chmod 700 ~/.ssh
   exit
   ```

### Option B: Via SSH mit Passwort (Einmalig)

```powershell
# Public Key kopieren
$pubKey = Get-Content $env:USERPROFILE\.ssh\hetzner_deploy.pub
ssh root@188.245.253.179 "echo '$pubKey' >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
```

## ✅ Schritt 2: SSH-Zugriff testen

```powershell
ssh -i $env:USERPROFILE\.ssh\hetzner_deploy root@188.245.253.179
```

**Sollte jetzt ohne Passwort funktionieren!**

## 🔐 Schritt 3: GitHub Secret konfigurieren

1. **Private Key anzeigen:**
   ```powershell
   type $env:USERPROFILE\.ssh\hetzner_deploy
   ```

2. **Ganzen Inhalt kopieren** (inkl. `-----BEGIN...` und `-----END...`)

3. **GitHub → Repository → Settings → Secrets → Actions → New Secret:**
   - Name: `HETZNER_SSH_KEY`
   - Value: `<Eingefügter Private Key>`

## 🚀 Schritt 4: Server-Setup starten

Nach erfolgreichem SSH-Zugriff:

```powershell
scp setup-server.sh root@188.245.253.179:/root/
ssh root@188.245.253.179 "chmod +x /root/setup-server.sh && /root/setup-server.sh"
```

**Bereit, den nächsten Schritt auszuführen?** 😊

