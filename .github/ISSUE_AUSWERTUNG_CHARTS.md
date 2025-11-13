# Feature: Auswertungen mit Charts

## Übersicht

Es wird ein neues Feature benötigt, das verschiedene Daten der Anwendung grafisch aufbereitet und visualisiert. Dazu soll ein neuer Menüeintrag "Auswertung" angelegt werden, der verschiedene Metriken in Form von interaktiven Charts darstellt.

## Status

- 📋 GEPLANT

## Anforderungen

### 1. 🎨 UI-Integration

**Menüeintrag & Dashboard-Kachel:**
- Neuer Menüeintrag "Auswertung" im SideNav (MainLayout)
- Dashboard-Kachel auf der MainView
- Route: `/auswertung` oder `/analytics`
- Icon: `vaadin:chart` oder `vaadin:bar-chart` oder `vaadin:line-chart`
- Order: 7 (nach Einstellungen)

**Berechtigungen:**
- Zugriff für alle authentifizierten Benutzer (analog zu anderen Views)
- Optional: Rollenbasierte Einschränkung (z.B. nur für ADMIN/OWNER)

### 2. 📊 Zu visualisierende Metriken

#### 2.1 Patientenbehandlungen pro Monat/Jahr
- **Chart-Typ:** Liniendiagramm oder Balkendiagramm
- **Zeitraum:** Monat/Jahr (mit Umschaltmöglichkeit)
- **Datenquelle:** Treatment-Entity (intravitreal/treatment)
- **Aggregation:** COUNT(Treatment) GROUP BY Jahr/Monat

#### 2.2 Patientenbehandlungen je Zeitslot als Verlaufskurve
- **Chart-Typ:** Liniendiagramm (Verlaufskurve) mit Wechselmöglichkeit zu Balkendiagramm
- **Datenquelle:** Treatment-Entity mit Zeitslot-Information
- **Aggregation:** COUNT(Treatment) GROUP BY Zeitslot
- **UI:** Toggle zwischen Liniendiagramm und Balkendiagramm

#### 2.3 Anzahl Patienten nach Altersklassen
- **Chart-Typ:** Balkendiagramm oder Kreisdiagramm (mit Umschaltmöglichkeit)
- **Altersklassen:** 
  - 0-3 Jahre
  - 4-12 Jahre
  - 13-18 Jahre
  - 19-30 Jahre
  - 31-50 Jahre
  - 51-65 Jahre
  - 66-80 Jahre
  - 81+ Jahre
- **Datenquelle:** Patient-Entity mit Geburtsdatum
- **Berechnung:** Aktuelles Datum - Geburtsdatum → Altersklasse zuordnen

#### 2.4 Anzahl Patienten Kasse/privat
- **Chart-Typ:** Kreisdiagramm (Pie Chart) oder Balkendiagramm
- **Datenquelle:** Patient-Entity mit Versicherungsart
- **Aggregation:** COUNT(Patient) GROUP BY Versicherungsart (Kasse/privat)

#### 2.5 Anzahl Patienten nach Krankenversicherung
- **Chart-Typ:** Balkendiagramm (horizontal oder vertikal)
- **Datenquelle:** Patient-Entity mit Krankenversicherung
- **Aggregation:** COUNT(Patient) GROUP BY Krankenversicherung (Debeka, AOK, BKK, etc.)
- **Sortierung:** Nach Anzahl absteigend (Top-N anzeigen, Rest als "Sonstige")

#### 2.6 Anzahl Patienten je verwendetes Medikament auf Monat oder Jahr
- **Chart-Typ:** Balkendiagramm (gestapelt oder gruppiert)
- **Zeitraum:** Monat/Jahr (mit Umschaltmöglichkeit)
- **Datenquelle:** Treatment-Entity mit Medikament-Referenz
- **Aggregation:** COUNT(Treatment) GROUP BY Medikament, Jahr/Monat

### 3. 🛠️ Technische Umsetzung

#### 3.1 Chart-Bibliothek: Apache ECharts

**Begründung:**
- Freie Software (Apache 2.0 Lizenz)
- Professionelles Aussehen
- Umfangreiche Chart-Typen
- Interaktive Features (Zoom, Tooltip, etc.)
- Gute Performance
- Vaadin unterstützt JavaScript-Importe nativ

**Integration:**
- ECharts via npm installieren oder CDN einbinden
- Vaadin JavaScript-Integration nutzen (`@JsModule` oder `@JavaScript`)
- TypeScript/JavaScript Wrapper-Komponenten für ECharts erstellen
- Java-Service-Layer für Datenaggregation

#### 3.2 Architektur

**Backend:**
```java
@Service
public class AnalyticsService {
    // Datenaggregation für alle Metriken
    public TreatmentStatistics getTreatmentStatistics(TimeRange range);
    public AgeGroupStatistics getAgeGroupStatistics();
    public InsuranceStatistics getInsuranceStatistics();
    // etc.
}
```

**Frontend:**
- `AnalyticsView.java` - Haupt-View mit Layout
- `EChartsComponent.java` - Vaadin-Wrapper für ECharts
- TypeScript/JavaScript Bridge für ECharts-Initialisierung
- DTOs für Chart-Daten (JSON-serialisierbar)

#### 3.3 Datenmodell

**DTOs:**
```java
public record TreatmentStatistics(
    List<TimeSeriesData> monthlyData,
    List<TimeSeriesData> yearlyData
) {}

public record AgeGroupStatistics(
    Map<String, Long> ageGroups // "0-3" -> 15, "4-12" -> 23, etc.
) {}

public record InsuranceStatistics(
    Map<String, Long> byType, // "Kasse" -> 120, "Privat" -> 45
    Map<String, Long> byProvider // "Debeka" -> 50, "AOK" -> 30, etc.
) {}
```

### 4. ✅ Testgetriebene Entwicklung

**Vorgehen:**
1. **Unit-Tests für Service-Layer:**
   - Test für jede Metrik-Aggregation
   - Test mit Mock-Daten
   - Edge Cases: Leere Daten, Null-Werte, etc.

2. **Integration-Tests:**
   - Test mit echten Datenbank-Daten (Test-Profile)
   - Test der JSON-Serialisierung für Chart-Daten
   - Test der Tenant-Isolation (falls Multi-Tenancy aktiv)

3. **UI-Tests (optional):**
   - TestBench-Tests für Chart-Rendering
   - Test der Interaktivität (Toggle zwischen Chart-Typen)

**Test-Struktur:**
```
src/test/java/de/bbajor/pvs/analytics/
  - service/
    - AnalyticsServiceTest.java
    - TreatmentStatisticsTest.java
    - AgeGroupStatisticsTest.java
    - InsuranceStatisticsTest.java
  - ui/
    - AnalyticsViewTest.java (optional)
```

### 5. 📦 Dependencies

**Gradle:**
```gradle
// ECharts via npm (package.json)
// Oder CDN-Einbindung über @JavaScript Annotation

// Optional: TypeScript-Support für bessere Type-Safety
// (bereits vorhanden via vite.config.ts)
```

**package.json:**
```json
{
  "dependencies": {
    "echarts": "^5.4.3"
  }
}
```

**Hinweis:** `npm install` wird automatisch von Vaadin über die Gradle-Tasks ausgeführt:
- `vaadinPrepareFrontend` (Dev-Mode) - läuft automatisch bei `./gradlew bootRun`
- `vaadinBuildFrontend` (Production) - läuft automatisch bei `./gradlew build`
- Konfiguriert in `application.yaml`: `vaadin.frontend.force-install: true`

**Keine manuelle npm-Installation nötig!** 🎉

### 6. 🎯 Implementierungs-Schritte

1. **Setup:**
   - [x] ECharts-Dependency hinzufügen (package.json) - wird automatisch via Gradle installiert
   - [x] Vaadin JavaScript-Integration vorbereiten
   - [x] AnalyticsService-Skeleton erstellen

2. **Backend:**
   - [ ] AnalyticsService mit Datenaggregation implementieren
   - [ ] DTOs für alle Metriken erstellen
   - [ ] Repository-Queries für Aggregationen schreiben
   - [ ] Unit-Tests für Service-Layer (TDD)

3. **Frontend:**
   - [ ] ECharts-Wrapper-Komponente erstellen
   - [ ] AnalyticsView mit Layout erstellen
   - [ ] Menüeintrag & Dashboard-Kachel hinzufügen
   - [ ] Chart-Komponenten für jede Metrik implementieren
   - [ ] Toggle-Funktionalität für Chart-Typ-Wechsel

4. **Integration:**
   - [ ] Backend-Frontend-Integration testen
   - [ ] JSON-Serialisierung verifizieren
   - [ ] Tenant-Isolation testen (falls relevant)
   - [ ] Performance-Optimierung (Caching bei Bedarf)

5. **Polish:**
   - [ ] Responsive Design
   - [ ] Loading-States
   - [ ] Error-Handling
   - [ ] Dokumentation

### 7. 🔍 Edge Cases & Anforderungen

**Datenqualität:**
- Fehlende Geburtsdaten → Altersklasse "Unbekannt"
- Fehlende Versicherungsdaten → "Nicht angegeben"
- Leere Datensätze → Leere Charts mit entsprechender Meldung

**Performance:**
- Große Datenmengen → Aggregation auf DB-Ebene (nicht in Memory)
- Caching für häufig abgerufene Statistiken (optional)
- Lazy Loading für Charts (nur sichtbare Charts laden)

**Sicherheit:**
- Tenant-Isolation beachten (nur Daten des aktuellen Tenants)
- Keine PII in Logs oder Fehlermeldungen
- Berechtigungsprüfung für Zugriff auf Analytics

**UX:**
- Loading-Indikatoren während Datenabfrage
- Tooltips mit detaillierten Informationen
- Export-Funktion (optional: PDF/CSV)

### 8. 📊 Beispiel-Chart-Konfiguration (ECharts)

```javascript
// Beispiel: Liniendiagramm für Behandlungen pro Monat
{
  title: {
    text: 'Patientenbehandlungen pro Monat'
  },
  tooltip: {
    trigger: 'axis'
  },
  xAxis: {
    type: 'category',
    data: ['Jan', 'Feb', 'Mär', 'Apr', 'Mai', 'Jun', ...]
  },
  yAxis: {
    type: 'value'
  },
  series: [{
    data: [12, 19, 15, 22, 18, 25, ...],
    type: 'line',
    smooth: true
  }]
}
```

### 9. 🎨 UI-Layout

**Struktur:**
- Header mit Titel "Auswertungen"
- Grid-Layout mit 2 Spalten (responsive: 1 Spalte auf Mobile)
- Jede Metrik in eigener Card
- Card-Header mit Metrik-Titel
- Card-Body mit Chart
- Card-Footer mit Toggle-Buttons für Chart-Typ-Wechsel (falls verfügbar)

**Beispiel-Layout:**
```
┌─────────────────────────────────┐
│  Auswertungen                   │
├─────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐   │
│  │ Metrik 1 │  │ Metrik 2 │   │
│  │ [Chart]  │  │ [Chart]  │   │
│  └──────────┘  └──────────┘   │
│  ┌──────────┐  ┌──────────┐   │
│  │ Metrik 3 │  │ Metrik 4 │   │
│  │ [Chart]  │  │ [Chart]  │   │
│  └──────────┘  └──────────┘   │
└─────────────────────────────────┘
```

### 10. 📝 Akzeptanzkriterien

- [ ] Menüeintrag "Auswertung" ist im SideNav sichtbar
- [ ] Dashboard-Kachel "Auswertung" ist auf MainView sichtbar
- [ ] Alle 6 Metriken sind als Charts dargestellt
- [ ] Chart-Typ-Wechsel funktioniert (wo vorgesehen)
- [ ] Zeitraum-Umschaltung funktioniert (Monat/Jahr)
- [ ] Daten werden korrekt aggregiert (Tenant-Isolation)
- [ ] Unit-Tests für Service-Layer sind vorhanden und grün
- [ ] Integration-Tests laufen erfolgreich
- [ ] Keine PII in Logs oder Fehlermeldungen
- [ ] Performance ist akzeptabel (< 2s Ladezeit)
- [ ] Responsive Design funktioniert auf Mobile/Tablet/Desktop

### 11. 🔗 Abhängigkeiten

**Voraussetzungen:**
- Behandlung-Entity (Treatment) mit Zeitslot-Information
- Patient-Entity mit Geburtsdatum und Versicherungsdaten
- Medikament-Entity (falls separate Entity vorhanden)
- Multi-Tenancy-Integration (falls aktiv)

**Externe Dependencies:**
- Apache ECharts (npm package)
- Vaadin JavaScript-Integration

### 12. 📅 Schätzung

**Aufwand:** 5-7 Tage

**Aufteilung:**
- Setup & Backend-Service: 2 Tage
- Frontend-Integration & Charts: 2-3 Tage
- Tests & Polish: 1-2 Tage

---

**Erstellt:** 2025-01-27  
**Status:** GEPLANT  
**Priorität:** MEDIUM  
**Milestone:** Feature Release

