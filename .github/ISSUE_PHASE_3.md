# Phase 3: Erweiterte Multi-Tenancy Features

## Übersicht

Phase 1 & 2 der Multi-Tenancy-Implementierung sind abgeschlossen und produktionsbereit. Phase 3 umfasst optionale, erweiterte Features für eine noch robustere und flexiblere Lösung.

## Status

- ✅ Phase 1: Kern-Architektur (COMPLETED)
- ✅ Phase 2: Produktionsreife (COMPLETED)  
- 📋 Phase 3: Erweiterte Features (GEPLANT)

## Geplante Features

### 1. 🔍 Hibernate Filter für automatisches Tenant-Filtering

**Priorität:** HIGH  
**Aufwand:** 2-3 Tage

Automatisches Hinzufügen von `WHERE tenant_id = :tenantId` bei JEDER Datenbank-Query durch Hibernate Filter.

**Vorteile:**
- Fail-Safe: Vergessene Tenant-Filter unmöglich
- Weniger Boilerplate-Code
- Transparenz über alle Queries

**Implementierung:**
```java
@Entity
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Patient extends BasicEntity<Integer> {
    // Hibernate fügt automatisch WHERE-Clause hinzu
}
```

**TODO:**
- [ ] `@FilterDef` auf alle Entities hinzufügen
- [ ] Filter in EntityManagerFactory konfigurieren
- [ ] Automatisches Aktivieren des Filters bei Request-Start
- [ ] Tests für automatisches Filtering
- [ ] Performance-Tests

---

### 2. 🎯 AOP-Aspekte für Tenant-Validierung

**Priorität:** MEDIUM  
**Aufwand:** 2 Tage

Automatische Tenant-Validierung bei allen Repository-Operationen durch AspectJ.

**Vorteile:**
- Zentrale Cross-Cutting-Concern-Behandlung
- Automatische Validierung bei save/delete/find
- Reduziert Duplikation

**Implementierung:**
```java
@Aspect
@Component
public class TenantAccessAspect {
    @Before("execution(* de.bbajor.pvs..repository.*Repository.save(..))")
    public void validateTenantBeforeSave(JoinPoint joinPoint) {
        Object entity = joinPoint.getArgs()[0];
        validateTenant(entity);
    }
}
```

**TODO:**
- [ ] Spring AOP Dependency hinzufügen
- [ ] Aspekt für save/delete/find implementieren
- [ ] Exception-Handling bei Violations
- [ ] Performance-Tests
- [ ] Dokumentation

---

### 3. 🎨 Tenant-spezifische Features & Theming

**Priorität:** LOW  
**Aufwand:** 3-5 Tage

Jeder Tenant kann eigene UI-Themes, Feature-Flags und Konfigurationen haben.

**Features:**
- Custom Farb-Themes (Primary Color, Logo)
- Feature-Flags (AI-Extraction, Advanced Reports, etc.)
- Workflow-Konfigurationen (z.B. Zweite Genehmigung erforderlich)
- Max. Benutzer-Limits

**Implementierung:**
```java
@Entity
public class TenantConfiguration {
    @ManyToOne
    private Tenant tenant;
    
    private String primaryColor;
    private String logoUrl;
    private boolean aiExtractionEnabled;
    private int maxUsers;
}
```

**TODO:**
- [ ] TenantConfiguration Entity erstellen
- [ ] UI für Tenant-Einstellungen (Super-Admin)
- [ ] Theming-Service für dynamisches CSS
- [ ] Feature-Flag-Checks in Code integrieren
- [ ] Tests

---

### 4. ✅ Integration-Tests mit Spring-Context

**Priorität:** HIGH  
**Aufwand:** 2 Tage

Vollständige End-to-End-Tests mit echtem Spring-Context und Datenbank.

**Ziel:**
- Cross-Tenant-Zugriffe verhindern testen
- Tenant-Isolation verifizieren
- Performance unter Last testen

**Implementierung:**
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantIsolationIntegrationTest {
    @Test
    void tenant1_cannotAccessTenant2Data() {
        TenantContext.setTenantId(tenant1.getId());
        List<Patient> patients = patientRepository.findAll();
        assertTrue(patients.stream().allMatch(
            p -> p.getTenant().getId().equals(tenant1.getId())));
    }
}
```

**TODO:**
- [ ] Test-Profile korrekt konfigurieren
- [ ] TestContainers für isolierte DB-Tests
- [ ] Cross-Tenant-Access-Tests
- [ ] Performance-Tests unter Last
- [ ] CI/CD-Integration

---

### 5. 📦 Tenant-Export/Import-Funktionalität

**Priorität:** MEDIUM  
**Aufwand:** 3-4 Tage

Tenant-Daten exportieren und importieren für Migrations, Backups, DSGVO-Compliance.

**Use Cases:**
- Tenant-Migration zwischen Servern
- Tenant-spezifische Backups
- DSGVO-Datenportabilität
- Test-Daten generieren

**Implementierung:**
```java
@Service
public class TenantExportService {
    public TenantExport exportTenant(Long tenantId) {
        return TenantExport.builder()
            .tenant(tenantRepository.findById(tenantId))
            .patients(patientRepository.findByTenantId(tenantId))
            .treatments(treatmentRepository.findByTenantId(tenantId))
            .build();
    }
    
    public void importTenant(TenantExport export, Long targetTenantId) {
        // Import-Logik
    }
}
```

**TODO:**
- [ ] TenantExport DTO erstellen
- [ ] Export-Service implementieren
- [ ] Import-Service mit Validierung
- [ ] UI für Export/Import (Admin)
- [ ] Tests für Export/Import
- [ ] Dokumentation

---

### 6. 📊 Tenant-Statistiken im Admin-Dashboard

**Priorität:** LOW  
**Aufwand:** 2-3 Tage

Übersicht für Super-Admins über Tenant-Nutzung, Performance, Storage.

**Metriken:**
- Anzahl Patienten/Behandlungen pro Tenant
- Speicherplatz-Nutzung
- Login-Statistiken
- Feature-Usage
- Performance-Metriken

**Implementierung:**
```java
@Route("admin/tenant-stats")
public class TenantStatsView extends VerticalLayout {
    Grid<TenantStats> grid;
    // Zeigt: Tenant | Patienten | Treatments | Last Login | Storage
}
```

**TODO:**
- [ ] TenantStats Service erstellen
- [ ] Statistik-Aggregation implementieren
- [ ] Admin-Dashboard UI
- [ ] Charts/Visualisierungen
- [ ] Performance-Optimierung
- [ ] Caching für Statistiken

---

### 7. 🔐 Erweiterte Security-Features

**Priorität:** MEDIUM  
**Aufwand:** 4-5 Tage

Zusätzliche Sicherheits-Features für Enterprise-Kunden.

**Features:**
- Rate Limiting pro Tenant (verhindert Missbrauch)
- IP-Whitelisting pro Tenant
- 2FA (Two-Factor Authentication)
- SSO-Integration (Single Sign-On)
- Session-Management (Max. Sessions pro User/Tenant)
- Audit-Log-Export für Compliance

**TODO:**
- [ ] Rate Limiting mit Redis
- [ ] IP-Whitelist-Feature
- [ ] 2FA mit TOTP
- [ ] SSO-Integration (SAML/OAuth)
- [ ] Session-Management
- [ ] Audit-Log-Export

---

## Abhängigkeiten

Neue Dependencies für Phase 3:
```gradle
// AOP
implementation 'org.springframework.boot:spring-boot-starter-aop'

// TestContainers
testImplementation 'org.testcontainers:testcontainers:1.19.0'
testImplementation 'org.testcontainers:postgresql:1.19.0'

// Rate Limiting (optional)
implementation 'org.springframework.boot:spring-boot-starter-data-redis'

// 2FA (optional)
implementation 'dev.samstevens.totp:totp:1.7.1'
```

## Reihenfolge-Empfehlung

1. **Zuerst:** Hibernate Filter + Integration-Tests (HIGH Priority, maximale Sicherheit)
2. **Dann:** Tenant-Export/Import (MEDIUM Priority, praktisch für Backups)
3. **Optional:** Theming, Stats, Erweiterte Security (LOW Priority, Nice-to-Have)

## Schätzung Gesamt-Aufwand

- **HIGH Priority:** ~6-7 Tage
- **MEDIUM Priority:** ~9-11 Tage  
- **LOW Priority:** ~7-10 Tage
- **GESAMT:** ca. 3-4 Wochen (alle Features)

## Akzeptanzkriterien

- [ ] Hibernate Filter aktiv und getestet
- [ ] Integration-Tests laufen grün
- [ ] AOP-Aspekte validieren Tenant-Zugriffe
- [ ] Tenant-Export/Import funktioniert
- [ ] Admin-Dashboard zeigt Statistiken
- [ ] Erweiterte Security-Features konfigurierbar
- [ ] Dokumentation vollständig
- [ ] Performance-Tests bestanden

---

**Erstellt:** 2025-10-31  
**Status:** GEPLANT  
**Milestone:** Phase 3 - Erweiterte Features
