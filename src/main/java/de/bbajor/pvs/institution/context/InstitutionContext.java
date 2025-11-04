package de.bbajor.pvs.institution.context;

import org.springframework.stereotype.Component;

/**
 * Thread-local storage for the current institution/tenant ID.
 * <p>
 * Used to enforce data isolation across the application.
 * All data filtering is done via institution.
 * </p>
 * <p>
 * Note: Despite the name "InstitutionContext", this actually stores the Institution ID.
 * The name is kept for backward compatibility during migration.
 * </p>
 * <p>
 * Data isolation hierarchy:
 * - Patient → Location → Institution (primary path)
 * - All queries filter by institution ID to ensure institutions cannot see each other's data
 * </p>
 */
@Component
public class InstitutionContext {

    private static final ThreadLocal<Long> currentInstitutionId = new ThreadLocal<>();

    /**
     * Set the current institution ID for this thread.
     * <p>
     * Note: Despite the parameter name "institutionId", this is actually the Institution ID.
     * </p>
     */
    public static void setInstitutionId(Long institutionId) {
        currentInstitutionId.set(institutionId);
    }

    /**
     * Get the current institution ID for this thread.
     * <p>
     * Note: Despite the return name "institutionId", this is actually the Institution ID.
     * </p>
     * @return the institution ID, or null if no institution is set
     */
    public static Long getInstitutionId() {
        return currentInstitutionId.get();
    }

    /**
     * Clear the institution ID from the current thread.
     */
    public static void clear() {
        currentInstitutionId.remove();
    }

    /**
     * Check if a institution is currently set.
     */
    public static boolean hasInstitution() {
        return currentInstitutionId.get() != null;
    }
}

