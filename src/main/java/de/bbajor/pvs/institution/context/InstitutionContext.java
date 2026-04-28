package de.bbajor.pvs.institution.context;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.institution.service.CurrentInstitutionService;

/**
 * Backward-compatible static access to the active institution id.
 * <p>
 * Delegates to {@link CurrentInstitutionService}; prefer injecting that service in new code.
 * </p>
 */
@Component
public class InstitutionContext {

    private static volatile CurrentInstitutionService delegate;

    public InstitutionContext(CurrentInstitutionService currentInstitutionService) {
        InstitutionContext.delegate = currentInstitutionService;
    }

    public static void setInstitutionId(Long institutionId) {
        if (delegate == null) {
            return;
        }
        delegate.setThreadLocalInstitutionId(institutionId);
    }

    public static Long getInstitutionId() {
        return delegate == null ? null : delegate.getCurrentInstitutionId().orElse(null);
    }

    public static void clear() {
        if (delegate != null) {
            delegate.clearExecutionOverride();
        }
    }

    public static boolean hasInstitution() {
        return delegate != null && delegate.hasInstitution();
    }

    public static Long getRequiredInstitutionId() {
        if (delegate == null) {
            throw new IllegalStateException("CurrentInstitutionService not initialized");
        }
        return delegate.getRequiredInstitutionId();
    }

    public static void runWithInstitutionId(Long institutionId, Runnable action) {
        if (delegate == null) {
            throw new IllegalStateException("CurrentInstitutionService not initialized");
        }
        delegate.runWithInstitutionId(institutionId, action);
    }
}
