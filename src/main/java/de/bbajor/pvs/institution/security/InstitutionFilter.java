package de.bbajor.pvs.institution.security;

import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.persistence.InstitutionFilterConstants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

/**
 * Utility to enable Hibernate institution filter for queries.
 * This ensures that all queries automatically filter by institution_id.
 */
@Component
public class InstitutionFilter {

    /**
     * Enable the institution filter on the given EntityManager.
     * This should be called before executing queries that need institution isolation.
     */
    public static void enableFilter(EntityManager entityManager) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            try {
                Session session = entityManager.unwrap(Session.class);
                Filter filter = session.enableFilter(InstitutionFilterConstants.FILTER_NAME);
                filter.setParameter(InstitutionFilterConstants.PARAM_NAME, institutionId);
            } catch (PersistenceException | IllegalStateException ignored) {
                // EntityManager might already be closed or not available (e.g. outside transaction)
            }
        }
    }

    /**
     * Disable the institution filter on the given EntityManager.
     */
    public static void disableFilter(EntityManager entityManager) {
        try {
            Session session = entityManager.unwrap(Session.class);
            if (session != null && session.getEnabledFilter(InstitutionFilterConstants.FILTER_NAME) != null) {
                session.disableFilter(InstitutionFilterConstants.FILTER_NAME);
            }
        } catch (PersistenceException | IllegalStateException ignored) {
            // EntityManager might already be closed or not available
        }
    }

    /**
     * Check if we're in a web request context (used for conditional filter application).
     */
    public static boolean isInWebContext() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes;
    }
}

