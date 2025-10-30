# Nächste Schritte - Deployment Setup

Nachdem Render Account erstellt und GitHub verbunden wurde, folge diesen Schritten:

## 1. Render API Key erstellen

1. Render Dashboard → Account Settings (oben rechts)
2. "API Keys" → "Create API Key"
3. Name: z.B. "GitHub Actions"
4. **WICHTIG**: Copy den Key sofort (wird nur einmal angezeigt)

## 2. GitHub Secrets konfigurieren

Gehe zu GitHub Repository → Settings → Secrets and variables → Actions → "New repository secret"

Folgende Secrets erstellen:

### Erforderliche Secrets:

**`RENDER_API_KEY`**
- Wert: Dein Render API Key

**Hinweis**: Service IDs werden nach dem ersten Setup in Render Dashboard angezeigt!

## 3. Render Services via Blueprint erstellen

1. Render Dashboard → "New" → **"Blueprint"**
2. Repository auswählen (dein GitHub Repo)
3. Render erkennt automatisch `render.yaml`
4. Klicke "Apply"

Render erstellt automatisch:
- ✅ pvs-dev (Free Tier)
- ✅ pvs-test (Starter Plan + DB)
- ✅ pvs-prod (Starter Plan + DB, manual deploy)
- ✅ pvs-test-db (PostgreSQL)
- ✅ pvs-prod-db (PostgreSQL)

## 4. Service IDs nach Setup abrufen

Nach dem Blueprint-Setup:

1. Render Dashboard → Service (z.B. `pvs-dev`)
2. In der URL siehst du: `https://dashboard.render.com/web/xxx-xxx-xxx-xxx`
   - Die `xxx-xxx-xxx-xxx` ist die Service ID
3. Oder: Settings → Scroll nach unten → "Service ID" ist dort

**Füge diese zu GitHub Secrets hinzu:**

- `RENDER_DEV_SERVICE_ID`
- `RENDER_TEST_SERVICE_ID`
- `RENDER_PROD_SERVICE_ID`

## 5. URLs nach Setup abrufen

Nach dem ersten Deployment werden Services verfügbar:

1. Render Dashboard → Service
2. URL ist oben sichtbar (z.B. `https://pvs-dev.onrender.com`)

**Füge zu GitHub Secrets hinzu:**

- `RENDER_TEST_URL` (z.B. `pvs-test.onrender.com`)
- `RENDER_PROD_URL` (z.B. `pvs-prod.onrender.com`)

## 6. Erste Deployment testen

### Dev Environment testen:

```bash
# Erstelle develop Branch (falls nicht vorhanden)
git checkout -b develop
git push origin develop
```

GitHub Actions startet automatisch:
- Build & Test
- Deployment zu Render Dev

**Prüfe Status:**
- GitHub Actions: Repository → Actions
- Render Dashboard: Service → "Events" Tab

### Erwartetes Ergebnis:

✅ Dev-Service läuft auf `https://pvs-dev.onrender.com`
✅ Health Check: `https://pvs-dev.onrender.com/actuator/health`

## 7. Database Migrations prüfen

Nach erstem Deployment zu Test/Prod:

1. Öffne: `https://pvs-test.onrender.com/actuator/flyway`
2. Prüfe: Migrations wurden ausgeführt
3. Status sollte "Success" zeigen

## 8. Optional: Test-Deployment prüfen

```bash
# Merge develop → main
git checkout main
git merge develop
git push origin main
```

GitHub Actions startet automatisch:
- Build & Test
- Flyway Validation
- Deployment zu Render Test

## Troubleshooting

### "Service not found" Fehler

- Service IDs noch nicht zu GitHub Secrets hinzugefügt
- Falsche Service ID kopiert
- Lösung: Prüfe Service ID in Render Dashboard

### "API Key invalid"

- API Key falsch kopiert (mit Leerzeichen?)
- API Key nicht zu GitHub Secrets hinzugefügt
- Lösung: Erstelle neuen API Key

### Database Connection Error

- PostgreSQL Service noch nicht bereit
- DATABASE_URL Environment Variable fehlt
- Lösung: Prüfe Render Dashboard → Service → Environment

### Health Check schlägt fehl

- App startet nicht (siehe Logs in Render Dashboard)
- Port-Konfiguration falsch
- Lösung: Prüfe Logs → "Events" Tab

## Fertig! 🎉

Sobald alle Secrets konfiguriert sind:
- ✅ Dev läuft automatisch bei Push auf `develop`
- ✅ Test läuft automatisch bei Push auf `main`
- ✅ Prod kann manuell via GitHub Actions deployed werden

Für Production Deployment siehe: [README.md](README.md#production-deployment)

