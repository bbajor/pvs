# Alternativen zu Render.com

## Kostenvergleich für Multi-Stage Deployment

### Option 1: Railway.app ⭐ **EMPFOHLEN**

**Kosten:**
- **Dev**: Free Tier (5$ Credit/Monat) - reicht für kleine Services
- **Test**: Hobby Plan (5$/Monat)
- **Prod**: Starter Plan (20$/Monat)
- **PostgreSQL**: Starter (5$/Monat für test/prod)
- **Gesamt: ~30$/Monat (~27€)**

**Vorteile:**
✅ Kostenlose Build-Pipelines
✅ GitHub Integration (automatische Deployments)
✅ EU-Server verfügbar
✅ Docker-Support
✅ PostgreSQL included
✅ Auto-Deploy aus Git

**Nachteile:**
⚠️ Free Tier nur 5$ Credit (reicht für Dev)
⚠️ Setup etwas weniger dokumentiert als Render

---

### Option 2: Fly.io

**Kosten:**
- **Dev**: Free Tier (256MB RAM, Shared CPU)
- **Test**: Shared (3$/Monat pro Service)
- **Prod**: Dedicated (6-12$/Monat pro Service)
- **PostgreSQL**: 5$/Monat (shared) - 15$/Monat (dedicated)
- **Gesamt: ~20-35$/Monat (~18-32€)**

**Vorteile:**
✅ Sehr günstig
✅ Globale Edge-Deployments
✅ Docker-native
✅ PostgreSQL Support
✅ Kostenlose Builds

**Nachteile:**
⚠️ CLI-basiertes Setup (weniger UI)
⚠️ Weniger Enterprise-Features

---

### Option 3: DigitalOcean App Platform

**Kosten:**
- **Dev**: Basic (5$/Monat)
- **Test**: Basic (5$/Monat)
- **Prod**: Professional (12$/Monat)
- **PostgreSQL**: Managed DB (15$/Monat)
- **Gesamt: ~37$/Monat (~34€)**

**Vorteile:**
✅ Sehr transparente Preise
✅ Bekannte Plattform
✅ Managed PostgreSQL
✅ EU-Server (Frankfurt)
✅ Automatische Deployments

**Nachteile:**
⚠️ Etwas teurer als Railway/Fly.io
⚠️ Weniger kostenlose Optionen

---

### Option 4: Hetzner Cloud + GitHub Actions (Self-Hosted) 🔥 **GÜNSTIGSTE**

**Kosten:**
- **VPS (CX21)**: 5€/Monat (2 vCPU, 4GB RAM)
- **PostgreSQL**: Included (auf VPS)
- **GitHub Actions**: Kostenlos (für Public Repos)
- **Gesamt: ~5€/Monat** (für alle 3 Stages!)

**Setup:**
- Docker Compose auf einem VPS
- GitHub Actions für CI/CD
- Nginx als Reverse Proxy

**Vorteile:**
✅ Sehr günstig (5€/Monat für alles!)
✅ Volle Kontrolle
✅ EU-Server (Nürnberg, Falkenstein)
✅ Keine Vendor-Lock-in
✅ DSGVO-konform

**Nachteile:**
⚠️ Mehr Setup-Aufwand
⚠️ Selbst-Management (Updates, Backups)
⚠️ Single-Point-of-Failure (ein Server)

---

### Option 5: Coolify (Self-Hosted auf Hetzner)

**Kosten:**
- **VPS (CPX21)**: 5€/Monat
- **PostgreSQL**: Included
- **Coolify**: Open Source (kostenlos)
- **Gesamt: ~5€/Monat**

**Vorteile:**
✅ Render-ähnliche UI
✅ Open Source
✅ GitHub Integration
✅ Docker Support
✅ PostgreSQL included

**Nachteile:**
⚠️ Self-Hosted (mehr Wartung)
⚠️ Community-Support

---

## Empfehlung

### Für Budget-Optimierung (<10€/Monat):
**Hetzner Cloud + GitHub Actions** (Option 4)
- 5€/Monat für alles
- Volle Kontrolle
- Mehr Setup, aber beste Kosten-Performance

### Für Einfachheit + Preis (~30€/Monat):
**Railway.app** (Option 1)
- Automatische Deployments
- Managed PostgreSQL
- Wenig Konfiguration
- 30$/Monat für alle Stages

### Für Enterprise-features:
**DigitalOcean App Platform** (Option 3)
- Transparente Preise
- Managed Services
- 37$/Monat

## Migration von Render zu Railway

1. **Railway Account erstellen**
2. **render.yaml → railway.toml konvertieren**
3. **GitHub Integration aktivieren**
4. **Environment Variables übertragen**
5. **Deploy testen**

Soll ich dir die Railway- oder Hetzner-Konfiguration erstellen?

