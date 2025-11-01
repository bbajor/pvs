package de.bbajor.pvs.tenant.security;

import de.bbajor.pvs.tenant.context.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility to enable Hibernate tenant filter for queries.
 * This ensures that all queries automatically filter by tenant_id.
 */
@Component
public class TenantFilter {

    /**
     * Enable the tenant filter on the given EntityManager.
     * This should be called before executing queries that need tenant isolation.
     */
    public static void enableFilter(EntityManager entityManager) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            Filter filter = session.enableFilter("tenantFilter");
            filter.setParameter("tenantId", tenantId);
        }
    }

    /**
     * Check if we're in a web request context (used for conditional filter application).
     */
    public static boolean isInWebContext() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes;
    }
}
