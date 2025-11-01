package de.bbajor.pvs.tenant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * Base repository interface for tenant-aware entities.
 * Provides tenant-scoped query methods.
 * 
 * @param <T> The entity type
 * @param <ID> The ID type
 */
@NoRepositoryBean
public interface TenantAwareRepository<T, ID> extends JpaRepository<T, ID> {

    /**
     * Find all entities for the given tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of entities
     */
    List<T> findByTenantId(Long tenantId);

    /**
     * Find entity by ID and tenant.
     * This ensures cross-tenant access is prevented.
     * 
     * @param id the entity ID
     * @param tenantId the tenant ID
     * @return Optional containing the entity if found and belongs to tenant
     */
    Optional<T> findByIdAndTenantId(ID id, Long tenantId);

    /**
     * Count entities for the given tenant.
     * 
     * @param tenantId the tenant ID
     * @return count of entities
     */
    long countByTenantId(Long tenantId);

    /**
     * Check if entity exists for the given tenant.
     * 
     * @param id the entity ID
     * @param tenantId the tenant ID
     * @return true if exists and belongs to tenant
     */
    boolean existsByIdAndTenantId(ID id, Long tenantId);

    /**
     * Delete all entities for a given tenant.
     * USE WITH CAUTION - for tenant deletion/cleanup only.
     * 
     * @param tenantId the tenant ID
     */
    void deleteByTenantId(Long tenantId);
}
