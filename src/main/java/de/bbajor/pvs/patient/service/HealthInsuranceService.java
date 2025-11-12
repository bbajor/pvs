package de.bbajor.pvs.patient.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.repository.HealthInsuranceRepository;

@Service
public class HealthInsuranceService {

    @Autowired
    private HealthInsuranceRepository healthInsuranceRepository;
    
    @Autowired
    private InstitutionRepository institutionRepository;

    public List<HealthInsurance> findAll() {
        return healthInsuranceRepository.findAll();
    }
    
    /**
     * Findet alle Versicherungen für die aktuelle Institution.
     */
    public List<HealthInsurance> findAllForCurrentInstitution() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return List.of();
        }
        return healthInsuranceRepository.findByInstitutionId(institutionId);
    }

    public HealthInsurance findById(HealthInsurance healthInsurance) {
        return healthInsuranceRepository.getReferenceById(healthInsurance.getId());
    }
    
    /**
     * Speichert eine Versicherung und setzt die Institution aus dem Context.
     */
    @Transactional
    public HealthInsurance save(HealthInsurance healthInsurance) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Keine Institution im Context gesetzt");
        }
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalStateException("Institution nicht gefunden: " + institutionId));
        healthInsurance.setInstitution(institution);
        return healthInsuranceRepository.save(healthInsurance);
    }
    
    /**
     * Deaktiviert eine Versicherung (soft delete).
     */
    @Transactional
    public void deactivate(HealthInsurance healthInsurance) {
        // Für jetzt: einfach löschen, später könnte man ein "active" Flag hinzufügen
        healthInsuranceRepository.delete(healthInsurance);
    }

}
