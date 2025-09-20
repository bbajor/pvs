package de.bbajor.pvs.base.domain;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
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