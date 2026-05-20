package de.bbajor.pvs.security.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.security.AppRoles;

@ExtendWith(MockitoExtension.class)
class InstitutionRequiredAuthorizationManagerTest {

    @Mock
    private CurrentInstitutionService currentInstitutionService;

    @Test
    void deniesSuperAdminWithoutInstitutionContext() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "superadmin",
                "n/a",
                "ROLE_" + AppRoles.SUPER_ADMIN);
        authentication.setAuthenticated(true);

        var manager = new InstitutionRequiredAuthorizationManager(currentInstitutionService);

        assertThat(manager.check(() -> authentication, null).isGranted()).isFalse();
    }

    @Test
    void allowsAuthenticatedUserWithInstitutionContext() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "doctor",
                "n/a",
                "ROLE_" + AppRoles.USER);
        authentication.setAuthenticated(true);
        when(currentInstitutionService.hasInstitution()).thenReturn(true);

        var manager = new InstitutionRequiredAuthorizationManager(currentInstitutionService);

        assertThat(manager.check(() -> authentication, null).isGranted()).isTrue();
    }

    @Test
    void deniesAnonymousAuthenticationToken() {
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        var manager = new InstitutionRequiredAuthorizationManager(currentInstitutionService);

        assertThat(manager.check(() -> authentication, null).isGranted()).isFalse();
    }
}
