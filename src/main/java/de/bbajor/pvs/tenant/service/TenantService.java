package de.bbajor.pvs.tenant.service;

import de.bbajor.pvs.tenant.model.Tenant;
import de.bbajor.pvs.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    /**
     * Find a tenant by its code.
     */
    @Transactional(readOnly = true)
    public Optional<Tenant> findByCode(String tenantCode) {
        return tenantRepository.findByTenantCode(tenantCode);
    }

    /**
     * Get all tenants.
     */
    @Transactional(readOnly = true)
    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    /**
     * Create a new tenant with a generated tenant code.
     * 
     * @param tenantName the name of the tenant
     * @return the created tenant
     */
    @Transactional
    public Tenant createTenant(String tenantName) {
        String tenantCode = generateTenantCode();
        
        Tenant tenant = new Tenant()
                .setTenantCode(tenantCode)
                .setTenantName(tenantName)
                .setActive(true);
        
        return tenantRepository.save(tenant);
    }

    /**
     * Save or update a tenant.
     */
    @Transactional
    public Tenant save(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    /**
     * Deactivate a tenant (soft delete).
     * Inactive tenants cannot log in.
     */
    @Transactional
    public void deactivate(Long tenantId) {
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            tenant.setActive(false);
            tenantRepository.save(tenant);
        });
    }

    /**
     * Generate a pseudorandom tenant code.
     * Format: PRAX-XXXX where XXXX is a random alphanumeric string.
     */
    private String generateTenantCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "PRAX-" + uuid;
    }
}
