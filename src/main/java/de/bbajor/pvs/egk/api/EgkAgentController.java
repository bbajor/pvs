package de.bbajor.pvs.egk.api;

import de.bbajor.pvs.egk.api.dto.EgkDataDto;
import de.bbajor.pvs.egk.api.dto.EgkDataResponse;
import de.bbajor.pvs.egk.service.EgkDataService;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST-API für eGK-Agent (Client-seitige eGK-Kartenlesung).
 * Empfängt eGK-Daten vom Client-Agent und verarbeitet sie.
 */
@RestController
@RequestMapping("/api/egk")
@RequiredArgsConstructor
@Slf4j
public class EgkAgentController {

    private final EgkDataService egkDataService;

    /**
     * Empfängt eGK-Daten vom Client-Agent und gibt Patientendaten zurück.
     * 
     * @param egkData Die eGK-Daten vom Client-Agent
     * @return Patientendaten und Versicherungsdaten
     */
    @PostMapping("/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EgkDataResponse> readEgkData(@Valid @RequestBody EgkDataDto egkData) {
        try {
            log.info("eGK-Daten empfangen vom Agent");
            
            // Verarbeite eGK-Daten
            Patient patient = egkDataService.processPatientData(egkData);
            HealthInsurance healthInsurance = egkDataService.processHealthInsuranceData(egkData);
            
            patient.setHealthInsurance(healthInsurance);
            
            EgkDataResponse response = new EgkDataResponse(patient, null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Fehler beim Verarbeiten der eGK-Daten", e);
            EgkDataResponse response = new EgkDataResponse(null, 
                    "Fehler beim Verarbeiten der eGK-Daten: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Health-Check für eGK-Agent API.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("eGK-Agent API is available");
    }
}
