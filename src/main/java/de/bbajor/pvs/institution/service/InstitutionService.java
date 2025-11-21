package de.bbajor.pvs.institution.service;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository institutionRepository;
    private final FeatureFlagService featureFlagService;

    /**
     * Find a institution by its code.
     */
    @Transactional(readOnly = true)
    public Optional<Institution> findByCode(String institutionCode) {
        return institutionRepository.findByInstitutionCode(institutionCode);
    }

    /**
     * Get all tenants.
     */
    @Transactional(readOnly = true)
    public List<Institution> findAll() {
        return institutionRepository.findAll();
    }

    /**
     * Get the current institution for the logged-in user.
     */
    @Transactional(readOnly = true)
    public Institution getInstitution() {
        return institutionRepository.findAll().stream().findFirst().orElse(null);
    }

    /**
     * Create a new institution with a generated institution code.
     * 
     * @param institutionName the name of the institution
     * @return the created institution
     */
    @Transactional
    public Institution createInstitution(String institutionName) {
        String institutionCode = generateInstitutionCode();
        String normalizedCode = institutionCode.replace("-", "_").toLowerCase();
        
        Institution institution = new Institution()
                .setInstitutionCode(institutionCode)
                .setInstitutionName(institutionName)
                .setActive(true)
                .setDatabaseName("pvs_inst_" + normalizedCode)
                .setContainerName("postgres-inst-" + normalizedCode);
        
        Institution saved = institutionRepository.save(institution);
        
        // Initialisiere Standard-Feature-Flags (alle deaktiviert)
        featureFlagService.initializeDefaultFeatures(saved.getId());
        
        return saved;
    }

    /**
     * Save or update a institution.
     */
    @Transactional
    public Institution save(Institution institution) {
        return institutionRepository.save(institution);
    }

    /**
     * Deactivate a institution (soft delete).
     * Inactive institutions cannot log in.
     */
    @Transactional
    public void deactivate(Long institutionId) {
        institutionRepository.findById(institutionId).ifPresent(institution -> {
            institution.setActive(false);
            institutionRepository.save(institution);
        });
    }

    /**
     * Generate a pseudorandom institution code.
     * Format: PRAX-XXXX where XXXX is a random alphanumeric string.
     */
    private String generateInstitutionCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "INST-" + uuid;
    }
}

