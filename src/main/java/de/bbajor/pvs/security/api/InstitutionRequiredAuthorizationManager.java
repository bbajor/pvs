package de.bbajor.pvs.security.api;

import java.util.function.Supplier;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.security.AppRoles;

public final class InstitutionRequiredAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final CurrentInstitutionService currentInstitutionService;

    public InstitutionRequiredAuthorizationManager(CurrentInstitutionService currentInstitutionService) {
        this.currentInstitutionService = currentInstitutionService;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + AppRoles.SUPER_ADMIN).equals(a.getAuthority()));
        if (isSuperAdmin) {
            return new AuthorizationDecision(true);
        }

        return new AuthorizationDecision(currentInstitutionService.hasInstitution());
    }
}

