package de.bbajor.pvs.security.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import de.bbajor.pvs.institution.service.CurrentInstitutionService;

@ExtendWith(MockitoExtension.class)
class InstitutionRequiredAuthorizationManagerTest {

    @Mock
    private CurrentInstitutionService currentInstitutionService;

    @Test
    void deniesSuperAdminWithoutInstitutionContext() {
        InstitutionRequiredAuthorizationManager manager =
                new InstitutionRequiredAuthorizationManager(currentInstitutionService);
        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken("admin", "n/a", "ROLE_SUPER_ADMIN");
        authentication.setAuthenticated(true);
        when(currentInstitutionService.hasInstitution()).thenReturn(false);

        assertThat(manager.check(() -> authentication, null).isGranted()).isFalse();
    }

    @Test
    void allowsAuthenticatedUserWithInstitutionContext() {
        InstitutionRequiredAuthorizationManager manager =
                new InstitutionRequiredAuthorizationManager(currentInstitutionService);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "n/a", "ROLE_USER");
        authentication.setAuthenticated(true);
        when(currentInstitutionService.hasInstitution()).thenReturn(true);

        assertThat(manager.check(() -> authentication, null).isGranted()).isTrue();
    }

    @Test
    void deniesAnonymousAuthentication() {
        InstitutionRequiredAuthorizationManager manager =
                new InstitutionRequiredAuthorizationManager(currentInstitutionService);
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThat(manager.check(() -> authentication, null).isGranted()).isFalse();
    }
}
