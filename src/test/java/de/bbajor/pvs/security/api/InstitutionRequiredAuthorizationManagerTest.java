package de.bbajor.pvs.security.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import de.bbajor.pvs.institution.service.CurrentInstitutionService;

@ExtendWith(MockitoExtension.class)
class InstitutionRequiredAuthorizationManagerTest {

    @Mock
    private CurrentInstitutionService currentInstitutionService;

    @Test
    void check_deniesSuperAdminWithoutInstitution() {
        InstitutionRequiredAuthorizationManager manager =
                new InstitutionRequiredAuthorizationManager(currentInstitutionService);
        var authentication = new UsernamePasswordAuthenticationToken(
                "superadmin",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        when(currentInstitutionService.hasInstitution()).thenReturn(false);

        var decision = manager.check(() -> authentication, null);

        assertFalse(decision.isGranted());
    }

    @Test
    void check_allowsAuthenticatedUserWithInstitution() {
        InstitutionRequiredAuthorizationManager manager =
                new InstitutionRequiredAuthorizationManager(currentInstitutionService);
        var authentication = new UsernamePasswordAuthenticationToken(
                "doctor",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(currentInstitutionService.hasInstitution()).thenReturn(true);

        var decision = manager.check(() -> authentication, null);

        assertTrue(decision.isGranted());
    }
}
