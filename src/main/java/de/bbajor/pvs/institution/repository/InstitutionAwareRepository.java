package de.bbajor.pvs.institution.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * Base repository interface for tenant-aware entities.
 * Provides institution-scoped query methods.
 * 
 * @param <T> The entity type
 * @param <ID> The ID type
 */
@NoRepositoryBean
public interface InstitutionAwareRepository<T, ID> extends JpaRepository<T, ID> {

    /**
     * Find all entities for the given tenant.
     * 
     * @param institutionId the institution ID
     * @return list of entities
     */
    List<T> findByInstitutionId(Long institutionId);

    /**
     * Find entity by ID and institution.
     * This ensures cross-tenant access is prevented.
     * 
     * @param id the entity ID
     * @param institutionId the institution ID
     * @return Optional containing the entity if found and belongs to institution
     */
    Optional<T> findByIdAndInstitutionId(ID id, Long institutionId);

    /**
     * Count entities for the given institution.
     * 
     * @param institutionId the institution ID
     * @return count of entities
     */
    long countByInstitutionId(Long institutionId);

    /**
     * Check if entity exists for the given institution.
     * 
     * @param id the entity ID
     * @param institutionId the institution ID
     * @return true if exists and belongs to tenant
     */
    boolean existsByIdAndInstitutionId(ID id, Long institutionId);

    /**
     * Delete all entities for a given tenant.
     * USE WITH CAUTION - for institution deletion/cleanup only.
     * 
     * @param institutionId the institution ID
     */
    void deleteByInstitutionId(Long institutionId);
}

