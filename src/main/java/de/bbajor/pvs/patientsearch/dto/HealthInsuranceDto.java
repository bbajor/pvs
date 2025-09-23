package de.bbajor.pvs.patientsearch.dto;

import java.time.LocalDate;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class HealthInsuranceDto {

    private Integer id;
    private Long version;
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
        return costCarrierName == null ? "Name n.a." : costCarrierName.trim();
    }
}
