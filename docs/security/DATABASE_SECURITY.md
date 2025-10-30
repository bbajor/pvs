# Database Security - PostgreSQL Hardening

**Production-Ready Database Security für PVS**

---

## 🎯 Überblick

Database Security umfasst:
- ✅ PostgreSQL SSL/TLS Connections
- ✅ Connection-Pooling (HikariCP)
- ✅ Network Isolation (keine Public Ports)
- ✅ Secrets Management (Environment-Variablen)
- ✅ Minimal Database-User-Permissions

---

## 🔒 Implementierte Maßnahmen

### 1. PostgreSQL SSL/TLS Connection

**application-prod.yaml:**
```yaml
datasource:
  url: ${DATABASE_URL}?ssl=true&sslmode=require
  username: ${DATABASE_USERNAME:}
  password: ${DATABASE_PASSWORD:}
```

**Vorteile:**
- Verschlüsselte DB-Verbindungen
- Man-in-the-Middle-Schutz
- Compliance-Anforderung (DSGVO)

**Testing:**
```bash
# SSL-Connection testen
psql "postgresql://user@host:5432/dbname?ssl=true&sslmode=require"
```

---

### 2. Connection-Pooling (HikariCP)

**Optimierte Konfiguration:**
```yaml
hikari:
  maximum-pool-size: 10       # Max 10 Connections
  minimum-idle: 5             # Min 5 Idle-Connections
  connection-timeout: 30000   # 30s Timeout
  idle-timeout: 600000        # 10min Idle
  max-lifetime: 1800000       # 30min Max-Lifetime
  connection-test-query: SELECT 1
  pool-name: PVS-Production-Pool
```

**Monitoring:**
```bash
# Pool-Metrics via Actuator
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# Expected: 5-10 Connections
```

---

### 3. Network Isolation

**docker-compose.production.yml:**
```yaml
postgres-prod:
  # KEINE Public Ports!
  # ports:
  #   - "5432:5432"  # DEAKTIVIERT
  networks:
    - pvs-network  # Nur intern
```

**Vorteile:**
- PostgreSQL nur für pvs-app erreichbar
- Kein externer Zugriff
- Schutz vor Brute-Force

**Testing:**
```bash
# Von außen: Connection refused
telnet localhost 5432
# Expected: Connection refused

# Von App: Connection OK
docker exec pvs-prod nc -zv postgres-prod 5432
# Expected: Connection succeeded
```

---

### 4. Secrets Management

**Environment-Variablen:**
```bash
# .env (NICHT in Git!)
DATABASE_URL=jdbc:postgresql://postgres-prod:5432/pvs_prod
DATABASE_USERNAME=pvs_user
DATABASE_PASSWORD=STRONG_PASSWORD_HERE
```

**Kein Hardcoding:**
```java
// ✅ RICHTIG
url: ${DATABASE_URL}

// ❌ FALSCH
url: jdbc:postgresql://localhost:5432/pvs_prod
```

---

## 🔧 Production-Deployment

### Setup

1. **Kopiere .env.production.example zu .env:**
```bash
cp .env.production.example .env
```

2. **Setze sichere Passwörter:**
```bash
# Generiere starkes Passwort
openssl rand -base64 32

# .env editieren
DATABASE_PASSWORD=<generiertes-passwort>
```

3. **Starte PostgreSQL:**
```bash
docker-compose -f docker-compose.production.yml --profile prod up -d postgres-prod
```

4. **Prüfe Connection:**
```bash
docker logs pvs-postgres-prod
docker exec pvs-prod psql -U pvs_user -d pvs_prod -c "SELECT 1"
```

---

## 🧪 Testing

### Connection-Test

```bash
# Connection mit SSL
docker exec pvs-prod psql "postgresql://pvs_user:password@postgres-prod:5432/pvs_prod?ssl=true&sslmode=require"

# Expected: Connected
```

### Pool-Monitoring

```bash
# HikariCP-Metrics
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.idle

# Expected: active: 5-10, idle: 0-5
```

---

## 📚 Dokumentation

**Siehe auch:**
- [Secrets Management](./SECRETS_MANAGEMENT.md)
- [Backup & Disaster Recovery](../deployment/BACKUP_DISASTER_RECOVERY.md)

---

**Erstellt:** 2025-10-30  
**Version:** 1.0  
**Status:** ✅ Production-Ready
