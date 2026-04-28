# Hetzner Load Balancer Konfiguration

## Übersicht

Der Hetzner Load Balancer verteilt den Traffic auf mehrere Application-Instanzen für High Availability und bessere Performance.

## Voraussetzungen

- Hetzner Cloud Account
- Mindestens 2 Application-Instanzen
- Domain mit DNS-Konfiguration

## Load Balancer erstellen

### 1. In Hetzner Console

1. **Load Balancer erstellen:**
   - Name: `pvs-loadbalancer`
   - Type: `LB11` (oder größer)
   - Location: Gleiche Location wie Server
   - Algorithm: `round_robin` (oder `least_connections`)

2. **Targets hinzufügen:**
   - Server 1: `10.0.0.1:8080`
   - Server 2: `10.0.0.2:8080`
   - Health Check: HTTP `GET /actuator/health`

3. **Services konfigurieren:**
   - HTTP: Port 80 → Targets
   - HTTPS: Port 443 → Targets (mit SSL-Zertifikat)

### 2. DNS-Konfiguration

```dns
# A-Record auf Load Balancer IP zeigen lassen
pvs.example.com.  IN  A  <load-balancer-ip>
```

## Health Checks

### Konfiguration

```yaml
# Hetzner Load Balancer Health Check
health_check:
  protocol: http
  port: 8080
  interval: 10s
  timeout: 5s
  retries: 3
  http:
    domain: ""
    path: /actuator/health
    response: '{"status":"UP"}'
    status_codes:
      - "2??"
      - "3??"
```

### Application Health Endpoint

Die Application muss den Health-Check-Endpoint bereitstellen:

```yaml
# application-cloud.yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
```

## Sticky Sessions (Session Affinity)

### Konfiguration

Für Session-basierte Anwendungen (Vaadin) ist Sticky Sessions wichtig:

```yaml
# Hetzner Load Balancer
services:
  - protocol: https
    port: 443
    destination_port: 8080
    health_check: ...
    http:
      sticky_sessions:
        cookie_name: JSESSIONID
        cookie_ttl: 1800  # 30 Minuten
```

### Alternative: Redis Session Storage

Statt Sticky Sessions kann Redis für Session-Sharing verwendet werden:

```yaml
# application-cloud.yaml
spring:
  session:
    store-type: redis
  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: ${REDIS_PORT:6379}
```

## SSL/TLS Zertifikat

### Let's Encrypt via Traefik

```yaml
# traefik.yml (auf jedem Server)
certificatesResolvers:
  letsencrypt:
    acme:
      email: admin@example.com
      storage: /letsencrypt/acme.json
      httpChallenge:
        entryPoint: web
```

### Oder: Hetzner Load Balancer SSL

1. **Zertifikat hochladen:**
   - Hetzner Console → Load Balancer → Certificates
   - Zertifikat und Private Key hochladen

2. **Service konfigurieren:**
   - HTTPS Service → Certificate auswählen

## Traffic-Verteilung

### Algorithmen

- **Round Robin:** Gleichmäßige Verteilung
- **Least Connections:** Wenigste aktive Verbindungen
- **Source IP:** Basierend auf Client-IP

### Empfehlung

Für Vaadin-Anwendungen: **Source IP** oder **Sticky Sessions** verwenden.

## Monitoring

### Load Balancer Metrics

```bash
# Via Hetzner API
curl -H "Authorization: Bearer $HETZNER_API_TOKEN" \
  https://api.hetzner.cloud/v1/load_balancers/$LB_ID/metrics
```

### Application Metrics

```bash
# Prometheus Metrics
curl http://localhost:8080/actuator/prometheus | grep http_server_requests
```

## Troubleshooting

### Health Check schlägt fehl

```bash
# Prüfe Health Endpoint direkt
curl http://<server-ip>:8080/actuator/health

# Prüfe Logs
podman logs pvs-prod | grep health
```

### Session-Probleme

```bash
# Prüfe Redis-Verbindung
podman exec redis redis-cli ping

# Prüfe Session-Storage
podman exec redis redis-cli keys "spring:session:*"
```

### Traffic-Verteilung ungleichmäßig

- Prüfe Health Checks (alle Targets healthy?)
- Prüfe Load Balancer Algorithm
- Prüfe Application-Performance

## Best Practices

1. **Mindestens 2 Instanzen:**
   - Für High Availability
   - Graceful Shutdown bei Updates

2. **Health Checks konfigurieren:**
   - Automatisches Failover bei Ausfall
   - Regelmäßige Überprüfung

3. **Monitoring:**
   - Load Balancer Metrics überwachen
   - Application Metrics sammeln

4. **SSL/TLS:**
   - Immer HTTPS verwenden
   - Automatische Zertifikats-Erneuerung

5. **Session-Management:**
   - Redis für Session-Sharing
   - Oder Sticky Sessions
