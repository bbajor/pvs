# Datenbank-Architektur Entscheidung

## Frage: Container vs. Native PostgreSQL?

## 📊 Vergleich

### Option 1: PostgreSQL als Container (EMPFOHLEN) ✅

**Vorteile:**
- ✅ **Isolation**: Dev/Test/Prod komplett getrennt
- ✅ **Einfaches Backup/Restore**: Volume-Snapshots
- ✅ **Portabel**: Gleiche Config überall
- ✅ **Einfaches Upgrade**: Container-Image wechseln
- ✅ **Wartbar**: Alles in docker-compose.yml
- ✅ **Konsistente Deployment-Pipeline**: CI/CD freundlich
- ✅ **Ressourcen-Limits**: Per Container konfigurierbar
- ✅ **Profiles**: Nur aktive Environment starten

**Nachteile:**
- ⚠️ Etwas weniger Performance als native Installation (aber vernachlässigbar bei richtigem Setup)
- ⚠️ Volume-Management nötig (aber einfach)

### Option 2: Native PostgreSQL auf Server ❌

**Vorteile:**
- ✅ Etwas bessere Performance (5-10%)
- ✅ Kann von mehreren Projekten genutzt werden

**Nachteile:**
- ❌ **Wartung auf Server-Level**: Updates komplizierter
- ❌ **Keine Isolation**: Alle Environments teilen sich PostgreSQL
- ❌ **Manuelle Backups**: Scripts selbst schreiben
- ❌ **Nicht portabel**: Server-spezifische Konfiguration
- ❌ **Mehr Fehlerquellen**: OS-Updates, Package-Management
- ❌ **Schwierigeres Rollback**: Keine einfachen Snapshots

## 🎯 Empfehlung: **Separate Container für Dev/Test/Prod**

### Warum?
1. **Sicherheit**: Keine Datenvermischung zwischen Environments
2. **Flexibilität**: Prod kann mehr Ressourcen bekommen
3. **Testbarkeit**: Test-DB kann einfach zurückgesetzt werden
4. **Wartbarkeit**: Alles in docker-compose.yml dokumentiert
5. **Skalierbarkeit**: Später einfach auf mehrere Server verteilen

### Konkrete Umsetzung

```yaml
# Separate Container pro Environment
postgres-dev:    Port 5433, Volume: postgres-data-dev
postgres-test:   Port 5434, Volume: postgres-data-test  
postgres-prod:   Port 5435, Volume: postgres-data-prod (4GB RAM, 2 CPUs)
```

### Resource-Limits für Prod:
- **Production**: 4GB RAM, 2 CPUs (Performance)
- **Test**: 1GB RAM, 0.5 CPUs (Sparsam)
- **Dev**: 1GB RAM, 0.5 CPUs (Sparsam)

## 📝 Migration-Plan

### Schritt 1: Environment-Variablen erweitern
```bash
# In .env Datei:
POSTGRES_DB_DEV=pvs_dev
POSTGRES_USER_DEV=pvs_user
POSTGRES_PASSWORD_DEV=<generiert>

POSTGRES_DB_TEST=pvs_test
POSTGRES_USER_TEST=pvs_user
POSTGRES_PASSWORD_TEST=<generiert>

POSTGRES_DB_PROD=pvs_prod
POSTGRES_USER_PROD=pvs_user
POSTGRES_PASSWORD_PROD=<generiert>
```

### Schritt 2: Container starten
```bash
# Nur Dev starten:
docker-compose --profile dev up -d postgres-dev

# Nur Test starten:
docker-compose --profile test up -d postgres-test

# Nur Prod starten:
docker-compose --profile prod up -d postgres-prod
```

### Schritt 3: Migrations ausführen
```bash
# Für jedes Environment separat
docker-compose --profile prod up -d pvs-prod
# Migrations laufen automatisch via Spring Boot
```

## 🔒 Sicherheit

- **Separate Passwörter** pro Environment
- **Isolierte Volumes** (keine Datenvermischung)
- **Profile-basiert**: Nur aktive Environments laufen
- **Port-Binding**: Nur lokal (127.0.0.1), nicht extern

## 📈 Performance

Für deine Anwendung (wahrscheinlich < 1000 tägliche Nutzer):
- **Container-Performance ist mehr als ausreichend**
- PostgreSQL in Container: ~95-98% der nativen Performance
- Durch separate Container: Bessere Ressourcen-Kontrolle

## ✅ Fazit

**Container-Lösung ist deutlich besser** für:
- ✅ Wartbarkeit
- ✅ Sicherheit (Isolation)
- ✅ Deployment-Einfachheit
- ✅ Skalierbarkeit
- ✅ Backup/Restore

Native Installation nur sinnvoll bei:
- Extrem hohem Performance-Bedarf (> 100k Nutzer)
- Shared Database für mehrere Projekte
- Legacy-Systemen ohne Container

