# Kostenmodul - Implementierungsplan

## Übersicht

Das Kostenmodul ermöglicht die Berechnung und Verwaltung von OP-Saal-Kosten pro Institution. Es unterstützt verschiedene Preismodelle (Miete vs. eigene Kosten) und bietet eine Kostenhistorie am Patienten sowie Analytics-Views.

## Feature Flag

- **Feature Key**: `COST_MODULE`
- **Feature Name**: "Kostenmodul"
- **Beschreibung**: "Ermöglicht die Berechnung und Verwaltung von OP-Saal-Kosten sowie Kostenhistorie am Patienten"
- **Standard**: Deaktiviert (muss pro Institution aktiviert werden)

## 1. Datenmodell

### 1.1 Entity: `CostCalculation` (Preismodell pro OP-Saal)

```java
@Entity
@Table(name = "cost_calculation")
public class CostCalculation extends BasicEntity<Long> {
    
    @ManyToOne
    @JoinColumn(name = "surgical_center_id", nullable = false)
    private SurgicalCenter surgicalCenter;
    
    @ManyToOne
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;
    
    /**
     * Preismodell: RENTAL (Miete) oder OWNED (eigener OP-Saal)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false)
    private PricingModel pricingModel;
    
    /**
     * Für RENTAL: Fixpreis pro Zeitslot (in Euro)
     */
    @Column(name = "price_per_slot")
    private BigDecimal pricePerSlot;
    
    /**
     * Für RENTAL: Preis pro Stunde (in Euro) - Alternative zu pricePerSlot
     */
    @Column(name = "price_per_hour")
    private BigDecimal pricePerHour;
    
    /**
     * Für OWNED: Monatliche Fixkosten (in Euro)
     */
    @Column(name = "monthly_fixed_costs")
    private BigDecimal monthlyFixedCosts;
    
    /**
     * Für OWNED: Variable Kosten pro Behandlung (in Euro)
     */
    @Column(name = "variable_cost_per_treatment")
    private BigDecimal variableCostPerTreatment;
    
    /**
     * Gültig ab (Datum)
     */
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;
    
    /**
     * Gültig bis (Datum, null = unbegrenzt)
     */
    @Column(name = "valid_to")
    private LocalDate validTo;
    
    /**
     * Aktiv (kann temporär deaktiviert werden)
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
```

### 1.2 Entity: `TreatmentCost` (Kosten pro Behandlung)

```java
@Entity
@Table(name = "treatment_cost")
public class TreatmentCost extends BasicEntity<Long> {
    
    @OneToOne
    @JoinColumn(name = "treatment_id", nullable = false, unique = true)
    private Treatment treatment;
    
    /**
     * Berechnete Gesamtkosten (in Euro)
     */
    @Column(name = "total_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCost;
    
    /**
     * Kostenanteil pro Patient (bei mehreren Patienten im Zeitslot)
     */
    @Column(name = "cost_per_patient", nullable = false, precision = 19, scale = 2)
    private BigDecimal costPerPatient;
    
    /**
     * Anzahl Patienten im Zeitslot zum Zeitpunkt der Berechnung
     */
    @Column(name = "patient_count_at_calculation")
    private Integer patientCountAtCalculation;
    
    /**
     * Verwendetes Preismodell zum Zeitpunkt der Berechnung
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model_used")
    private PricingModel pricingModelUsed;
    
    /**
     * Berechnungsdatum
     */
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
    
    /**
     * Berechnet von (User)
     */
    @ManyToOne
    @JoinColumn(name = "calculated_by_user_id")
    private UserAccount calculatedBy;
    
    /**
     * Notizen zur Kostenberechnung
     */
    @Column(name = "notes", length = 1000)
    private String notes;
}
```

### 1.3 Entity: `PatientCostHistory` (Kostenhistorie am Patienten)

```java
@Entity
@Table(name = "patient_cost_history")
public class PatientCostHistory extends BasicEntity<Long> {
    
    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @ManyToOne
    @JoinColumn(name = "treatment_id", nullable = false)
    private Treatment treatment;
    
    @ManyToOne
    @JoinColumn(name = "treatment_cost_id", nullable = false)
    private TreatmentCost treatmentCost;
    
    /**
     * Kostenanteil dieses Patienten (in Euro)
     */
    @Column(name = "cost_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal costAmount;
    
    /**
     * Datum der Behandlung
     */
    @Column(name = "treatment_date", nullable = false)
    private LocalDate treatmentDate;
    
    /**
     * OP-Saal
     */
    @ManyToOne
    @JoinColumn(name = "surgical_center_id")
    private SurgicalCenter surgicalCenter;
}
```

### 1.4 Enum: `PricingModel`

```java
public enum PricingModel {
    /**
     * Miete: Fixpreis pro Zeitslot oder pro Stunde
     */
    RENTAL,
    
    /**
     * Eigener OP-Saal: Laufende Kosten (monatlich) + variable Kosten
     */
    OWNED
}
```

### 1.5 Flyway Migration: `V22__cost_module.sql`

```sql
-- Cost Calculation (Preismodell pro OP-Saal)
CREATE TABLE cost_calculation (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    surgical_center_id INTEGER NOT NULL,
    institution_id BIGINT NOT NULL,
    pricing_model VARCHAR(50) NOT NULL,
    price_per_slot DECIMAL(19,2),
    price_per_hour DECIMAL(19,2),
    monthly_fixed_costs DECIMAL(19,2),
    variable_cost_per_treatment DECIMAL(19,2),
    valid_from DATE NOT NULL,
    valid_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (surgical_center_id) REFERENCES surgical_center(id) ON DELETE CASCADE,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT,
    CONSTRAINT chk_cost_calculation_model CHECK (
        (pricing_model = 'RENTAL' AND (price_per_slot IS NOT NULL OR price_per_hour IS NOT NULL))
        OR (pricing_model = 'OWNED' AND monthly_fixed_costs IS NOT NULL)
    )
);

CREATE INDEX idx_cost_calculation_surgical_center ON cost_calculation(surgical_center_id);
CREATE INDEX idx_cost_calculation_institution ON cost_calculation(institution_id);
CREATE INDEX idx_cost_calculation_valid_from ON cost_calculation(valid_from);
CREATE INDEX idx_cost_calculation_active ON cost_calculation(active);

-- Treatment Cost (Kosten pro Behandlung)
CREATE TABLE treatment_cost (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    treatment_id BIGINT NOT NULL UNIQUE,
    total_cost DECIMAL(19,2) NOT NULL,
    cost_per_patient DECIMAL(19,2) NOT NULL,
    patient_count_at_calculation INTEGER,
    pricing_model_used VARCHAR(50),
    calculated_at TIMESTAMP NOT NULL,
    calculated_by_user_id VARCHAR(255),
    notes VARCHAR(1000),
    FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON DELETE CASCADE,
    FOREIGN KEY (calculated_by_user_id) REFERENCES user_account(id) ON DELETE SET NULL
);

CREATE INDEX idx_treatment_cost_treatment ON treatment_cost(treatment_id);
CREATE INDEX idx_treatment_cost_calculated_at ON treatment_cost(calculated_at);

-- Patient Cost History (Kostenhistorie am Patienten)
CREATE TABLE patient_cost_history (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    patient_id INTEGER NOT NULL,
    treatment_id BIGINT NOT NULL,
    treatment_cost_id BIGINT NOT NULL,
    cost_amount DECIMAL(19,2) NOT NULL,
    treatment_date DATE NOT NULL,
    surgical_center_id INTEGER,
    FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE,
    FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON DELETE CASCADE,
    FOREIGN KEY (treatment_cost_id) REFERENCES treatment_cost(id) ON DELETE CASCADE,
    FOREIGN KEY (surgical_center_id) REFERENCES surgical_center(id) ON DELETE SET NULL
);

CREATE INDEX idx_patient_cost_history_patient ON patient_cost_history(patient_id);
CREATE INDEX idx_patient_cost_history_treatment ON patient_cost_history(treatment_id);
CREATE INDEX idx_patient_cost_history_treatment_date ON patient_cost_history(treatment_date);
CREATE INDEX idx_patient_cost_history_surgical_center ON patient_cost_history(surgical_center_id);
```

## 2. Repository-Layer

### 2.1 `CostCalculationRepository`

```java
public interface CostCalculationRepository extends JpaRepository<CostCalculation, Long> {
    
    /**
     * Findet aktive Preismodelle für einen OP-Saal zum gegebenen Datum
     */
    @Query("""
        SELECT c FROM CostCalculation c
        WHERE c.surgicalCenter.id = :surgicalCenterId
        AND c.institution.id = :institutionId
        AND c.active = true
        AND c.validFrom <= :date
        AND (c.validTo IS NULL OR c.validTo >= :date)
        ORDER BY c.validFrom DESC
        """)
    List<CostCalculation> findActiveBySurgicalCenterAndDate(
        @Param("surgicalCenterId") Integer surgicalCenterId,
        @Param("institutionId") Long institutionId,
        @Param("date") LocalDate date
    );
    
    /**
     * Findet alle Preismodelle für einen OP-Saal
     */
    List<CostCalculation> findBySurgicalCenterIdAndInstitutionId(
        Integer surgicalCenterId,
        Long institutionId
    );
}
```

### 2.2 `TreatmentCostRepository`

```java
public interface TreatmentCostRepository extends JpaRepository<TreatmentCost, Long> {
    
    Optional<TreatmentCost> findByTreatmentId(Long treatmentId);
    
    /**
     * Findet alle Kosten für Behandlungen in einem Zeitraum
     */
    @Query("""
        SELECT tc FROM TreatmentCost tc
        JOIN tc.treatment t
        JOIN t.surgicalCenterTimeSlot ts
        WHERE ts.surgicalCenter.institution.id = :institutionId
        AND ts.date BETWEEN :startDate AND :endDate
        ORDER BY ts.date DESC, ts.startTime DESC
        """)
    List<TreatmentCost> findByInstitutionAndDateRange(
        @Param("institutionId") Long institutionId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * Aggregiert monatliche Kosten
     */
    @Query("""
        SELECT FUNCTION('DATE_FORMAT', ts.date, '%Y-%m') as month,
               SUM(tc.totalCost) as totalCost
        FROM TreatmentCost tc
        JOIN tc.treatment t
        JOIN t.surgicalCenterTimeSlot ts
        WHERE ts.surgicalCenter.institution.id = :institutionId
        AND ts.date >= :startDate
        GROUP BY FUNCTION('DATE_FORMAT', ts.date, '%Y-%m')
        ORDER BY month ASC
        """)
    List<Object[]> getMonthlyCosts(
        @Param("institutionId") Long institutionId,
        @Param("startDate") LocalDate startDate
    );
}
```

### 2.3 `PatientCostHistoryRepository`

```java
public interface PatientCostHistoryRepository extends JpaRepository<PatientCostHistory, Long> {
    
    /**
     * Findet Kostenhistorie für einen Patienten
     */
    @Query("""
        SELECT pch FROM PatientCostHistory pch
        WHERE pch.patient.id = :patientId
        AND pch.patient.institution.id = :institutionId
        ORDER BY pch.treatmentDate DESC
        """)
    List<PatientCostHistory> findByPatientId(
        @Param("patientId") Integer patientId,
        @Param("institutionId") Long institutionId
    );
    
    /**
     * Berechnet Gesamtkosten für einen Patienten
     */
    @Query("""
        SELECT SUM(pch.costAmount) FROM PatientCostHistory pch
        WHERE pch.patient.id = :patientId
        AND pch.patient.institution.id = :institutionId
        """)
    Optional<BigDecimal> getTotalCostsByPatientId(
        @Param("patientId") Integer patientId,
        @Param("institutionId") Long institutionId
    );
}
```

## 3. Service-Layer

### 3.1 `CostCalculationService`

```java
@Service
@Transactional
public class CostCalculationService {
    
    /**
     * Berechnet die Kosten für einen Behandlungsslot
     */
    public BigDecimal calculateCostForTimeSlot(
        SurgicalCenterTimeSlot timeSlot,
        LocalDate treatmentDate
    ) {
        // 1. Finde aktives Preismodell
        CostCalculation calculation = findActiveCalculation(
            timeSlot.getSurgicalCenter().getId(),
            treatmentDate
        );
        
        if (calculation == null) {
            return BigDecimal.ZERO;
        }
        
        // 2. Berechne je nach Modell
        return switch (calculation.getPricingModel()) {
            case RENTAL -> calculateRentalCost(calculation, timeSlot);
            case OWNED -> calculateOwnedCost(calculation, timeSlot, treatmentDate);
        };
    }
    
    private BigDecimal calculateRentalCost(
        CostCalculation calculation,
        SurgicalCenterTimeSlot timeSlot
    ) {
        if (calculation.getPricePerSlot() != null) {
            return calculation.getPricePerSlot();
        }
        
        if (calculation.getPricePerHour() != null) {
            long hours = ChronoUnit.HOURS.between(
                timeSlot.getStartTime(),
                timeSlot.getEndTime()
            );
            return calculation.getPricePerHour()
                .multiply(BigDecimal.valueOf(hours));
        }
        
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculateOwnedCost(
        CostCalculation calculation,
        SurgicalCenterTimeSlot timeSlot,
        LocalDate treatmentDate
    ) {
        // 1. Monatliche Fixkosten auf Behandlungen im Monat aufteilen
        int treatmentsInMonth = countTreatmentsInMonth(
            timeSlot.getSurgicalCenter().getId(),
            treatmentDate
        );
        
        BigDecimal monthlyCostPerTreatment = treatmentsInMonth > 0
            ? calculation.getMonthlyFixedCosts()
                .divide(BigDecimal.valueOf(treatmentsInMonth), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        // 2. Variable Kosten hinzufügen
        BigDecimal variableCost = calculation.getVariableCostPerTreatment() != null
            ? calculation.getVariableCostPerTreatment()
            : BigDecimal.ZERO;
        
        return monthlyCostPerTreatment.add(variableCost);
    }
}
```

### 3.2 `TreatmentCostService`

```java
@Service
@Transactional
public class TreatmentCostService {
    
    /**
     * Berechnet und speichert Kosten für eine Behandlung
     */
    public TreatmentCost calculateAndSaveTreatmentCost(
        Treatment treatment,
        UserAccount calculatedBy
    ) {
        // 1. Berechne Gesamtkosten für den Zeitslot
        BigDecimal totalCost = costCalculationService.calculateCostForTimeSlot(
            treatment.getSurgicalCenterTimeSlot(),
            treatment.getSurgicalCenterTimeSlot().getDate()
        );
        
        // 2. Zähle Patienten im Zeitslot
        int patientCount = countPatientsInTimeSlot(
            treatment.getSurgicalCenterTimeSlot()
        );
        
        // 3. Berechne Kostenanteil pro Patient
        BigDecimal costPerPatient = patientCount > 0
            ? totalCost.divide(BigDecimal.valueOf(patientCount), 2, RoundingMode.HALF_UP)
            : totalCost;
        
        // 4. Erstelle TreatmentCost
        TreatmentCost treatmentCost = new TreatmentCost();
        treatmentCost.setTreatment(treatment);
        treatmentCost.setTotalCost(totalCost);
        treatmentCost.setCostPerPatient(costPerPatient);
        treatmentCost.setPatientCountAtCalculation(patientCount);
        treatmentCost.setPricingModelUsed(/* aus CostCalculation */);
        treatmentCost.setCalculatedAt(LocalDateTime.now());
        treatmentCost.setCalculatedBy(calculatedBy);
        
        // 5. Speichere
        treatmentCost = treatmentCostRepository.save(treatmentCost);
        
        // 6. Erstelle PatientCostHistory-Einträge
        createPatientCostHistory(treatment, treatmentCost, costPerPatient);
        
        return treatmentCost;
    }
}
```

## 4. UI-Komponenten

### 4.1 `CostCalculationDialog` (Preismodell-Verwaltung)

- **Ort**: Settings → OP-Saal-Verwaltung
- **Zweck**: Preismodell pro OP-Saal konfigurieren
- **Features**:
  - Auswahl: RENTAL oder OWNED
  - Felder je nach Modell (pricePerSlot, pricePerHour, monthlyFixedCosts, etc.)
  - Gültigkeitszeitraum (validFrom, validTo)
  - Aktiv/Inaktiv Toggle

### 4.2 `TreatmentCostView` (Kostenübersicht am Behandlungsslot)

- **Ort**: TreatmentDetailDialog oder TimeSlotSummary
- **Zweck**: Kosten für einen Behandlungsslot anzeigen
- **Features**:
  - Gesamtkosten des Zeitslots
  - Kostenanteil pro Patient
  - Anzahl Patienten
  - Verwendetes Preismodell
  - Button: "Kosten berechnen" (wenn noch nicht berechnet)

### 4.3 `PatientCostHistoryTab` (Kostenhistorie am Patienten)

- **Ort**: PatientDialog → Neuer Tab "Kostenhistorie"
- **Zweck**: Alle Kosten eines Patienten anzeigen
- **Features**:
  - Grid mit allen Behandlungen und Kosten
  - Spalten: Datum, OP-Saal, Kosten, Behandlung
  - Gesamtsumme
  - Filter nach Zeitraum

### 4.4 `CostAnalyticsView` (Analytics-View)

- **Ort**: Analytics → Neuer Tab "Kosten"
- **Zweck**: Monatliche Kostenverläufe visualisieren
- **Features**:
  - Liniendiagramm: Monatliche Kosten (ähnlich TreatmentsOverTimeView)
  - Toggle: Monat/Jahr
  - Optional: Kosten nach OP-Saal aufgeteilt

### 4.5 Feature Flag Integration

- **FeatureFlagsTab**: "COST_MODULE" Feature hinzufügen
- **FeatureFlagService.initializeDefaultFeatures()**: COST_MODULE initialisieren
- **UI-Checks**: Alle Kosten-Views nur anzeigen, wenn Feature aktiviert

## 5. Integration Points

### 5.1 TreatmentDetailDialog

- Kostenübersicht als zusätzlicher Abschnitt einfügen
- Nur anzeigen, wenn COST_MODULE aktiviert

### 5.2 PatientDialog

- Neuer Tab "Kostenhistorie" hinzufügen
- Nur anzeigen, wenn COST_MODULE aktiviert

### 5.3 AnalyticsOverviewView

- Neuer Tab "Kosten" hinzufügen
- Nur anzeigen, wenn COST_MODULE aktiviert

### 5.4 SurgicalCenterMainView

- Button "Preismodell konfigurieren" hinzufügen
- Öffnet CostCalculationDialog
- Nur anzeigen, wenn COST_MODULE aktiviert

## 6. Implementierungsreihenfolge

1. **Phase 1: Datenmodell & Migration**
   - Entities erstellen
   - Flyway Migration V22__cost_module.sql
   - Repositories erstellen

2. **Phase 2: Service-Layer**
   - CostCalculationService
   - TreatmentCostService
   - PatientCostService

3. **Phase 3: Feature Flag**
   - COST_MODULE in FeatureFlagService.initializeDefaultFeatures()
   - FeatureFlagsTab aktualisieren

4. **Phase 4: UI - Preismodell-Verwaltung**
   - CostCalculationDialog
   - Integration in SurgicalCenterMainView

5. **Phase 5: UI - Behandlungsslot**
   - TreatmentCostView
   - Integration in TreatmentDetailDialog

6. **Phase 6: UI - Patient**
   - PatientCostHistoryTab
   - Integration in PatientDialog

7. **Phase 7: UI - Analytics**
   - CostAnalyticsView
   - Integration in AnalyticsOverviewView

## 7. Tests

- **Unit Tests**: CostCalculationService (verschiedene Preismodelle)
- **Unit Tests**: TreatmentCostService (Kostenberechnung)
- **Integration Tests**: Repository-Queries
- **UI Tests**: Feature Flag Checks

## 8. Sicherheitsaspekte

- InstitutionContext: Alle Queries filtern nach Institution
- Berechtigungen: Nur berechtigte User können Preismodelle konfigurieren
- Datenisolation: Institutionen sehen nur eigene Kosten

