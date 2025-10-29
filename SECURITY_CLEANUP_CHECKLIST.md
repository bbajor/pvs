# Security Cleanup Checklist

## ✅ Bereits erledigt
- [x] Neuer SSH-Key generiert
- [x] Neuer SSH-Key in GitHub Secrets hochgeladen
- [x] Alter SSH-Key vom Server gelöscht
- [x] Neuer Public Key auf Server eingetragen

## 🔧 Noch zu erledigen

### 1. Git-Historie bereinigen
```bash
bash cleanup-ssh-key.sh
# Option 2 wählen (Git Filter-Branch)
```

### 2. **KRITISCH: Datenbank-Passwort ändern!** 🔐

Das alte DB-Passwort war in der Git-Historie:
- Altes Passwort: `wLje1DKyHUUEMffCW09jCzS7neZlF2OV+MTO1TJGd+k=`
- DB User: `pvs_user`
- DB Name: `pvs_prod`

**Schritte:**
1. Auf Server einloggen: `ssh root@188.245.253.179`
2. Neues Passwort für DB-User setzen:
   ```bash
   # In PostgreSQL
   sudo -u postgres psql
   \c pvs_prod
   ALTER USER pvs_user WITH PASSWORD 'NEUES_STARKES_PASSWORT_HIER';
   \q
   ```
3. Neues Passwort in GitHub Secrets aktualisieren:
   - Gehe zu: https://github.com/bbajor/pvs/settings/secrets/actions
   - Bearbeite `PROD_DB_PASSWORD`
   - Setze das neue Passwort

### 3. Force-Push nach Historie-Bereinigung
```bash
git push origin --force --all
git push origin --force --tags
```

### 4. Weitere Credentials prüfen
Die folgenden Werte waren auch in den Commits (aber weniger kritisch):
- `HETZNER_HOST`: `188.245.253.179` (öffentliche IP - OK)
- `HETZNER_USER`: `root` (Standard - OK)
- `PROD_DB_HOST`: `localhost` (OK)
- `PROD_DB_NAME`: `pvs_prod` (OK)
- `PROD_DB_USER`: `pvs_user` (OK, aber mit neuem Passwort kombinieren)

### 5. GitHub Secrets validieren
Stelle sicher, dass alle Secrets aktuell sind:
- ✅ `HETZNER_SSH_KEY` - Neuer Key
- ✅ `PROD_DB_PASSWORD` - Neues Passwort (nach Änderung)

## ⚠️ Sicherheitshinweise

- **Das alte DB-Passwort ist kompromittiert** - ändere es sofort!
- Der SSH-Key wurde bereits erneuert ✓
- Nach Historie-Bereinigung: Team-Mitglieder informieren (falls vorhanden)
- Überwache Server-Logs auf verdächtige Aktivitäten

