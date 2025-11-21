package de.bbajor.pvs.institution.repository;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.model.InstitutionFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for InstitutionFeature entities.
 */
public interface InstitutionFeatureRepository extends JpaRepository<InstitutionFeature, Long> {

    /**
     * Find all features for an institution.
     */
    @Query("SELECT f FROM InstitutionFeature f WHERE f.institution.id = :institutionId")
    List<InstitutionFeature> findByInstitutionId(@Param("institutionId") Long institutionId);

    /**
     * Find a specific feature for an institution.
     */
    @Query("SELECT f FROM InstitutionFeature f WHERE f.institution.id = :institutionId AND f.featureKey = :featureKey")
    Optional<InstitutionFeature> findByInstitutionIdAndFeatureKey(
            @Param("institutionId") Long institutionId,
            @Param("featureKey") String featureKey);

    /**
     * Check if a feature is enabled for an institution.
     */
    @Query("SELECT f.enabled FROM InstitutionFeature f WHERE f.institution.id = :institutionId AND f.featureKey = :featureKey")
    Optional<Boolean> isFeatureEnabled(
            @Param("institutionId") Long institutionId,
            @Param("featureKey") String featureKey);

    /**
     * Find all enabled features for an institution.
     */
    @Query("SELECT f FROM InstitutionFeature f WHERE f.institution.id = :institutionId AND f.enabled = true")
    List<InstitutionFeature> findEnabledFeaturesByInstitutionId(@Param("institutionId") Long institutionId);
}

