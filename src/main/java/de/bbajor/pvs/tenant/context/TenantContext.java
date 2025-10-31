package de.bbajor.pvs.tenant.context;

import org.springframework.stereotype.Component;

/**
 * Thread-local storage for the current tenant.
 * Used to enforce tenant isolation across the application.
 */
@Component
public class TenantContext {

    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();

    /**
     * Set the current tenant ID for this thread.
     */
    public static void setTenantId(Long tenantId) {
        currentTenantId.set(tenantId);
    }

    /**
     * Get the current tenant ID for this thread.
     * @return the tenant ID, or null if no tenant is set
     */
    public static Long getTenantId() {
        return currentTenantId.get();
    }

    /**
     * Clear the tenant ID from the current thread.
     */
    public static void clear() {
        currentTenantId.remove();
    }

    /**
     * Check if a tenant is currently set.
     */
    public static boolean hasTenant() {
        return currentTenantId.get() != null;
    }
}
