package de.bbajor.pvs.institution.security;

import de.bbajor.pvs.institution.context.InstitutionContext;
import jakarta.persistence.EntityManager;

import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
            Session session = entityManager.unwrap(Session.class);
            Filter filter = session.enableFilter("institutionFilter");
            filter.setParameter("institutionId", institutionId);
        }
    }

    /**
     * Check if we're in a web request context (used for conditional filter application).
     */
    public static boolean isInWebContext() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes;
    }
}

