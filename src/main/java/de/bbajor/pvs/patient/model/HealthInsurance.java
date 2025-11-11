package de.bbajor.pvs.patient.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.persistence.InstitutionFilterConstants;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Filter;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Filter(name = InstitutionFilterConstants.FILTER_NAME, condition = InstitutionFilterConstants.FILTER_CONDITION)
public class HealthInsurance extends BasicEntity<Integer> {

    /**
     * The institution this health insurance belongs to.
     * <p>
     * Data isolation: Health insurances are filtered by institution.
     * This ensures that each institution only sees its own health insurance records.
     * </p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    private LocalDate insuranceStart; // Beginn der Versicherung
    private String insuranceType; // Versichertenart
    private String status; // Status der Versichertenkarte
    private String wop; // Wohnortprinzip
    private String billingCarrierCountryCode; // Länderkürzel der Versicherung
    private String billingCarrierId; // Abrechnungsstellennummer
    private String billingCarrierName; // Abrechnungsstelle
    private String costCarrierCountryCode; // Länderkürzel des Kostenträgers
    private String costCarrierId; // Kostenträgernummer
    private String costCarrierName; // Kostenträger

    @Override
    public String toString() {
        return costCarrierName != null ? costCarrierName
                : "Unbekannte Krankenkasse" + billingCarrierName != null
                        ? " (" + billingCarrierName + ")"
                        : "";
    }
}