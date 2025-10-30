# Branch-Strategie für Deployment

## Übersicht

Die Deployment-Änderungen wurden in `master` integriert und sollten von dort aus weiterentwickelt werden.

## Aktueller Stand

✅ **master**: Enthält alle Deployment-Änderungen
- Cloud Deployment Setup
- Multi-Stage Security (Prod-Security für Test/Prod)
- Flyway Integration
- GitHub Actions CI/CD
- Render.com Configuration

## Workflow für neue Features

### 1. Feature-Branch erstellen

```bash
git checkout master
git pull origin master
git checkout -b feature/mein-feature
```

### 2. Änderungen committen

```bash
git add .
git commit -m "feat: Beschreibung"
```

### 3. Merge in master

```bash
git checkout master
git pull origin master
git merge feature/mein-feature --no-ff
git push origin master
```

## Deployment-Branches

- **master**: Production-Ready Code (für Prod-Deployment)
- **develop**: Development Branch (falls vorhanden, für Dev-Deployment)
- **feature/***: Feature Branches

## CI/CD Integration

- Push auf `master` → Automatisches Deployment zu **Test**
- Push auf `develop` → Automatisches Deployment zu **Dev**
- Production Deployment → Manuell via GitHub Actions

## Wichtige Hinweise

- **Keine Dev-Security in Test/Prod**: ProdSecurityConfig ist aktiv für Profile `test` und `prod`
- **Flyway Migrations**: Neue DB-Änderungen müssen als Migration erstellt werden
- **Environment Configs**: Profile-spezifische Änderungen in `application-{dev,test,prod}.yaml`

