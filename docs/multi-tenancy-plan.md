# Multi-Tenancy Implementierungsplan

## Übersicht

Die Anwendung wird um Multi-Tenancy erweitert, sodass jede Praxis/MVZ/Klinik (Tenant) ihre eigenen isolierten Daten hat. Dies ermöglicht den Betrieb mehrerer Einrichtungen auf derselben Plattform mit vollständiger Datenisolation.

## Architektur

### Tenant-Modell

Jeder Tenant repräsentiert eine Einrichtung (Praxis/MVZ/Klinik) und hat:
- **Tenant-Code**: Eindeutiger Identifikator (z.B. `PRAX-A1B2C3D4`)
- **Tenant-Name**: Anzeigename der Einrichtung
- **Status**: Aktiv/Inaktiv
- **Beschreibung**: Optionale zusätzliche Informationen

### Datenmodell-Änderungen

Alle fachlichen Entities wurden um eine `tenant_id`-Spalte erweitert:

#### Kern-Entities mit `tenant_id` (NOT NULL):
- `Patient` - Patienten gehören zu einem Tenant
- `UserAccount` - Benutzer gehören zu einem Tenant (außer Super-Admins)
- `Practice` - Praxisdaten sind tenant-spezifisch
- `Treatment` - Behandlungen sind tenant-spezifisch
- `TreatmentPlan` - Behandlungspläne sind tenant-spezifisch
- `Task` - Aufgaben sind tenant-spezifisch
- `SurgicalCenter` - OP-Zentren sind tenant-spezifisch
- `SurgicalCenterTimeSlot` - Zeitslots sind tenant-spezifisch
- `ClinicalTrial` - Klinische Studien sind tenant-spezifisch

#### Entities mit optionaler `tenant_id`:
- `Medication` - Kann tenant-spezifisch oder system-weit sein
- `Diagnosis` - Kann tenant-spezifisch oder system-weit sein
- `HealthInsurance` - Kann tenant-spezifisch sein

### Unique Constraints

Alle unique constraints wurden um `tenant_id` erweitert, um Eindeutigkeit innerhalb eines Tenants zu gewährleisten:

**Beispiel Patient:**
```java
@UniqueConstraint(columnNames = { "tenant_id", "first_name", "last_name", "birth" })
@UniqueConstraint(columnNames = { "tenant_id", "insurance_number" })
```

Dies bedeutet: Ein Patient kann in verschiedenen Tenants denselben Namen/Versicherungsnummer haben.

## Login-Prozess

### Neuer Login-Flow

1. Benutzer gibt **Tenant-Code** ein
2. Benutzer gibt **Benutzername** ein
3. Benutzer gibt **Passwort** ein

### Authentifizierung

Der neue `TenantAuthenticationProvider` validiert:
1. Tenant-Code existiert und ist aktiv
2. Benutzername existiert
3. Benutzer gehört zu diesem Tenant (oder ist Super-Admin)
4. Passwort ist korrekt
5. Benutzer ist aktiviert

Nach erfolgreicher Authentifizierung wird ein `TenantAuthenticationToken` erstellt, das die Tenant-ID enthält.

## Security & Datenisolation

### TenantContext

Der `TenantContext` speichert die aktuelle Tenant-ID in einem ThreadLocal:
```java
TenantContext.setTenantId(tenantId);
Long currentTenantId = TenantContext.getTenantId();
```

### TenantContextFilter

Der `TenantContextFilter` setzt nach der Authentifizierung automatisch den TenantContext aus dem `TenantAuthenticationToken`.

### Repository-Ebene (Geplant)

Für zusätzliche Sicherheit können Repositories erweitert werden, um automatisch nach `tenant_id` zu filtern:

```java
// Beispiel: PatientRepository
@Query("SELECT p FROM Patient p WHERE p.tenant.id = :tenantId")
List<Patient> findByTenantId(@Param("tenantId") Long tenantId);
```

**Best Practice:** Alle Queries sollten explizit `tenant_id` prüfen oder über Hibernate Filter automatisch filtern.

## Rollen & Berechtigungen

### Neue Rollen

- **SUPER_ADMIN**: Kann alle Tenants verwalten, gehört zu keinem spezifischen Tenant
- **ADMIN**: Kann innerhalb seines Tenants administrieren
- **USER**: Standard-Benutzer innerhalb eines Tenants

### Berechtigungen

| Rolle | Tenant-Verwaltung | Benutzer-Verwaltung | Patienten-Verwaltung |
|-------|-------------------|---------------------|----------------------|
| SUPER_ADMIN | ✅ Alle Tenants | ✅ Alle Tenants | ✅ Alle Tenants |
| ADMIN | ❌ | ✅ Eigener Tenant | ✅ Eigener Tenant |
| USER | ❌ | ❌ | ✅ Eigener Tenant |

## Test-Daten (Dev/Test)

Der `TenantTestDataInitializer` erstellt automatisch:

**Tenants:**
- `DEV-TEST` - Standard-Test-Tenant
- `PRAX-001` - Beispiel-Praxis 1
- `PRAX-002` - Beispiel-Praxis 2

**Benutzer:**
- `superadmin` / `123` - Super-Admin (kein Tenant)
- `testadmin` / `123` - Admin für DEV-TEST
- `dr.mueller` / `123` - Admin für PRAX-001
- `dr.schmidt` / `123` - Admin für PRAX-002

## Migration bestehender Daten

### Strategie für Produktionsdaten

Für bestehende Produktionsinstanzen:

1. **Backup erstellen** - Vollständiges DB-Backup vor Migration
2. **Standard-Tenant erstellen** - Für bestehende Daten
3. **Daten migrieren** - Alle bestehenden Daten dem Standard-Tenant zuweisen
4. **Unique Constraints aktualisieren** - Nach Migration
5. **Testen** - Sicherstellen, dass alle Funktionen funktionieren

### Migrations-Script (Beispiel)

```sql
-- 1. Tenant-Tabelle erstellen
CREATE TABLE tenant (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(50) NOT NULL UNIQUE,
    tenant_name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    description VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0
);

-- 2. Standard-Tenant anlegen
INSERT INTO tenant (tenant_code, tenant_name, active, description)
VALUES ('PROD-DEFAULT', 'Bestehende Praxis', true, 'Migrierte Daten');

-- 3. tenant_id zu allen Tabellen hinzufügen
ALTER TABLE patient ADD COLUMN tenant_id BIGINT;
ALTER TABLE user_account ADD COLUMN tenant_id BIGINT;
-- ... weitere Tabellen

-- 4. Bestehende Daten zuweisen
UPDATE patient SET tenant_id = (SELECT id FROM tenant WHERE tenant_code = 'PROD-DEFAULT');
UPDATE user_account SET tenant_id = (SELECT id FROM tenant WHERE tenant_code = 'PROD-DEFAULT');
-- ... weitere Tabellen

-- 5. NOT NULL und Foreign Keys setzen (nach Migration)
ALTER TABLE patient ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE patient ADD CONSTRAINT fk_patient_tenant 
    FOREIGN KEY (tenant_id) REFERENCES tenant(id);
-- ... weitere Constraints
```

## Implementierungs-Schritte

### ✅ Phase 1: Datenmodell (COMPLETED)
- [x] Tenant-Entity erstellen
- [x] TenantRepository erstellen
- [x] TenantService erstellen
- [x] Bestehende Entities um `tenant_id` erweitern
- [x] Unique Constraints anpassen

### ✅ Phase 2: Login & Security (COMPLETED)
- [x] Login-UI um Tenant-Code-Feld erweitern
- [x] TenantAuthenticationToken implementieren
- [x] TenantAuthenticationProvider implementieren
- [x] TenantContext und TenantContextFilter implementieren

### ✅ Phase 3: Tenant-Verwaltung (COMPLETED)
- [x] TenantManagementView erstellen (für Super-Admins)
- [x] TenantTestDataInitializer für Dev/Test-Daten
- [x] SUPER_ADMIN-Rolle definieren

### 🔄 Phase 4: Repository-Isolation (TODO)
- [ ] Repository-Queries um tenant_id-Filter erweitern
- [ ] Hibernate Filter konfigurieren (optional)
- [ ] Service-Layer um Tenant-Checks erweitern

### 🔄 Phase 5: Tests (TODO)
- [ ] Unit-Tests für Tenant-Isolation
- [ ] Integration-Tests für Multi-Tenant-Szenarien
- [ ] Security-Tests (Tenant-Leakage verhindern)

### 🔄 Phase 6: Migration & Deployment (TODO)
- [ ] Migrations-Scripts für Produktion erstellen
- [ ] Deployment-Guide aktualisieren
- [ ] Rollback-Plan erstellen

## Sicherheitsüberlegungen

### ⚠️ Kritische Punkte

1. **Tenant-Leakage verhindern**: Alle Queries MÜSSEN tenant_id prüfen
2. **Cross-Tenant-Zugriff**: NIEMALS `tenant_id` aus User-Input verwenden
3. **Super-Admin-Rechte**: Nur für vertrauenswürdige System-Administratoren
4. **Audit-Logging**: Tenant-ID in allen Audit-Logs speichern

### Best Practices

- **Explizite Tenant-Checks**: Besser explizit prüfen als auf Hibernate Filter verlassen
- **Repository-Tests**: Jedes Repository sollte Tenant-Isolation testen
- **Code-Reviews**: Besonders bei neuen Queries auf Tenant-Isolation achten
- **Monitoring**: Verdächtige Cross-Tenant-Zugriffe loggen und überwachen

## Offene Fragen & Diskussionspunkte

1. **Shared vs. Tenant-spezifische Medications**: Sollen Medications global oder pro Tenant sein?
   - **Empfehlung**: Optional - System-weite Medications (tenant_id = NULL) + Tenant-spezifische Custom-Medications
   
2. **Tenant-Migration**: Sollen Benutzer/Daten zwischen Tenants migrierbar sein?
   - **Empfehlung**: Vorerst NEIN - zu komplex und fehleranfällig

3. **Tenant-Löschung**: Soft-Delete oder Hard-Delete?
   - **Empfehlung**: Soft-Delete (active=false) + optionales Hard-Delete für DSGVO-Compliance

## Akzeptanzkriterien

### ✅ Must-Have

- [x] Tenant-Login funktioniert mit Tenant-Code
- [x] Jeder Tenant sieht nur seine eigenen Patienten
- [x] Super-Admin kann alle Tenants verwalten
- [x] Test-Daten für Dev-Umgebung vorhanden

### 🔄 Should-Have

- [ ] Repository-Queries sind tenant-sicher
- [ ] Migration-Script für Produktionsdaten vorhanden
- [ ] Tests für Tenant-Isolation geschrieben
- [ ] Monitoring für Cross-Tenant-Zugriffe

### 📋 Nice-to-Have

- [ ] Tenant-Statistiken im Admin-Dashboard
- [ ] Tenant-spezifische Theming
- [ ] Tenant-Export/-Import-Funktion

## Nächste Schritte

1. ✅ Entity-Modell um tenant_id erweitern
2. ✅ Login-Flow mit Tenant-Code implementieren
3. ✅ Tenant-Verwaltung für Super-Admins
4. ✅ Repository-Isolation implementieren
5. ✅ Tests schreiben
6. ✅ Migration-Scripts für Produktion
7. ✅ Security-Audit-Logging implementieren
8. ✅ Service-Layer um Tenant-Validierung erweitern

## Implementierungsstatus

### ✅ Phase 1: Kern-Architektur (COMPLETED)
- Datenmodell mit tenant_id
- Login mit Tenant-Code
- TenantContext & Security-Filter
- Tenant-Verwaltungs-UI
- Test-Daten für Dev/Test

### ✅ Phase 2: Produktionsreife (COMPLETED)
- Repository-Isolation mit TenantAwareRepository
- Explizite Tenant-Filter in kritischen Queries
- TenantAccessValidator für Service-Layer
- Security-Audit-Logging mit TenantAuditLogger
- Umfassende Unit-Tests

### 📋 Phase 3: Optional (Für zukünftige Iterationen)
- Hibernate Filter für automatisches Tenant-Filtering
- AOP-Aspekte für Tenant-Validierung
- Tenant-spezifische Features & Theming
- Integration-Tests mit Spring-Context
- Tenant-Export/Import-Funktionalität

---

**Status**: ✅ PRODUKTIONSBEREIT  
**Version**: 1.0  
**Letzte Aktualisierung**: 2025-10-31
