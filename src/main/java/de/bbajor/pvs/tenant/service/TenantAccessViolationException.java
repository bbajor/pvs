package de.bbajor.pvs.tenant.service;

/**
 * Exception thrown when a cross-tenant access violation is detected.
 */
public class TenantAccessViolationException extends RuntimeException {

    public TenantAccessViolationException(String message) {
        super(message);
    }

    public TenantAccessViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
