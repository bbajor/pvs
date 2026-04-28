# High Availability - Mehrere Instanzen & Graceful Shutdown

## Übersicht

High Availability (HA) ermöglicht es, die Anwendung mit mehreren Instanzen zu betreiben, um Ausfälle zu vermeiden und die Last zu verteilen.

## Architektur

```
                    ┌─────────────────┐
                    │  Load Balancer  │
                    │   (Hetzner LB)  │
                    └────────┬────────┘
                             │
                ┌────────────┼────────────┐
                │                        │
        ┌───────▼────────┐      ┌───────▼────────┐
        │  PVS Instance 1│      │  PVS Instance 2│
        │  (Server 1)    │      │  (Server 2)    │
        └───────┬────────┘      └───────┬────────┘
                │                        │
                └────────────┬───────────┘
                             │
                    ┌────────▼────────┐
                    │  PostgreSQL     │
                    │  (Managed DB)   │
                    └─────────────────┘
                             │
                    ┌────────▼────────┐
                    │  Redis          │
                    │  (Session Store)│
                    └─────────────────┘
```

## Voraussetzungen

- Load Balancer (Hetzner LB)
- Mindestens 2 Application-Server
- Shared Session Storage (Redis)
- Shared Database (PostgreSQL)

## Graceful Shutdown

### Konfiguration

```yaml
# application-cloud.yaml
server:
  shutdown: graceful
  # Graceful Shutdown Timeout (Standard: 30s)
  # Load Balancer sollte Health Checks während Shutdown berücksichtigen
```

### Verhalten

1. **Shutdown-Signal empfangen:**
   - Spring Boot stoppt neue Requests
   - Laufende Requests werden abgeschlossen
   - Health Check antwortet mit "DOWN"

2. **Load Balancer reagiert:**
   - Health Check erkennt "DOWN"
   - Traffic wird auf andere Instanzen umgeleitet
   - Instanz wird aus Rotation entfernt

3. **Container stoppt:**
   - Nach Timeout oder alle Requests abgeschlossen
   - Container wird beendet

### Timeout-Konfiguration

```yaml
# application-cloud.yaml
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

## Session-Sharing (Redis)

### Konfiguration

```yaml
# application-cloud.yaml
spring:
  session:
    store-type: redis
    redis:
      namespace: spring:session:pvs
  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

### Vorteile

- Sessions überleben Instanz-Ausfälle
- Benutzer bleiben eingeloggt bei Failover
- Keine Sticky Sessions nötig

## Deployment-Strategie

### Rolling Update

```bash
# 1. Neue Version auf Instance 1 deployen
./scripts/deployment/deploy-hetzner.sh prod <new-tag> instance1

# 2. Warten auf Health Check
sleep 60
curl http://instance1:8080/actuator/health

# 3. Neue Version auf Instance 2 deployen
./scripts/deployment/deploy-hetzner.sh prod <new-tag> instance2

# 4. Warten auf Health Check
sleep 60
curl http://instance2:8080/actuator/health
```

### Blue-Green Deployment

```bash
# 1. Neue Instanzen starten (Green)
podman-compose -f podman-compose.production.yml --profile prod-green up -d

# 2. Health Checks prüfen
curl http://green-instance:8080/actuator/health

# 3. Load Balancer auf Green umschalten
# (via Hetzner Console oder API)

# 4. Alte Instanzen stoppen (Blue)
podman-compose -f podman-compose.production.yml --profile prod-blue down
```

## Health Checks

### Load Balancer Health Check

```yaml
# Hetzner Load Balancer
health_check:
  protocol: http
  port: 8080
  interval: 10s
  timeout: 5s
  retries: 3
  http:
    path: /actuator/health
    status_codes:
      - "2??"
      - "3??"
```

### Application Health Endpoint

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

## Monitoring

### Instanz-Status

```bash
# Health Check direkt prüfen
curl http://instance1:8080/actuator/health
curl http://instance2:8080/actuator/health

# Prometheus Metrics
curl http://instance1:8080/actuator/prometheus | grep http_server_requests
```

### Load Balancer Status

```bash
# Via Hetzner API
curl -H "Authorization: Bearer $HETZNER_API_TOKEN" \
  https://api.hetzner.cloud/v1/load_balancers/$LB_ID
```

## Failover-Szenarien

### Instanz-Ausfall

1. Health Check schlägt fehl
2. Load Balancer entfernt Instanz aus Rotation
3. Traffic wird auf andere Instanzen umgeleitet
4. Sessions bleiben erhalten (Redis)
5. Benutzer merken keinen Unterschied

### Datenbank-Ausfall

1. Application erkennt DB-Verbindungsfehler
2. Health Check antwortet mit "DOWN"
3. Load Balancer stoppt Traffic
4. Admin wird benachrichtigt
5. Failover zu Backup-DB (wenn konfiguriert)

### Redis-Ausfall

1. Sessions werden nicht mehr gespeichert
2. Benutzer müssen sich neu einloggen
3. Application läuft weiter (nur Session-Problem)
4. Redis sollte redundant sein (Master-Slave)

## Best Practices

1. **Mindestens 2 Instanzen:**
   - Für echte HA
   - In verschiedenen Availability Zones (wenn möglich)

2. **Graceful Shutdown:**
   - Immer aktiviert
   - Timeout angemessen setzen

3. **Health Checks:**
   - Regelmäßig prüfen
   - Automatisches Failover

4. **Session-Sharing:**
   - Redis für Session-Storage
   - Keine Sticky Sessions nötig

5. **Monitoring:**
   - Instanz-Status überwachen
   - Load Balancer Metrics sammeln

6. **Rolling Updates:**
   - Schrittweise Deployment
   - Immer eine Instanz verfügbar
