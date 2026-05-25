package de.bbajor.pvs.institution.service;

import de.bbajor.pvs.institution.audit.InstitutionAuditLogger;
import de.bbajor.pvs.institution.context.InstitutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

/**
 * Validates institution access and prevents cross-institution data leakage.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InstitutionAccessValidator {

    private final InstitutionAuditLogger auditLogger;

    /**
     * Validate that the entity belongs to the current institution.
     * 
     * @param entityInstitutionId the institution ID of the entity
     * @param entityType the type of entity (for logging)
     * @param entityId the entity ID (for logging)
     * @throws InstitutionAccessViolationException if access is denied
     */
    public void validateInstitutionAccess(Long entityInstitutionId, String entityType, Object entityId) {
        Long currentInstitutionId = InstitutionContext.getInstitutionId();
        
        if (currentInstitutionId == null) {
            if (entityInstitutionId == null) {
                log.debug("Accessing institution-less {} with ID {} without tenant context", entityType, entityId);
                return;
            }
            log.error("SECURITY: Access to institution-bound {} {} without tenant context", entityType, entityId);
            auditLogger.logNoInstitutionContext("access", entityType);
            throw new InstitutionAccessViolationException(
                    String.format("Access denied: missing institution context for %s %s", entityType, entityId));
        }

        if (entityInstitutionId == null) {
            // Entity has no institution (e.g., system-wide data)
            log.debug("Accessing institution-less {} with ID {} from institution {}", 
                    entityType, entityId, currentInstitutionId);
            return;
        }

        if (!entityInstitutionId.equals(currentInstitutionId)) {
            log.error("SECURITY: Cross-institution access attempt! Institution {} tried to access {} {} belonging to institution {}",
                    currentInstitutionId, entityType, entityId, entityInstitutionId);
            
            // Audit log the security violation
            auditLogger.logAccessDenied(entityType, entityId, currentInstitutionId, entityInstitutionId);
            
            throw new InstitutionAccessViolationException(
                    String.format("Access denied: %s %s does not belong to your institution", entityType, entityId));
        }

        log.debug("Validated institution access for {} {} by institution {}", entityType, entityId, currentInstitutionId);
        auditLogger.logAccess(entityType, entityId, currentInstitutionId);
    }

    /**
     * Check if the current institution matches the given institution ID.
     * 
     * @param institutionId the institution ID to check
     * @return true if matches or no institution context set
     */
    public boolean isCurrentInstitution(Long institutionId) {
        Long currentInstitutionId = InstitutionContext.getInstitutionId();
        return currentInstitutionId != null && currentInstitutionId.equals(institutionId);
    }

    /**
     * Get the current institution ID or throw exception if not set.
     * 
     * @return the current institution ID
     * @throws IllegalStateException if no institution context is set
     */
    public Long requireCurrentInstitutionId() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("No institution context set. User must be authenticated.");
        }
        return institutionId;
    }

    /**
     * Check if current user is in a institution context.
     * 
     * @return true if institution context is set
     */
    public boolean hasInstitutionContext() {
        return InstitutionContext.getInstitutionId() != null;
    }
}

