# Logging & Monitoring (Agent 5)

**Structured Logging, Security Events, Prometheus-Metrics**

---

## 🎯 Implementierung

### 1. Structured Logging (Logback + JSON)

**logback-spring.xml:**
- **Dev/Test:** Readable Console-Output
- **Production:** JSON-Format (Logstash-Encoder)
- Rolling-File-Appender (10MB/File, 30 Tage Retention)

**JSON-Fields:**
- timestamp, level, logger, message
- MDC: traceId, spanId, userId, username, ipAddress
- Exception-Stacktraces

### 2. Security Event Logger

**SecurityEventLogger.java:**
- Loggt Security-Events als JSON
- Separates Log-File: `security-events.log`
- Event-Types:
  - LOGIN_SUCCESS, LOGIN_FAILED
  - MFA_VERIFICATION_SUCCESS/FAILED
  - RATE_LIMIT_VIOLATION
  - ACCOUNT_LOCKOUT
  - PERMISSION_DENIED

**Usage:**
```java
@Autowired
private SecurityEventLogger securityLogger;

securityLogger.logLoginSuccess(userId, username, ipAddress, userAgent);
securityLogger.logLoginFailed(username, ipAddress, userAgent, "Invalid password");
```

### 3. Prometheus Metrics

**Dependency:** `micrometer-registry-prometheus`

**Endpoint:** `/actuator/prometheus`

**Metrics:**
- Standard: JVM, HTTP, DB-Pool
- Custom: Security-Metrics (TODO: implementieren)

---

## 🚀 Usage

### Production Logging

```bash
# Starte mit Production-Profil
docker-compose -f docker-compose.production.yml --profile prod up -d

# Logs anzeigen (JSON)
docker logs pvs-prod

# Security-Events anzeigen
docker exec pvs-prod cat /tmp/security-events.log | jq
```

### Prometheus-Metrics

```bash
# Metrics abrufen
curl http://localhost:8080/actuator/prometheus

# Prometheus-Config (siehe docker/monitoring/)
# - Scrape-Interval: 15s
# - Targets: pvs-app:8080
```

---

## 📚 Siehe auch

- [Backup & Disaster Recovery](../deployment/BACKUP_DISASTER_RECOVERY.md)

---

**Erstellt:** 2025-10-30  
**Version:** 1.0 (Agent 5)
