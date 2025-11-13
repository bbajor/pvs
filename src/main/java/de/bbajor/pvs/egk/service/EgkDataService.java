package de.bbajor.pvs.egk.service;

import de.bbajor.pvs.egk.api.dto.EgkDataDto;
import de.bbajor.pvs.egk.reader.EgkReader;
import de.bbajor.pvs.patient.model.Address;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service für die Verarbeitung von eGK-Daten.
 * Unterstützt sowohl eGK-Tool (lokal) als auch eGK-Agent (Client-seitig).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EgkDataService {

    private final EgkReader egkReader;

    /**
     * Verarbeitet Patientendaten aus eGK-Daten-DTO.
     */
    public Patient processPatientData(EgkDataDto egkData) {
        Patient patient = new Patient();
        
        patient.setInsuranceNumber(egkData.getVersichertenId());
        patient.setFirstName(egkData.getVorname());
        patient.setLastName(egkData.getNachname());
        patient.setBirth(egkData.getGeburtsdatum());
        patient.setGender(egkData.getGeschlecht());
        
        // Adresse verarbeiten
        if (egkData.getAdresse() != null) {
            Address address = new Address();
            address.setStreet(egkData.getAdresse().getStrasse() != null 
                    ? egkData.getAdresse().getStrasse() : "");
            address.setHouseNo(egkData.getAdresse().getHausnummer() != null 
                    ? egkData.getAdresse().getHausnummer() : "");
            
            try {
                if (egkData.getAdresse().getPostleitzahl() != null 
                        && !egkData.getAdresse().getPostleitzahl().isEmpty()) {
                    address.setPostalCode(Integer.parseInt(egkData.getAdresse().getPostleitzahl()));
                }
            } catch (NumberFormatException e) {
                log.warn("Fehler beim Parsen der Postleitzahl: {}", egkData.getAdresse().getPostleitzahl());
                address.setPostalCode(0);
            }
            
            address.setCity(egkData.getAdresse().getOrt() != null 
                    ? egkData.getAdresse().getOrt() : "");
            
            if (egkData.getAdresse().getLand() != null) {
                address.setCountry(EgkReader.toLocale(egkData.getAdresse().getLand()));
            }
            
            patient.setAddress(address);
        }
        
        return patient;
    }

    /**
     * Verarbeitet Versicherungsdaten aus eGK-Daten-DTO.
     */
    public HealthInsurance processHealthInsuranceData(EgkDataDto egkData) {
        HealthInsurance healthInsurance = new HealthInsurance();
        
        if (egkData.getVersicherungsschutz() != null) {
            healthInsurance.setInsuranceStart(egkData.getVersicherungsschutz().getBeginn());
            healthInsurance.setInsuranceType(egkData.getVersicherungsschutz().getVersichertenart());
            healthInsurance.setWop(egkData.getVersicherungsschutz().getWop());
            
            if (egkData.getVersicherungsschutz().getKostentraeger() != null) {
                var kt = egkData.getVersicherungsschutz().getKostentraeger();
                healthInsurance.setCostCarrierId(kt.getKostentraegerkennung());
                healthInsurance.setCostCarrierName(kt.getName());
                healthInsurance.setCostCarrierCountryCode(kt.getLaendercode() != null 
                        ? kt.getLaendercode() : "");
                
                if (kt.getAbrechnenderKostentraeger() != null) {
                    var akt = kt.getAbrechnenderKostentraeger();
                    healthInsurance.setBillingCarrierId(akt.getKostentraegerkennung());
                    healthInsurance.setBillingCarrierName(akt.getName());
                    healthInsurance.setBillingCarrierCountryCode(akt.getLaendercode() != null 
                            ? akt.getLaendercode() : "");
                }
            }
        }
        
        return healthInsurance;
    }
}
