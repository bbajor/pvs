package de.bbajor.pvs.cost.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.security.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Entity für Kosten pro Behandlung.
 * Speichert die berechneten Kosten für einen Behandlungsslot.
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "treatment_cost")
public class TreatmentCost extends BasicEntity<Long> {

    @OneToOne(fetch = FetchType.EAGER)
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
    @Column(name = "pricing_model_used", length = 50)
    private PricingModel pricingModelUsed;

    /**
     * Berechnungsdatum
     */
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    /**
     * Berechnet von (User)
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "calculated_by_user_id")
    private UserAccount calculatedBy;

    /**
     * Notizen zur Kostenberechnung
     */
    @Column(name = "notes", length = 1000)
    private String notes;
}

