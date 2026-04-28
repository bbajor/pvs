package de.bbajor.pvs.institution.service;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.model.InstitutionFeature;
import de.bbajor.pvs.institution.repository.InstitutionFeatureRepository;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing feature flags per institution.
 * Features are disabled by default and can be enabled by SuperAdmin.
 */
@Service
public class FeatureFlagService {

    private final InstitutionFeatureRepository featureRepository;
    private final InstitutionRepository institutionRepository;

    public FeatureFlagService(
            InstitutionFeatureRepository featureRepository,
            InstitutionRepository institutionRepository) {
        this.featureRepository = featureRepository;
        this.institutionRepository = institutionRepository;
    }

    /**
     * Check if a feature is enabled for the current institution.
     * Returns false if no feature flag exists (default: disabled).
     */
    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(String featureKey) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return false; // No institution context = feature disabled
        }

        return featureRepository.isFeatureEnabled(institutionId, featureKey)
                .orElse(false); // Default: disabled
    }

    /**
     * Check if a feature is enabled for a specific institution.
     */
    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(Long institutionId, String featureKey) {
        if (institutionId == null) {
            return false;
        }

        return featureRepository.isFeatureEnabled(institutionId, featureKey)
                .orElse(false); // Default: disabled
    }

    /**
     * Get all features for an institution.
     */
    @Transactional(readOnly = true)
    public List<InstitutionFeature> getFeaturesForInstitution(Long institutionId) {
        return featureRepository.findByInstitutionId(institutionId);
    }

    /**
     * Get a specific feature for an institution.
     */
    @Transactional(readOnly = true)
    public Optional<InstitutionFeature> getFeature(Long institutionId, String featureKey) {
        return featureRepository.findByInstitutionIdAndFeatureKey(institutionId, featureKey);
    }

    /**
     * Enable or disable a feature for an institution.
     * Creates the feature flag if it doesn't exist.
     */
    @Transactional
    public InstitutionFeature setFeatureEnabled(Long institutionId, String featureKey, 
            String featureName, String description, boolean enabled, boolean beta) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("Institution not found: " + institutionId));

        Optional<InstitutionFeature> existing = featureRepository.findByInstitutionIdAndFeatureKey(institutionId, featureKey);
        
        InstitutionFeature feature;
        if (existing.isPresent()) {
            feature = existing.get();
            feature.setEnabled(enabled);
            feature.setBeta(beta);
            if (featureName != null) {
                feature.setFeatureName(featureName);
            }
            if (description != null) {
                feature.setDescription(description);
            }
        } else {
            feature = new InstitutionFeature();
            feature.setInstitution(institution);
            feature.setFeatureKey(featureKey);
            feature.setFeatureName(featureName != null ? featureName : featureKey);
            feature.setDescription(description);
            feature.setEnabled(enabled);
            feature.setBeta(beta);
        }

        return featureRepository.save(feature);
    }

    /**
     * Initialize default features for an institution (all disabled).
     */
    @Transactional
    public void initializeDefaultFeatures(Long institutionId) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("Institution not found: " + institutionId));

        // EGK Reader Feature
        if (!featureRepository.findByInstitutionIdAndFeatureKey(institutionId, "EGK_READER").isPresent()) {
            InstitutionFeature egkFeature = new InstitutionFeature();
            egkFeature.setInstitution(institution);
            egkFeature.setFeatureKey("EGK_READER");
            egkFeature.setFeatureName("Aus Gesundheitskarte einlesen");
            egkFeature.setDescription("Ermöglicht das Einlesen von Patientendaten aus der elektronischen Gesundheitskarte");
            egkFeature.setEnabled(false);
            egkFeature.setBeta(true);
            featureRepository.save(egkFeature);
        }

        // Voice Input Feature
        if (!featureRepository.findByInstitutionIdAndFeatureKey(institutionId, "VOICE_INPUT").isPresent()) {
            InstitutionFeature voiceFeature = new InstitutionFeature();
            voiceFeature.setInstitution(institution);
            voiceFeature.setFeatureKey("VOICE_INPUT");
            voiceFeature.setFeatureName("Spracheingabe");
            voiceFeature.setDescription("Ermöglicht die Eingabe von Patientendaten per Spracheingabe mit Whisper-Transkription");
            voiceFeature.setEnabled(false);
            voiceFeature.setBeta(true);
            featureRepository.save(voiceFeature);
        }
    }
}



