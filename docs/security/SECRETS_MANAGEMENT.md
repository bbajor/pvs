# Secrets Management

**Best Practices für sichere Credentials-Verwaltung**

---

## 🎯 Überblick

Secrets Management umfasst:
- ✅ Environment-Variablen statt Hardcoding
- ✅ `.env.example` Templates
- ✅ `.gitignore` für Secrets
- ✅ Secrets-Rotation-Strategie
- ⏳ Optional: HashiCorp Vault (später)

---

## 🔒 Implementierte Maßnahmen

### 1. Environment-Variablen

**application-prod.yaml:**
```yaml
datasource:
  url: ${DATABASE_URL}
  username: ${DATABASE_USERNAME:}
  password: ${DATABASE_PASSWORD:}

ai:
  whisper:
    remote:
      api-key: ${AI_API_KEY:}
```

**Vorteile:**
- Keine Secrets im Code
- Environment-spezifische Configs
- 12-Factor-App-Compliance

---

### 2. .env.production.example

**Template für Production:**
```bash
# Domain & SSL
DOMAIN=pvs.example.com
LETSENCRYPT_EMAIL=admin@example.com

# PostgreSQL Production
DATABASE_URL=jdbc:postgresql://postgres-prod:5432/pvs_prod
DATABASE_USERNAME=pvs_user
DATABASE_PASSWORD=CHANGE_ME_STRONG_PASSWORD_HERE

# AI Services
AI_API_KEY=
```

**Usage:**
```bash
# Kopieren und editieren
cp .env.production.example .env
nano .env

# In docker-compose.production.yml wird .env automatisch geladen
```

---

### 3. .gitignore

**Secrets niemals committen:**
```gitignore
# Environment-Variables
.env
.env.local
.env.production
.env.*.local

# Zertifikate
*.key
*.pem
*.p12
*.pfx

# Backups mit potentiell sensiblen Daten
*.sql
*.dump
backups/

# Traefik-Zertifikate
letsencrypt/acme.json
```

---

### 4. Secrets-Rotation

**Strategie:**
1. Passwörter alle 90 Tage rotieren
2. API-Keys bei Verdacht auf Leak sofort rotieren
3. TLS-Zertifikate automatisch via Let's Encrypt

**Prozess:**
```bash
# 1. Neues Passwort generieren
openssl rand -base64 32

# 2. In .env aktualisieren
nano .env

# 3. Applikation neustarten
docker-compose -f docker-compose.production.yml --profile prod restart pvs-prod

# 4. Alte Credentials deaktivieren
docker exec postgres-prod psql -U postgres -c "ALTER USER pvs_user PASSWORD 'new_password'"
```

---

## 🚨 Was NICHT tun

### ❌ Hardcoded Credentials

```java
// ❌ FALSCH
String password = "123456";
String apiKey = "sk-abc123def456";
```

### ❌ Credentials in Git

```bash
# ❌ FALSCH
git add .env
git commit -m "Add production credentials"
```

### ❌ Credentials in Logs

```java
// ❌ FALSCH
log.info("Database password: {}", password);

// ✅ RICHTIG
log.info("Database connection established");
```

---

## 🔧 Optional: HashiCorp Vault

**Für spätere Implementierung:**

```gradle
// build.gradle
implementation 'org.springframework.cloud:spring-cloud-starter-vault-config'
```

```yaml
# application.yaml
spring:
  cloud:
    vault:
      uri: https://vault.example.com
      authentication: TOKEN
      token: ${VAULT_TOKEN}
```

**Vorteile:**
- Zentrale Secrets-Verwaltung
- Automatische Rotation
- Audit-Logs
- Fine-grained Access-Control

---

## 📚 Dokumentation

**Siehe auch:**
- [Database Security](./DATABASE_SECURITY.md)
- [12-Factor App: Config](https://12factor.net/config)
- [HashiCorp Vault](https://www.vaultproject.io/)

---

**Erstellt:** 2025-10-30  
**Version:** 1.0  
**Status:** ✅ Production-Ready
