package de.bbajor.pvs.tenant.security;

import de.bbajor.pvs.tenant.context.TenantContext;
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
 * Filter that sets the tenant context from the authenticated user.
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication instanceof TenantAuthenticationToken) {
                TenantAuthenticationToken tenantAuth = (TenantAuthenticationToken) authentication;
                if (tenantAuth.getTenantId() != null) {
                    TenantContext.setTenantId(tenantAuth.getTenantId());
                }
            }
            
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
