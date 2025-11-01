package de.bbajor.pvs.tenant.audit;

import de.bbajor.pvs.tenant.context.TenantContext;
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
public class TenantAuditLogger {

    // In-memory store for audit events (in production, use database or external logging)
    private final Map<String, AuditEvent> recentEvents = new ConcurrentHashMap<>();

    /**
     * Log a successful tenant access.
     */
    public void logAccess(String entityType, Object entityId, Long tenantId) {
        log.debug("Tenant {} accessed {} with ID {}", tenantId, entityType, entityId);
    }

    /**
     * Log a tenant access denial (cross-tenant access attempt).
     * This is a CRITICAL security event.
     */
    public void logAccessDenied(String entityType, Object entityId, Long requestedTenantId, Long actualTenantId) {
        String eventId = generateEventId(entityType, entityId, requestedTenantId);
        
        AuditEvent event = new AuditEvent(
                eventId,
                Instant.now(),
                "ACCESS_DENIED",
                requestedTenantId,
                actualTenantId,
                entityType,
                entityId.toString()
        );

        recentEvents.put(eventId, event);

        log.error("SECURITY ALERT: Tenant {} attempted to access {} {} belonging to tenant {}",
                requestedTenantId, entityType, entityId, actualTenantId);
    }

    /**
     * Log a tenant data modification.
     */
    public void logModification(String operation, String entityType, Object entityId, Long tenantId) {
        log.info("Tenant {} performed {} on {} with ID {}", tenantId, operation, entityType, entityId);
    }

    /**
     * Log a tenant login.
     */
    public void logLogin(String username, Long tenantId, boolean successful) {
        Long currentTenantId = TenantContext.getTenantId();
        
        if (successful) {
            log.info("User {} successfully logged in to tenant {}", username, tenantId);
        } else {
            log.warn("Failed login attempt for user {} on tenant {}", username, tenantId);
        }
    }

    /**
     * Log a tenant context switch (for super admins).
     */
    public void logTenantSwitch(String username, Long fromTenantId, Long toTenantId) {
        log.info("User {} switched from tenant {} to tenant {}", username, fromTenantId, toTenantId);
    }

    /**
     * Log an attempt to perform an operation without tenant context.
     */
    public void logNoTenantContext(String operation, String entityType) {
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

    private String generateEventId(String entityType, Object entityId, Long tenantId) {
        return String.format("%s-%s-%s-%d", entityType, entityId, tenantId, Instant.now().toEpochMilli());
    }

    /**
     * Audit event record.
     */
    public record AuditEvent(
            String eventId,
            Instant timestamp,
            String eventType,
            Long requestingTenantId,
            Long targetTenantId,
            String entityType,
            String entityId
    ) {}
}
