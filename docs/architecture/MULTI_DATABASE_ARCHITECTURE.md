# Multi-Datenbank-Architektur pro Einrichtung

## Übersicht

Umstellung von Multi-Tenant (eine Datenbank) zu Multi-Datenbank (eine Datenbank pro Einrichtung/Kunde).

## Architektur-Änderungen

### 1. Datenmodell-Refactoring

#### Vorher:
- `Tenant` = Einrichtung (wird in einer zentralen DB gespeichert)
- `Practice` = Praxis-Standort (gehört zu einem Tenant)

#### Nachher:
- `Institution` = Einrichtung/Kunde (wird in einer zentralen Registry-DB gespeichert)
- `Location` = Standort/Praxis (gehört zu einer Institution, mehrere Standorte möglich)

#### Entity-Änderungen:

```java
// Tenants werden zu Institutions (in Registry DB)
@Entity
public class Institution extends BasicEntity<Long> {
    private String institutionCode;  // Login-Identifier
    private String institutionName;
    private String databaseName;     // z.B. "pvs_inst_abc123"
    private String containerName;    // z.B. "postgres-inst-abc123"
    private boolean active;
}

// Practices werden zu Locations (in Institution-DB)
@Entity
public class Location extends BasicEntity<Long> {
    private String locationName;
    private String street;
    private String houseNumber;
    private String postalCode;
    private String city;
    // ... weitere Felder wie bisher Practice
    
    @ManyToOne
    private Institution institution;  // Aber: Institution ist nur Referenz, nicht FK!
}

// Users bekommen preferredLocation
@Entity
public class UserAccount extends BasicEntity<Long> {
    @ManyToOne
    private Institution institution;  // Immer gesetzt (außer Super-Admin)
    
    @ManyToOne
    private Location preferredLocation;  // Optional
}
```

### 2. Datenbank-Struktur

#### Registry-Datenbank (zentral)
- Enthält: Institutions, System-User (Super-Admin)
- Name: `pvs_registry`
- Zweck: Institution-Registry, Login-Validierung, Container-Management

#### Institution-Datenbanken (pro Kunde)
- Enthält: Alle Business-Daten (Patients, Appointments, Locations, etc.)
- Name: `pvs_inst_{institutionCode}`
- Zweck: Vollständige Isolation der Kundendaten

### 3. Dynamic DataSource Management

#### Implementierung:
```java
@Component
public class InstitutionDataSourceManager {
    
    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();
    
    public DataSource getOrCreateDataSource(Institution institution) {
        // 1. Prüfe Cache
        if (dataSourceCache.containsKey(institution.getDatabaseName())) {
            return dataSourceCache.get(institution.getDatabaseName());
        }
        
        // 2. Prüfe ob Container existiert / starte Container
        ensureDatabaseContainer(institution);
        
        // 3. Erstelle DataSource
        DataSource ds = createDataSource(institution);
        dataSourceCache.put(institution.getDatabaseName(), ds);
        
        return ds;
    }
    
    private void ensureDatabaseContainer(Institution institution) {
        // Docker-Container Management
        // - Prüfe ob Container läuft
        // - Wenn nicht: starte Container
        // - Wenn nicht existiert: erstelle Container
    }
}
```

### 4. Login-Flow

1. User gibt Institution-Code, Username, Password ein
2. **Registry-DB prüfen**:
   - Existiert Institution?
   - Ist Institution aktiv?
   - Existiert User in Registry?
   - Ist User berechtigt für Institution?
3. **Institution-DB verbinden**:
   - Container prüfen/starten
   - DataSource erstellen/verbinden
   - User in Institution-DB validieren
4. **TenantContext setzen**:
   - Institution-ID
   - Institution-DB DataSource
   - Preferred Location (optional)

### 5. Docker-Container-Management

#### Pro Institution:
```yaml
# Dynamisch generiertes docker-compose für jede Institution
postgres-inst-{institutionCode}:
  image: postgres:15
  container_name: postgres-inst-{institutionCode}
  environment:
    POSTGRES_DB: pvs_inst_{institutionCode}
    POSTGRES_USER: pvs_user
    POSTGRES_PASSWORD: {generated}
  volumes:
    - pvs_inst_{institutionCode}:/var/lib/postgresql/data
  ports:
    - {dynamic_port}:5432
```

#### Container-Lifecycle:
- **Erstellung**: Bei erstem Login (falls nicht existiert)
- **Start**: Bei jedem Login (falls gestoppt)
- **Stop**: Nach Inaktivität (optional, Timer-basiert)
- **Löschung**: Bei Institution-Deaktivierung

### 6. Migration-Strategie

#### Phase 1: Vorbereitung
- [ ] Institution-Entity erstellen
- [ ] Location-Entity erstellen (ersetzt Practice)
- [ ] Registry-DB Setup
- [ ] DynamicDataSource-Infrastruktur

#### Phase 2: Dual-Mode
- [ ] Beide Modis parallel (aktuell + neu)
- [ ] Migration-Tool für bestehende Daten
- [ ] Login-Flow für beide Modis

#### Phase 3: Umschaltung
- [ ] Alle Institutionen migrieren
- [ ] Alten Multi-Tenant-Mode entfernen
- [ ] Registry-Only-Mode aktivieren

## Vorteile

1. **Vollständige Isolation**: Ein Fehler in einer Institution-DB betrifft nicht andere
2. **Skalierbarkeit**: Institutionen können auf verschiedene Server verteilt werden
3. **Wartbarkeit**: Backup/Restore pro Institution einfach
4. **Sicherheit**: Cross-Institution-Zugriffe technisch unmöglich
5. **Performance**: Keine Tenant-Filter-Queries nötig

## Herausforderungen

1. **Container-Management**: Dynamische Container-Erstellung/Verwaltung
2. **Connection-Pooling**: Viele DataSources = viele Connection-Pools
3. **Migration**: Bestehende Daten migrieren
4. **Komplexität**: Mehr zu verwalten
5. **Ressourcen**: Mehr Datenbank-Container = mehr Ressourcen

## Umsetzung

Diese Änderung ist zu umfangreich für eine einzige Session. Empfohlene Vorgehensweise:

1. **Phase 1**: Datenmodell-Refactoring (Tenant→Institution, Practice→Location)
2. **Phase 2**: DynamicDataSource-Infrastruktur
3. **Phase 3**: Container-Management
4. **Phase 4**: Login-Flow-Anpassung
5. **Phase 5**: Migration-Tool

Soll ich mit Phase 1 beginnen?

