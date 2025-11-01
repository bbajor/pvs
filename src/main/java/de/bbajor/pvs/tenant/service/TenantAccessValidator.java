package de.bbajor.pvs.tenant.service;

import de.bbajor.pvs.tenant.audit.TenantAuditLogger;
import de.bbajor.pvs.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates tenant access and prevents cross-tenant data leakage.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TenantAccessValidator {

    private final TenantAuditLogger auditLogger;

    /**
     * Validate that the entity belongs to the current tenant.
     * 
     * @param entityTenantId the tenant ID of the entity
     * @param entityType the type of entity (for logging)
     * @param entityId the entity ID (for logging)
     * @throws TenantAccessViolationException if access is denied
     */
    public void validateTenantAccess(Long entityTenantId, String entityType, Object entityId) {
        Long currentTenantId = TenantContext.getTenantId();
        
        if (currentTenantId == null) {
            // No tenant context - might be super admin or system operation
            log.warn("No tenant context set for access to {} with ID {}", entityType, entityId);
            return;
        }

        if (entityTenantId == null) {
            // Entity has no tenant (e.g., system-wide data)
            log.debug("Accessing tenant-less {} with ID {} from tenant {}", 
                    entityType, entityId, currentTenantId);
            return;
        }

        if (!entityTenantId.equals(currentTenantId)) {
            log.error("SECURITY: Cross-tenant access attempt! Tenant {} tried to access {} {} belonging to tenant {}",
                    currentTenantId, entityType, entityId, entityTenantId);
            
            // Audit log the security violation
            auditLogger.logAccessDenied(entityType, entityId, currentTenantId, entityTenantId);
            
            throw new TenantAccessViolationException(
                    String.format("Access denied: %s %s does not belong to your tenant", entityType, entityId));
        }

        log.debug("Validated tenant access for {} {} by tenant {}", entityType, entityId, currentTenantId);
        auditLogger.logAccess(entityType, entityId, currentTenantId);
    }

    /**
     * Check if the current tenant matches the given tenant ID.
     * 
     * @param tenantId the tenant ID to check
     * @return true if matches or no tenant context set
     */
    public boolean isCurrentTenant(Long tenantId) {
        Long currentTenantId = TenantContext.getTenantId();
        return currentTenantId == null || currentTenantId.equals(tenantId);
    }

    /**
     * Get the current tenant ID or throw exception if not set.
     * 
     * @return the current tenant ID
     * @throws IllegalStateException if no tenant context is set
     */
    public Long requireCurrentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context set. User must be authenticated.");
        }
        return tenantId;
    }

    /**
     * Check if current user is in a tenant context.
     * 
     * @return true if tenant context is set
     */
    public boolean hasTenantContext() {
        return TenantContext.getTenantId() != null;
    }
}
