package de.bbajor.pvs.institution.security;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Ensures the Hibernate tenant filter is active before repository interactions.
 */
@Aspect
@Component
public class InstitutionTenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* org.springframework.data.repository.Repository+.*(..))")
    public void applyInstitutionFilter() {
        InstitutionFilter.enableFilter(entityManager);
    }
}
