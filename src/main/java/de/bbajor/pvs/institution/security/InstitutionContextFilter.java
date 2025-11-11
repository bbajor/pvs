package de.bbajor.pvs.institution.security;

import de.bbajor.pvs.institution.context.InstitutionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that sets the institution context from the authenticated user.
 */
@Component
public class InstitutionContextFilter extends OncePerRequestFilter {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
                if (institutionAuth.getInstitutionId() != null) {
                    InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                }
            }
            
            filterChain.doFilter(request, response);
        } finally {
            if (entityManager != null) {
                InstitutionFilter.disableFilter(entityManager);
            }
            InstitutionContext.clear();
        }
    }
}

