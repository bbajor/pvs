# PVS Serverless Microservices

Diese Dokumentation beschreibt die Serverless-Architektur der PVS-Anwendung.

## Übersicht

Die PVS-Anwendung wurde in eine serverless Microservices-Architektur migriert, die auf Spring Cloud Function basiert. Alle Business-Logik-Services wurden in plattform-agnostische Functions umgewandelt, die auf OpenFaaS, AWS Lambda, Azure Functions oder anderen FaaS-Plattformen deployed werden können.

## Architektur

### Komponenten

1. **pvs-common**: Shared Kernel mit gemeinsamen DTOs, Utils und Base Classes
2. **Functions**: Serverless Functions für jeden Service
   - Patient Functions
   - Institution Functions
   - Treatment Functions
   - Task Functions
   - Appointment Functions
   - AI Functions (Voice Transcription, Data Extraction)
   - eGK Functions
   - Analytics Functions
   - Scheduled Tasks (Daily Task Creation)
3. **API Gateway**: Spring Cloud Gateway für Function-Routing
4. **Database**: PostgreSQL mit Row-Level Security für Multi-Tenant-Isolation

### Multi-Tenant-Isolation

- **Institution-ID als Function-Parameter**: Alle Function-Requests enthalten `institutionId`
- **FunctionWrapper**: Setzt automatisch InstitutionContext aus Request
- **PostgreSQL RLS**: Zusätzliche Sicherheitsebene auf Datenbankebene
- **Defense-in-Depth**: Mehrschichtige Sicherheit (Application + Database)

## Deployment

### Lokale Entwicklung

```bash
# OpenFaaS starten
cd docker/openfaas
podman-compose up -d

# Function deployen
.\scripts\deployment\deploy-function.ps1 -ServiceName patient-service -FunctionName createPatient
```

### OpenFaaS auf Hetzner

1. OpenFaaS installieren (siehe `docker/openfaas/README.md`)
2. Functions deployen via CI/CD Pipeline oder manuell
3. API Gateway konfigurieren (siehe `GatewayConfig.java`)

### Function-Aufruf

```bash
# Via OpenFaaS Gateway
curl -X POST http://localhost:31112/function/patient-service-createPatient \
  -H "Content-Type: application/json" \
  -d '{
    "institutionId": 1,
    "patient": {
      "firstName": "Max",
      "lastName": "Mustermann"
    }
  }'

# Via API Gateway (wenn konfiguriert)
curl -X POST http://localhost:8080/api/functions/patient/create \
  -H "Content-Type: application/json" \
  -d '{
    "institutionId": 1,
    "patient": {...}
  }'
```

## Funktionen

### Patient Service
- `createPatient`: Neuen Patienten anlegen
- `updatePatient`: Patienten aktualisieren
- `findPatient`: Patienten nach ID finden
- `searchPatients`: Patienten nach Name suchen
- `getAllPatients`: Alle Patienten einer Institution abrufen

### Institution Service
- `createInstitution`: Neue Institution anlegen
- `getInstitution`: Institution nach Code finden
- `listInstitutions`: Alle Institutionen auflisten

### Treatment Service
- `approveTreatment`: Behandlung genehmigen (erste/zweite Genehmigung)

### Task Service
- `completeTask`: Task als erledigt markieren

### Appointment Service
- `scheduleAppointment`: Termin anlegen
- `cancelAppointment`: Termin stornieren
- `getAppointments`: Termine abrufen
- `findNextAvailableSlot`: Nächsten verfügbaren Termin finden

### AI Service
- `transcribeVoice`: Sprachaufnahme transkribieren
- `extractPatientData`: Patientendaten aus Text extrahieren

### eGK Service
- `processEgkData`: eGK-Daten verarbeiten

### Analytics Service
- `getStatistics`: Statistiken abrufen

### Scheduled Tasks
- `scheduledDailyTask`: Tägliche Task-Erstellung (wird via Cron getriggert)

## Konfiguration

### Environment Variables

- `SPRING_CLOUD_FUNCTION_DEFINITION`: Name der Function (z.B. `createPatient`)
- `SPRING_PROFILES_ACTIVE`: `serverless`
- `DB_URL`: Datenbank-URL
- `DB_USER`: Datenbank-Benutzer
- `DB_PASSWORD`: Datenbank-Passwort

### Database Configuration

Die Database-Config ist für Serverless optimiert:
- Kleine Connection Pools (1-5 Connections)
- Schnelle Timeouts (5 Sekunden)
- Kein Connection Warming

## Monitoring

- **Micrometer**: Function-Metriken (Execution Count, Duration, Errors)
- **Prometheus**: Metriken-Export
- **Health Checks**: `/actuator/health` Endpoint

## Nächste Schritte

1. **UI-Migration zu Hilla**: Vaadin Flow → Hilla (React + `@BrowserCallable` Endpoints)
2. **Integrationstests**: Tests für alle Functions
3. **Load Testing**: Performance-Tests für Serverless-Deployment
4. **Production Deployment**: Deployment auf Hetzner mit OpenFaaS

## Weitere Informationen

- Spring Cloud Function: https://spring.io/projects/spring-cloud-function
- OpenFaaS: https://www.openfaas.com/
- PostgreSQL RLS: https://www.postgresql.org/docs/current/ddl-rowsecurity.html

