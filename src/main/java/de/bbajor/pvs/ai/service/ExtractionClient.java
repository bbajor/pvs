package de.bbajor.pvs.ai.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.ai.extraction.ExtractionOrchestrator;
import de.bbajor.pvs.ai.extraction.ExtractionResult;
import de.bbajor.pvs.patient.model.Patient;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExtractionClient {

    private static final Logger LOG = LogManager.getLogger(ExtractionClient.class);
    private final ExtractionOrchestrator extractionOrchestrator;

    public ExtractionResult<Patient> extractPatient(String text) {
        LOG.info("=== Starting patient data extraction ===");
        LOG.info("Input text length: {} characters", text != null ? text.length() : 0);
        LOG.debug("Input text: {}", text);
        
        try {
            ExtractionResult<Patient> result = extractionOrchestrator.extract(text, Patient.class);
            
            Patient patient = result.getEntity();
            
            LOG.info("Extraction successful!");
            if (patient != null) {
                LOG.info("Extracted patient - Name: {} {}, Birth: {}, Insurance: {}", 
                        patient.getFirstName(),
                        patient.getLastName(),
                        patient.getBirth(),
                        patient.getHealthInsurance() != null ? patient.getHealthInsurance().toString() : "null");
                if (patient.getAddress() != null) {
                    LOG.info("Address: {} {}, {} {}", 
                            patient.getAddress().getStreet(),
                            patient.getAddress().getHouseNo(),
                            patient.getAddress().getPostalCode(),
                            patient.getAddress().getCity());
                }
                LOG.info("Insurance Number: {}", patient.getInsuranceNumber());
            }
            LOG.info("Confidence: {}", result.getConfidence());
            if (result.getFieldConfidences() != null) {
                LOG.debug("Field confidences: {}", result.getFieldConfidences());
            }
            
            return result;
        } catch (Exception e) {
            LOG.error("=== Extraction Error ===");
            LOG.error("Error message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract patient data: " + e.getMessage(), e);
        }
    }
}

