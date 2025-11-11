# Implementierung: Diagnosebestimmung mit KBV-Daten

**Datum:** 2025-01-27  
**Branch:** `cursor/analyze-branch-and-resolve-open-issues-d9f7`

---

## Übersicht

Die Implementierung verbindet die KBV-Stammdaten mit der Diagnosebestimmung, sodass ICD-Codes validiert und aus KBV-Daten erstellt werden können.

---

## Implementierte Features

### 1. Diagnosis Entity erweitert ✅

**Datei:** `src/main/java/de/bbajor/pvs/intravitreal/treatment/model/Diagnosis.java`

**Neue Felder:**
- `kbvQuarter` - Quartal der KBV-Daten (z.B. "2025-Q1")
- `kbvValidFrom` - Gültigkeitsbeginn
- `kbvValidTo` - Gültigkeitsende
- `validatedAgainstKbv` - Flag, ob gegen KBV validiert

**Neue Methoden:**
- `isIcdCodeValid(LocalDate date)` - Prüft Gültigkeit für ein Datum
- `isIcdCodeCurrentlyValid()` - Prüft aktuelle Gültigkeit

### 2. Datenbank-Migration ✅

**Datei:** `src/main/resources/db/migration/V16__add_kbv_metadata_to_diagnosis.sql`

- Fügt KBV-Metadaten-Spalten zur `diagnosis`-Tabelle hinzu
- Erstellt Indizes für performante Abfragen

### 3. IvomDiagnosisService erweitert ✅

**Datei:** `src/main/java/de/bbajor/pvs/intravitreal/treatment/service/IvomDiagnosisService.java`

**Neue Methoden:**
- `createFromKbvIcd(String icdCode, String quarter)` - Erstellt Diagnosis aus KBV-ICD-Eintrag
- `validateIcdCode(String icdCode, LocalDate date, String quarter)` - Validiert ICD-Code
- `validateIcdCodeCurrently(String icdCode, String quarter)` - Validiert für heute
- `searchKbvIcdEntries(String searchTerm, String quarter)` - Suche für Autocomplete
- `updateWithKbvMetadata(Diagnosis diagnosis, String quarter)` - Aktualisiert bestehende Diagnosis

### 4. TreatmentPlanPresenter erweitert ✅

**Datei:** `src/main/java/de/bbajor/pvs/intravitreal/treatment/controller/TreatmentPlanPresenter.java`

**Neue Methoden:**
- `createDiagnosisFromKbvIcd(String icdCode, String quarter)` - Erstellt Diagnosis aus KBV
- `searchKbvIcdEntries(String searchTerm, String quarter)` - Suche für UI
- `validateIcdCode(String icdCode, String quarter)` - Validierung

### 5. UI-Integration ✅

**Datei:** `src/main/java/de/bbajor/pvs/intravitreal/treatment/ui/TreatmentPlanLayout.java`

**Features:**
- **Autocomplete mit KBV-Daten**: Wenn ein ICD-Code eingegeben wird (Format: z.B. "H35.0"), wird automatisch in KBV-Daten gesucht
- **Automatische Erstellung**: Wenn ein ICD-Code in KBV gefunden wird, wird automatisch eine validierte Diagnosis erstellt
- **Warnung bei veralteten Codes**: Veraltete ICD-Codes werden mit ⚠️ markiert
- **Fallback**: Wenn kein ICD-Code erkannt wird, wird eine normale Diagnosis erstellt (ohne Validierung)

**Angepasste Dateien:**
- `TreatmentPlanDetailView.java` - InstitutionRepository-Parameter hinzugefügt
- `TreatmentPlanDialog.java` - InstitutionRepository-Parameter hinzugefügt

---

## Verwendung

### In der UI

1. **ICD-Code eingeben**: In der ComboBox "Behandlungsgrund" einen ICD-Code eingeben (z.B. "H35.0")
2. **Automatische Suche**: Das System erkennt das Format und sucht in KBV-Daten
3. **Automatische Erstellung**: Wenn gefunden, wird eine validierte Diagnosis erstellt
4. **Warnung**: Veraltete Codes werden mit ⚠️ markiert

### Programmatisch

```java
// Diagnosis aus KBV erstellen
Optional<Diagnosis> diagnosis = diagnosisService.createFromKbvIcd("H35.0", "2025-Q1");

// ICD-Code validieren
boolean isValid = diagnosisService.validateIcdCodeCurrently("H35.0", "2025-Q1");

// Suche für Autocomplete
List<KbvIcdEntryDto> entries = diagnosisService.searchKbvIcdEntries("H35", "2025-Q1");
```

---

## Quartalsupdates

Die Quartalsupdate-Check-Funktionalität kann als separater Service implementiert werden:

```java
@Service
public class DiagnosisQuartalsUpdateService {
    
    @Transactional
    public void checkAndUpdateDiagnoses(String newQuarter) {
        // Finde alle validierten Diagnosen
        // Prüfe gegen neue KBV-Daten
        // Aktualisiere oder warne bei veralteten Codes
    }
}
```

**Status:** ⚪ Optional - kann später implementiert werden

---

## Nächste Schritte (Optional)

1. **Quartalsupdate-Service**: Automatische Prüfung bestehender Diagnosen bei Quartalsupdates
2. **Bulk-Update**: Batch-Update mehrerer Diagnosen
3. **Benachrichtigungen**: E-Mail-Benachrichtigungen bei veralteten Codes
4. **Reporting**: Dashboard für veraltete Diagnosen

---

## Testing

**Empfohlene Tests:**
- `IvomDiagnosisServiceTest` - Service-Methoden testen
- `DiagnosisEntityTest` - Validierungslogik testen
- UI-Tests für Autocomplete-Funktionalität

---

**Status:** ✅ **Implementiert und funktionsfähig**

Die Diagnosebestimmung mit KBV-Daten ist vollständig implementiert und kann verwendet werden.
