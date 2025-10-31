package de.bbajor.pvs.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Security Event Logger für Audit-Trail.
 * 
 * Loggt Security-relevante Events:
 * - LOGIN_SUCCESS, LOGIN_FAILED
 * - MFA_VERIFICATION_SUCCESS, MFA_VERIFICATION_FAILED
 * - RATE_LIMIT_VIOLATION
 * - ACCOUNT_LOCKOUT
 * - PASSWORD_CHANGE
 * - PERMISSION_DENIED
 * 
 * Alle Events werden als JSON geloggt (via Logback + LogstashEncoder).
 * 
 * @author Agent 5 - Monitoring & Backup
 * @since 2025-10-30
 */
@Service
public class SecurityEventLogger {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventLogger.class);

    /**
     * Loggt ein Security-Event.
     * 
     * @param eventType Event-Typ (z.B. "LOGIN_SUCCESS")
     * @param userId User-ID (optional)
     * @param username Username
     * @param ipAddress IP-Adresse
     * @param userAgent User-Agent
     * @param details Zusätzliche Details
     */
    public void logEvent(String eventType, String userId, String username, String ipAddress, String userAgent, Map<String, String> details) {
        try {
            // MDC für strukturiertes Logging
            MDC.put("eventType", eventType);
            if (userId != null) MDC.put("userId", userId);
            if (username != null) MDC.put("username", username);
            if (ipAddress != null) MDC.put("ipAddress", ipAddress);
            if (userAgent != null) MDC.put("userAgent", userAgent);
            
            // Alle Details als zusätzliche MDC-Felder
            if (details != null) {
                details.forEach(MDC::put);
            }
            
            // Log-Event
            log.info("SECURITY_EVENT: {} - User: {} - IP: {}", eventType, username, ipAddress);
            
        } finally {
            // MDC cleanup
            MDC.clear();
        }
    }

    /**
     * Loggt erfolgreichen Login.
     */
    public void logLoginSuccess(String userId, String username, String ipAddress, String userAgent) {
        logEvent("LOGIN_SUCCESS", userId, username, ipAddress, userAgent, null);
    }

    /**
     * Loggt fehlgeschlagenen Login.
     */
    public void logLoginFailed(String username, String ipAddress, String userAgent, String reason) {
        Map<String, String> details = new HashMap<>();
        details.put("reason", reason);
        logEvent("LOGIN_FAILED", null, username, ipAddress, userAgent, details);
    }

    /**
     * Loggt MFA-Verification Success.
     */
    public void logMfaVerificationSuccess(String userId, String username, String ipAddress) {
        logEvent("MFA_VERIFICATION_SUCCESS", userId, username, ipAddress, null, null);
    }

    /**
     * Loggt MFA-Verification Failed.
     */
    public void logMfaVerificationFailed(String userId, String username, String ipAddress) {
        logEvent("MFA_VERIFICATION_FAILED", userId, username, ipAddress, null, null);
    }

    /**
     * Loggt Rate-Limit-Violation.
     */
    public void logRateLimitViolation(String username, String ipAddress, String endpoint) {
        Map<String, String> details = new HashMap<>();
        details.put("endpoint", endpoint);
        logEvent("RATE_LIMIT_VIOLATION", null, username, ipAddress, null, details);
    }

    /**
     * Loggt Account-Lockout.
     */
    public void logAccountLockout(String username, String ipAddress, int failedAttempts) {
        Map<String, String> details = new HashMap<>();
        details.put("failedAttempts", String.valueOf(failedAttempts));
        logEvent("ACCOUNT_LOCKOUT", null, username, ipAddress, null, details);
    }

    /**
     * Loggt Permission-Denied.
     */
    public void logPermissionDenied(String userId, String username, String resource) {
        Map<String, String> details = new HashMap<>();
        details.put("resource", resource);
        logEvent("PERMISSION_DENIED", userId, username, null, null, details);
    }
}
