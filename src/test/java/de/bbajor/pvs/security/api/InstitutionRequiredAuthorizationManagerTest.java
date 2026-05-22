package de.bbajor.pvs.security.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.security.AppRoles;

@ExtendWith(MockitoExtension.class)
class InstitutionRequiredAuthorizationManagerTest {

    @Mock
    private CurrentInstitutionService currentInstitutionService;

    @Test
    void superAdminWithoutInstitutionIsDeniedForTenantApi() {
        InstitutionRequiredAuthorizationManager manager =
                new InstitutionRequiredAuthorizationManager(currentInstitutionService);
        when(currentInstitutionService.hasInstitution()).thenReturn(false);

        var decision = manager.check(
                () -> authenticated("admin", "ROLE_" + AppRoles.SUPER_ADMIN),
                requestContext());

        assertFalse(decision.isGranted());
    }

    @Test
    void authenticatedUserWithInstitutionIsAllowedForTenantApi() {
        InstitutionRequiredAuthorizationManager manager =
                new InstitutionRequiredAuthorizationManager(currentInstitutionService);
        when(currentInstitutionService.hasInstitution()).thenReturn(true);

        var decision = manager.check(
                () -> authenticated("doctor", "ROLE_" + AppRoles.DOCTOR),
                requestContext());

        assertTrue(decision.isGranted());
    }

    private static Authentication authenticated(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username,
                "token",
                List.of(new SimpleGrantedAuthority(role)));
    }

    private static RequestAuthorizationContext requestContext() {
        return new RequestAuthorizationContext(new MockHttpServletRequest());
    }
}
