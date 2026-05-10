package de.bbajor.pvs.security.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import de.bbajor.pvs.institution.service.CurrentInstitutionService;

@ExtendWith(MockitoExtension.class)
class InstitutionRequiredAuthorizationManagerTest {

    @Mock
    private CurrentInstitutionService currentInstitutionService;

    @Test
    void deniesSuperAdminWithoutInstitution() {
        InstitutionRequiredAuthorizationManager manager =
                new InstitutionRequiredAuthorizationManager(currentInstitutionService);
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("superadmin", "n/a", "ROLE_SUPER_ADMIN");

        when(currentInstitutionService.hasInstitution()).thenReturn(false);

        AuthorizationDecision decision = manager.check(() -> authentication, requestContext());

        assertFalse(decision.isGranted());
    }

    @Test
    void grantsAuthenticatedUserWithInstitution() {
        InstitutionRequiredAuthorizationManager manager =
                new InstitutionRequiredAuthorizationManager(currentInstitutionService);
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("doctor", "n/a", "ROLE_DOCTOR");

        when(currentInstitutionService.hasInstitution()).thenReturn(true);

        AuthorizationDecision decision = manager.check(() -> authentication, requestContext());

        assertTrue(decision.isGranted());
    }

    @Test
    void deniesMissingAuthentication() {
        InstitutionRequiredAuthorizationManager manager =
                new InstitutionRequiredAuthorizationManager(currentInstitutionService);

        AuthorizationDecision decision = manager.check(() -> null, requestContext());

        assertFalse(decision.isGranted());
        verifyNoInteractions(currentInstitutionService);
    }

    private static RequestAuthorizationContext requestContext() {
        return new RequestAuthorizationContext(new MockHttpServletRequest());
    }
}
