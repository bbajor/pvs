package de.bbajor.pvs.cost.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Entity für Preismodell-Konfiguration pro OP-Saal.
 * Ermöglicht verschiedene Preismodelle (Miete vs. eigene Kosten).
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "cost_calculation")
public class CostCalculation extends BasicEntity<Long> {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "surgical_center_id", nullable = false)
    private SurgicalCenter surgicalCenter;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    /**
     * Preismodell: RENTAL (Miete) oder OWNED (eigener OP-Saal)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false, length = 50)
    private PricingModel pricingModel;

    /**
     * Für RENTAL: Fixpreis pro Zeitslot (in Euro)
     */
    @Column(name = "price_per_slot", precision = 19, scale = 2)
    private BigDecimal pricePerSlot;

    /**
     * Für RENTAL: Preis pro Stunde (in Euro) - Alternative zu pricePerSlot
     */
    @Column(name = "price_per_hour", precision = 19, scale = 2)
    private BigDecimal pricePerHour;

    /**
     * Für OWNED: Monatliche Fixkosten (in Euro)
     */
    @Column(name = "monthly_fixed_costs", precision = 19, scale = 2)
    private BigDecimal monthlyFixedCosts;

    /**
     * Für OWNED: Variable Kosten pro Behandlung (in Euro)
     */
    @Column(name = "variable_cost_per_treatment", precision = 19, scale = 2)
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

