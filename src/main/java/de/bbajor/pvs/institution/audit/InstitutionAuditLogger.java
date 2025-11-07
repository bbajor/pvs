package de.bbajor.pvs.institution.audit;

import de.bbajor.pvs.institution.context.InstitutionContext;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Audit logger for tenant-related security events.
 * Logs and tracks potential security violations.
 */
@Component
@Slf4j
public class InstitutionAuditLogger {

    // In-memory store for audit events (in production, use database or external logging)
    private final Map<String, AuditEvent> recentEvents = new ConcurrentHashMap<>();

    /**
     * Log a successful tenant access.
     */
    public void logAccess(String entityType, Object entityId, Long institutionId) {
        log.debug("Institution {} accessed {} with ID {}", institutionId, entityType, entityId);
    }

    /**
     * Log a tenant access denial (cross-tenant access attempt).
     * This is a CRITICAL security event.
     */
    public void logAccessDenied(String entityType, Object entityId, Long requestedInstitutionId, Long actualInstitutionId) {
        String eventId = generateEventId(entityType, entityId, requestedInstitutionId);
        
        AuditEvent event = new AuditEvent(
                eventId,
                Instant.now(),
                "ACCESS_DENIED",
                requestedInstitutionId,
                actualInstitutionId,
                entityType,
                entityId.toString()
        );

        recentEvents.put(eventId, event);

        log.error("SECURITY ALERT: Institution {} attempted to access {} {} belonging to institution {}",
                requestedInstitutionId, entityType, entityId, actualInstitutionId);
    }

    /**
     * Log a tenant data modification.
     */
    public void logModification(String operation, String entityType, Object entityId, Long institutionId) {
        log.info("Institution {} performed {} on {} with ID {}", institutionId, operation, entityType, entityId);
    }

    /**
     * Log a tenant login.
     */
    public void logLogin(String username, Long institutionId, boolean successful) {
        Long currentInstitutionId = InstitutionContext.getInstitutionId();
        
        if (successful) {
            log.info("User {} successfully logged in to institution {}", username, institutionId);
        } else {
            log.warn("Failed login attempt for user {} on institution {}", username, institutionId);
        }
    }

    /**
     * Log a tenant context switch (for super admins).
     */
    public void logInstitutionSwitch(String username, Long fromInstitutionId, Long toInstitutionId) {
        log.info("User {} switched from institution {} to institution {}", username, fromInstitutionId, toInstitutionId);
    }

    /**
     * Log an attempt to perform an operation without tenant context.
     */
    public void logNoInstitutionContext(String operation, String entityType) {
        log.warn("Operation {} on {} attempted without tenant context", operation, entityType);
    }

    /**
     * Get recent security events for monitoring.
     * In production, this would query a database or external logging system.
     */
    public Map<String, AuditEvent> getRecentEvents() {
        return new ConcurrentHashMap<>(recentEvents);
    }

    /**
     * Clear old audit events (cleanup).
     * In production, this would be handled by log rotation or database cleanup.
     */
    public void clearOldEvents() {
        Instant threshold = Instant.now().minusSeconds(3600); // Keep last hour
        recentEvents.entrySet().removeIf(entry -> entry.getValue().timestamp.isBefore(threshold));
    }

    private String generateEventId(String entityType, Object entityId, Long institutionId) {
        return String.format("%s-%s-%s-%d", entityType, entityId, institutionId, Instant.now().toEpochMilli());
    }

    /**
     * Audit event record.
     */
    public record AuditEvent(
            String eventId,
            Instant timestamp,
            String eventType,
            Long requestingInstitutionId,
            Long targetInstitutionId,
            String entityType,
            String entityId
    ) {}
}

