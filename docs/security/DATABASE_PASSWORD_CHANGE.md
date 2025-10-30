# Datenbank-Passwort aendern

Diese Anleitung beschreibt, wie du das Datenbank-Passwort auf dem Hetzner-Server aenderst.

## Situation

Wenn das Datenbank-Passwort kompromittiert wurde oder rotiert werden soll, muss es sowohl auf dem Server als auch in GitHub Secrets aktualisiert werden.

---

## Schritt 1: Neues Passwort generieren

```bash
# Sichere Passwort generieren (32 Zeichen)
openssl rand -base64 32
```

**Beispiel-Output:**
```
aB3xY9mP2qR7tV5wZ8nK4jL6hG1fD0cE
```

⚠️ **WICHTIG**: Speichere das neue Passwort sicher (Passwort-Manager)!

---

## Schritt 2: Datenbank-Passwort auf Server aendern

### 2.1 Auf Server einloggen

```bash
# Mit deinem SSH-Key einloggen
ssh root@<HETZNER_IP>

# ODER mit spezifischem Key:
ssh -i ~/.ssh/hetzner_deploy root@<HETZNER_IP>
```

### 2.2 Passwort in .env Datei aendern

```bash
cd /opt/pvs

# Backup der .env Datei erstellen
cp .env .env.backup_$(date +%Y%m%d)

# Passwort in .env aendern
nano .env
```

**In der .env Datei findest du:**
```bash
POSTGRES_PASSWORD_PROD=<ALTES_PASSWORT>
```

**Aendere zu:**
```bash
POSTGRES_PASSWORD_PROD=<NEUES_PASSWORT>
```

Speichern: `Ctrl+O`, `Enter`, `Ctrl+X`

### 2.3 PostgreSQL-Container neu starten

```bash
# Container stoppen
docker-compose -f docker-compose.production.yml --profile prod stop postgres-prod

# Container starten (laedt neue .env Werte)
docker-compose -f docker-compose.production.yml --profile prod start postgres-prod

# Status prüfen
docker-compose -f docker-compose.production.yml --profile prod logs postgres-prod
```

### 2.4 Neues Passwort in PostgreSQL setzen

```bash
# In PostgreSQL Container einloggen
docker exec -it pvs-postgres-prod psql -U pvs_user -d postgres

# Passwort aendern
ALTER USER pvs_user WITH PASSWORD 'NEUES_PASSWORT_HIER';

# Verlassen
\q
```

**WICHTIG**: Verwende hier das NEUE Passwort, nicht das alte aus .env!

### 2.5 Verbindung mit neuem Passwort testen

```bash
# Test mit neuem Passwort
docker exec -it pvs-postgres-prod psql -U pvs_user -d pvs_prod -c "SELECT version();"
```

Sollte erfolgreich sein.

---

## Schritt 3: GitHub Secret aktualisieren

### 3.1 GitHub Secrets oeffnen

1. Gehe zu: https://github.com/bbajor/pvs/settings/secrets/actions
2. Finde das Secret: `PROD_DB_PASSWORD`
3. Klicke auf das **Stift-Icon** (Bearbeiten)

### 3.2 Neues Passwort eintragen

- **Value**: Füge das NEUE Passwort ein (aus Schritt 1)
- **Update secret** klicken

### 3.3 Verifikation

Prüfe dass alle Secrets vorhanden sind:
- ✅ PROD_DB_HOST
- ✅ PROD_DB_NAME
- ✅ PROD_DB_USER
- ✅ PROD_DB_PASSWORD (neu gesetzt!)

---

## Schritt 4: Application neu starten

Nach Passwort-Aenderung muss die Application mit neuem Passwort neu starten:

```bash
# Auf Server
cd /opt/pvs

# Application-Container neu starten
docker-compose -f docker-compose.production.yml --profile prod restart pvs-prod

# Logs prüfen
docker-compose -f docker-compose.production.yml --profile prod logs -f pvs-prod
```

Die Application sollte sich erfolgreich mit der neuen Datenbank verbinden.

---

## Schritt 5: Alle Environments (optional)

Falls du auch DEV und TEST umstellen willst:

### DEV Environment

```bash
# .env Datei aendern
POSTGRES_PASSWORD_DEV=<NEUES_DEV_PASSWORT>

# Container neu starten
docker-compose -f docker-compose.production.yml --profile dev restart postgres-dev

# Passwort in PostgreSQL setzen
docker exec -it pvs-postgres-dev psql -U pvs_user -d postgres -c "ALTER USER pvs_user WITH PASSWORD 'NEUES_DEV_PASSWORT';"
```

### TEST Environment

```bash
# .env Datei aendern
POSTGRES_PASSWORD_TEST=<NEUES_TEST_PASSWORT>

# Container neu starten
docker-compose -f docker-compose.production.yml --profile test restart postgres-test

# Passwort in PostgreSQL setzen
docker exec -it pvs-postgres-test psql -U pvs_user -d postgres -c "ALTER USER pvs_user WITH PASSWORD 'NEUES_TEST_PASSWORT';"
```

---

## Schritt 6: Verifikation

### 6.1 Datenbank-Verbindung testen

```bash
# Von Server aus
docker exec -it pvs-postgres-prod psql -U pvs_user -d pvs_prod -c "SELECT current_database(), current_user;"
```

**Erwartete Ausgabe:**
```
 current_database | current_user
------------------+--------------
 pvs_prod         | pvs_user
```

### 6.2 Application Health Check

```bash
# Health Endpoint prüfen
curl http://localhost:8080/actuator/health

# Sollte {"status":"UP"} zurückgeben
```

### 6.3 GitHub Actions testen

1. Gehe zu: https://github.com/bbajor/pvs/actions
2. Starte ein Deployment
3. Prüfe die Logs - sollte keine Datenbank-Verbindungsfehler geben

---

## Troubleshooting

### "password authentication failed"

**Ursache**: Passwort nicht korrekt in beiden Stellen gesetzt

**Lösung**:
1. Prüfe `.env` Datei auf Server
2. Prüfe GitHub Secret
3. Prüfe ob PostgreSQL-Container neu gestartet wurde
4. Prüfe ob `ALTER USER` erfolgreich war

### "connection refused"

**Ursache**: PostgreSQL-Container läuft nicht

**Lösung**:
```bash
docker-compose -f docker-compose.production.yml --profile prod ps
docker-compose -f docker-compose.production.yml --profile prod start postgres-prod
```

### Application startet nicht

**Ursache**: Falsches Passwort in GitHub Secrets oder Environment

**Lösung**:
1. Prüfe GitHub Actions Logs
2. Prüfe Application-Logs auf Server
3. Stelle sicher dass GitHub Secret korrekt ist

---

## Sicherheitshinweise

- ✅ **Neues Passwort muss sicher sein**: Mindestens 32 Zeichen, zufällig generiert
- ✅ **Niemals Passwörter committen** (stehen bereits in `.gitignore`)
- ✅ **Backup vor Aenderung**: `.env.backup` erstellen
- ✅ **Altes Passwort dokumentieren**: Falls Rollback nötig ist
- ✅ **Alle Environments rotieren**: Nicht nur Prod, auch Dev/Test sollten sichere Passwörter haben

---

## Checkliste

- [ ] Neues Passwort generiert (32+ Zeichen)
- [ ] Passwort sicher gespeichert (Passwort-Manager)
- [ ] Backup von `.env` erstellt
- [ ] `.env` Datei auf Server aktualisiert
- [ ] PostgreSQL-Container neu gestartet
- [ ] `ALTER USER` in PostgreSQL ausgeführt
- [ ] Verbindung mit neuem Passwort getestet
- [ ] GitHub Secret aktualisiert
- [ ] Application neu gestartet
- [ ] Health Check erfolgreich
- [ ] GitHub Actions Deployment getestet

---

## Wiederholbarkeit

Falls du später nochmal ein Passwort aendern musst:

1. Neues Passwort generieren: `openssl rand -base64 32`
2. `.env` auf Server aendern
3. Container neu starten
4. `ALTER USER` in PostgreSQL
5. GitHub Secret aktualisieren
6. Application neu starten
7. Testen

---

**Siehe auch:**
- [`docs/deployment/HETZNER_COMPLETE_SETUP.md`](../deployment/HETZNER_COMPLETE_SETUP.md) - Initial Setup
- [`docs/security/SSH_KEY_SETUP.md`](SSH_KEY_SETUP.md) - SSH-Zugriff einrichten

