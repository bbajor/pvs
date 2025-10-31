package de.bbajor.pvs.tenant.repository;

import de.bbajor.pvs.tenant.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /**
     * Find a tenant by its unique tenant code.
     * 
     * @param tenantCode the tenant code
     * @return Optional containing the tenant if found
     */
    Optional<Tenant> findByTenantCode(String tenantCode);

    /**
     * Check if a tenant with the given code exists.
     * 
     * @param tenantCode the tenant code
     * @return true if exists, false otherwise
     */
    boolean existsByTenantCode(String tenantCode);
}
