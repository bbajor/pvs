package de.bbajor.pvs.patient.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
public class HealthInsurance extends BasicEntity<Integer> {

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
}