package de.bbajor.pvs.security.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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

    private InstitutionRequiredAuthorizationManager authorizationManager;

    @BeforeEach
    void setUp() {
        authorizationManager = new InstitutionRequiredAuthorizationManager(currentInstitutionService);
    }

    @Test
    void check_deniesSuperAdminWithoutInstitutionContext() {
        when(currentInstitutionService.hasInstitution()).thenReturn(false);
        var authentication = new UsernamePasswordAuthenticationToken(
                "super-admin",
                "token",
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        var decision = authorizationManager.check(() -> authentication, null);

        assertThat(decision.isGranted()).isFalse();
    }

    @Test
    void check_allowsAuthenticatedUserWithInstitutionContext() {
        when(currentInstitutionService.hasInstitution()).thenReturn(true);
        var authentication = new UsernamePasswordAuthenticationToken(
                "doctor",
                "token",
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));

        var decision = authorizationManager.check(() -> authentication, null);

        assertThat(decision.isGranted()).isTrue();
    }
}
