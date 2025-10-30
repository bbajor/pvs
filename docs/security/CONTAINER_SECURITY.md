# Container Security - Docker Best Practices

**Production-Ready Container Security für PVS**

---

## 🎯 Überblick

Container Security umfasst mehrere Layers:
- ✅ Non-Root User
- ✅ Minimal Base Images
- ✅ Multi-Stage Builds
- ✅ Security Scanning (Trivy)
- ✅ Image-Versionierung
- ✅ Network Security
- ✅ Resource Limits

---

## 🚀 Implementierte Security-Features

### 1. Non-Root User

**Dockerfile:**
```dockerfile
# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Set ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser
```

**Warum wichtig:**
- Container läuft NICHT als root
- Schutz vor Container-Escape
- Principle of Least Privilege
- Compliance-Anforderung

**Validierung:**
```bash
# Container-User prüfen
docker exec pvs-prod whoami
# Erwartete Ausgabe: appuser

# UID/GID prüfen
docker exec pvs-prod id
# Erwartete Ausgabe: uid=999(appuser) gid=999(appuser) groups=999(appuser)
```

---

### 2. Multi-Stage Builds

**Dockerfile:**
```dockerfile
# Stage 1: Build
FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle gradle.properties ./
RUN gradle dependencies --no-daemon || true
COPY src/ src/
RUN gradle bootJar --no-daemon -x test

# Stage 2: Production
FROM eclipse-temurin:21-jre-jammy
# Nur JAR wird kopiert, keine Build-Tools!
COPY --from=build /app/build/libs/*.jar app.jar
```

**Vorteile:**
- Build-Tools NICHT in Production-Image
- Kleinere Image-Größe
- Weniger Attack Surface
- Schnellere Deployments

**Image-Größe vergleichen:**
```bash
# Multi-Stage Build (klein)
docker images pvs-app:prod
# ca. 300-400 MB

# Single-Stage Build (groß)
# ca. 1-2 GB
```

---

### 3. Minimal Base Images

**Aktuell:** `eclipse-temurin:21-jre-jammy` (Ubuntu-basiert)

**Alternativen für noch kleinere Images:**

#### Alpine Linux
```dockerfile
FROM eclipse-temurin:21-jre-alpine
```
**Vorteile:**
- Sehr klein (ca. 150 MB)
- Weniger Vulnerabilities

**Nachteile:**
- musl libc statt glibc (Kompatibilitätsprobleme möglich)

#### Distroless (Google)
```dockerfile
FROM gcr.io/distroless/java21-debian12
```
**Vorteile:**
- Minimal (nur Runtime)
- KEINE Shell, Package Manager, etc.
- Sicherste Option

**Nachteile:**
- Debugging schwieriger
- Keine Shell für docker exec

**Empfehlung für PVS:** Bleibe bei `eclipse-temurin:21-jre-jammy` (gute Balance)

---

### 4. Security Scanning mit Trivy

**GitHub Actions Workflow:** `.github/workflows/trivy-scan.yml`

```yaml
- name: Run Trivy vulnerability scanner
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: 'pvs-app:scan'
    format: 'sarif'
    severity: 'CRITICAL,HIGH'
```

**Funktionen:**
- **Automatische Scans:** Bei jedem Build
- **Vulnerability-Datenbank:** CVE-Tracking
- **SARIF-Report:** GitHub Security Tab
- **Exit-Code:** Build fails bei Critical/High (optional)

**Lokales Scanning:**
```bash
# Trivy installieren (einmalig)
# https://github.com/aquasecurity/trivy#installation

# Image scannen
trivy image pvs-app:latest

# Nur Critical/High
trivy image --severity CRITICAL,HIGH pvs-app:latest

# JSON-Report
trivy image -f json -o trivy-report.json pvs-app:latest
```

**Erwartete Ausgabe:**
```
pvs-app:latest (debian 12.2)
Total: 0 (CRITICAL: 0, HIGH: 0)
```

---

### 5. Image-Versionierung

**Best Practice:**
```bash
# Semantic Versioning
docker tag pvs-app:latest pvs-app:1.0.0

# Git Commit Hash
docker tag pvs-app:latest pvs-app:$(git rev-parse --short HEAD)

# Date-based
docker tag pvs-app:latest pvs-app:$(date +%Y%m%d)
```

**GitHub Actions:**
```yaml
- name: Build and Push Docker Image
  uses: docker/build-push-action@v5
  with:
    tags: |
      ghcr.io/bbajor/pvs:${{ github.sha }}
      ghcr.io/bbajor/pvs:latest
      ghcr.io/bbajor/pvs:prod-latest
```

**Warum wichtig:**
- Reproduzierbarkeit
- Rollback möglich
- Audit-Trail
- Keine "latest"-Überraschungen in Prod

---

### 6. Network Security

**docker-compose.production.yml:**
```yaml
networks:
  pvs-network:
    driver: bridge
    internal: false  # External access via Traefik only
    ipam:
      config:
        - subnet: 172.28.0.0/16
```

**Security-Features:**
- **Isoliertes Netzwerk:** Services nur untereinander erreichbar
- **PostgreSQL KEINE Public Ports:** Nur intern (Traefik → App → DB)
- **Traefik als einziger Einstiegspunkt:** Port 80/443
- **No Container → Internet (optional):** internal: true

**Port-Exposition:**
```yaml
# ✅ RICHTIG: Nur localhost
ports:
  - "127.0.0.1:5433:5432"

# ❌ FALSCH: Public Port
ports:
  - "5432:5432"

# ✅ OPTIMAL: Keine Ports (nur intern)
# ports: []  # PostgreSQL nur für App erreichbar
```

---

### 7. Resource Limits

**docker-compose.production.yml:**
```yaml
pvs-prod:
  deploy:
    resources:
      limits:
        cpus: '1.5'
        memory: 2G
      reservations:
        cpus: '0.5'
        memory: 512M
```

**Vorteile:**
- Schutz vor Resource-Exhaustion
- DoS-Prevention
- Bessere Resource-Planung
- Container-Isolation

**Monitoring:**
```bash
# Resource-Usage anzeigen
docker stats pvs-prod

# Expected:
# CONTAINER   CPU %   MEM USAGE / LIMIT   MEM %   NET I/O
# pvs-prod    15%     512MB / 2GB         25%     1MB / 2MB
```

---

## 🔒 Security-Best-Practices-Checkliste

### Build-Time Security
- [x] Non-Root User
- [x] Multi-Stage Builds
- [x] Minimal Base Image
- [x] .dockerignore vorhanden
- [x] No Secrets in Image
- [x] Security Scanning (Trivy)
- [x] Image-Versionierung

### Runtime Security
- [x] Read-Only Filesystem (optional)
- [x] No Privileged Mode
- [x] No Host-Network
- [x] Resource Limits
- [x] Network Isolation
- [x] Health Checks
- [x] Security Options (no-new-privileges)

### Registry Security
- [x] Private Registry (ghcr.io)
- [x] Image-Signierung (optional)
- [x] Vulnerability-Scanning
- [x] Access-Control

---

## 🧪 Testing

### Security-Scan

```bash
# Trivy-Scan
trivy image --severity CRITICAL,HIGH pvs-app:latest

# Docker Bench Security
docker run -it --net host --pid host --cap-add audit_control \
  -v /var/lib:/var/lib \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /etc:/etc --label docker_bench_security \
  docker/docker-bench-security

# Expected: PASS auf allen Security-Checks
```

### Non-Root User Validation

```bash
# User prüfen
docker exec pvs-prod whoami
# Expected: appuser

# File-Permissions prüfen
docker exec pvs-prod ls -la /app
# Expected: appuser:appuser

# Root-Zugriff versuchen (sollte fehlschlagen)
docker exec --user root pvs-prod whoami
# Expected: Error (kein root-Zugriff)
```

### Network Isolation Test

```bash
# PostgreSQL von außen erreichbar? (sollte NICHT sein)
telnet localhost 5435
# Expected: Connection refused

# PostgreSQL von App erreichbar? (sollte sein)
docker exec pvs-prod nc -zv postgres-prod 5432
# Expected: Connection succeeded
```

---

## 🔧 Hardening-Optionen (Advanced)

### Read-Only Filesystem

```yaml
pvs-prod:
  read_only: true
  tmpfs:
    - /tmp
    - /app/tmp
```

**Vorteil:** Keine Datei-Änderungen im Container (Immutable Infrastructure)

### Security Options

```yaml
pvs-prod:
  security_opt:
    - no-new-privileges:true
    - seccomp:default
    - apparmor:default
```

**Vorteil:** Additional Security-Layer (Kernel-Level)

### Capabilities Drop

```yaml
pvs-prod:
  cap_drop:
    - ALL
  cap_add:
    - NET_BIND_SERVICE  # Nur für Port < 1024
```

**Vorteil:** Minimale Capabilities (Principle of Least Privilege)

---

## 🚨 Troubleshooting

### Problem: Permission Denied

**Symptom:** Container startet nicht, "Permission denied" Error

**Lösung:**
```bash
# File-Permissions prüfen
docker exec pvs-prod ls -la /app

# Ownership korrigieren (im Dockerfile)
RUN chown -R appuser:appuser /app
```

### Problem: Image-Scan zeigt Vulnerabilities

**Symptom:** Trivy findet Critical/High Vulnerabilities

**Lösung:**
1. **Base-Image updaten:**
```dockerfile
# Alte Version
FROM eclipse-temurin:21-jre-jammy

# Neueste Version
FROM eclipse-temurin:21-jre-jammy-latest
```

2. **Dependencies updaten:**
```bash
./gradlew dependencyUpdates
```

3. **Vulnerability ignorieren (wenn False-Positive):**
```yaml
# .trivyignore
CVE-2024-12345  # False positive, nicht betroffen
```

### Problem: Container-Performance

**Symptom:** Langsamer Start, hohe CPU

**Lösung:**
1. **Resource-Limits erhöhen:**
```yaml
resources:
  limits:
    cpus: '2.0'
    memory: 4G
```

2. **JVM-Tuning:**
```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

---

## 📚 Weiterführende Docs

- [Reverse Proxy Setup](../deployment/REVERSE_PROXY_SETUP.md) - Traefik-Integration
- [TLS Setup](./TLS_SETUP.md) - HTTPS-Konfiguration
- [Security Headers](./SECURITY_HEADERS.md) - OWASP-Compliance

---

## 📖 Externe Ressourcen

- [Docker Security Best Practices](https://docs.docker.com/develop/security-best-practices/)
- [CIS Docker Benchmark](https://www.cisecurity.org/benchmark/docker)
- [Trivy Docs](https://trivy.dev/)
- [OWASP Docker Security](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html)

---

**Erstellt:** 2025-10-30  
**Version:** 1.0  
**Status:** ✅ Production-Ready
