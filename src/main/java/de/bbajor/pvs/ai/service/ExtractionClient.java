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
                // Mask PII in logs (DSGVO compliance)
                String maskedFirstName = de.bbajor.pvs.base.util.PiiMasker.maskName(patient.getFirstName());
                String maskedLastName = de.bbajor.pvs.base.util.PiiMasker.maskName(patient.getLastName());
                String maskedBirth = patient.getBirth() != null 
                        ? de.bbajor.pvs.base.util.PiiMasker.maskBirthDate(patient.getBirth().toString())
                        : "null";
                String maskedInsurance = patient.getInsuranceNumber() != null
                        ? de.bbajor.pvs.base.util.PiiMasker.maskInsuranceNumber(patient.getInsuranceNumber())
                        : "null";
                
                LOG.info("Extracted patient - Name: {} {}, Birth: {}, Insurance: {}", 
                        maskedFirstName, maskedLastName, maskedBirth, maskedInsurance);
                if (patient.getAddress() != null) {
                    LOG.info("Address extracted - Postal Code: {}, City: {}", 
                            patient.getAddress().getPostalCode(),
                            patient.getAddress().getCity());
                }
                LOG.debug("Insurance Number: {}", maskedInsurance);
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

