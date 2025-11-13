# Resilienz - Retry-Logik & Circuit-Breaker

## Übersicht

Resilienz-Maßnahmen sorgen dafür, dass die Anwendung auch bei Ausfällen externer Services stabil bleibt.

## Implementierung

### Resilience4j

Die Anwendung nutzt Resilience4j für:
- **Retry-Logik:** Automatische Wiederholung bei temporären Fehlern
- **Circuit-Breaker:** Schutz vor kaskadierenden Ausfällen

### Konfiguration

```yaml
# application-cloud.yaml
resilience4j:
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 1000
        retryExceptions:
          - java.net.ConnectException
          - java.net.http.HttpTimeoutException
          - java.io.IOException
    instances:
      whisper-api:
        maxAttempts: 3
        waitDuration: 2000
      kbv-api:
        maxAttempts: 2
        waitDuration: 1000
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
    instances:
      whisper-api:
        waitDurationInOpenState: 60s
      kbv-api:
        waitDurationInOpenState: 30s
```

## Retry-Logik

### Verhalten

1. **Erster Versuch:** Request wird ausgeführt
2. **Bei Fehler:** Retry nach `waitDuration` (mit Exponential Backoff)
3. **Max Attempts:** Nach `maxAttempts` wird Exception geworfen

### Retry-Exceptions

Nur bestimmte Exceptions lösen Retry aus:
- `ConnectException` - Verbindungsfehler
- `HttpTimeoutException` - Timeout
- `IOException` - I/O-Fehler

### Beispiel

```java
@Autowired
private ResilientHttpClient resilientHttpClient;

public String callApi() {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.example.com/data"))
        .build();
    
    return resilientHttpClient.executeWithResilience(
        request, 
        "whisper-api"  // Instance name
    );
}
```

## Circuit-Breaker

### Zustände

1. **CLOSED:** Normalbetrieb, Requests werden durchgelassen
2. **OPEN:** Zu viele Fehler, Requests werden sofort abgelehnt
3. **HALF_OPEN:** Test-Phase, einige Requests werden durchgelassen

### Schwellwerte

- **failureRateThreshold:** 50% (bei 50% Fehlerrate → OPEN)
- **minimumNumberOfCalls:** 5 (mindestens 5 Calls für Statistik)
- **waitDurationInOpenState:** 30s (Wartezeit vor HALF_OPEN)

### Beispiel

```java
// Circuit Breaker schützt vor kaskadierenden Ausfällen
// Wenn Whisper API ausfällt, wird nach 5 Fehlern der Circuit geöffnet
// Nach 60s wird HALF_OPEN versucht
```

## Fallback-Mechanismen

### Whisper AI

```java
// 1. Versuch: Lokaler Whisper Container
// 2. Fallback: Remote Whisper API (mit Retry)
// 3. Fallback: Fehlermeldung an Benutzer
```

### KBV Master Data Service

```java
// 1. Versuch: KBV API (mit Retry)
// 2. Fallback: Cache (wenn verfügbar)
// 3. Fallback: Fehlermeldung
```

## Monitoring

### Metrics

```bash
# Circuit Breaker Status
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state

# Retry Attempts
curl http://localhost:8080/actuator/metrics/resilience4j.retry.calls
```

### Health Indicators

```yaml
# application-cloud.yaml
management:
  health:
    circuitbreakers:
      enabled: true
```

## Best Practices

1. **Retry nur bei temporären Fehlern:**
   - ConnectException → Retry
   - 404 Not Found → Kein Retry

2. **Circuit Breaker konfigurieren:**
   - Angemessene Schwellwerte
   - Nicht zu aggressiv (sonst zu viele False Positives)

3. **Fallback-Mechanismen:**
   - Immer einen Fallback haben
   - Cache für externe APIs

4. **Monitoring:**
   - Circuit Breaker Status überwachen
   - Retry-Statistiken sammeln

5. **Timeout konfigurieren:**
   - Nicht zu lang (sonst lange Wartezeiten)
   - Nicht zu kurz (sonst zu viele Timeouts)
