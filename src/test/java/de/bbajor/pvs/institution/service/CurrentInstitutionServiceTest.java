package de.bbajor.pvs.institution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.security.api.JwtAppUserInfo;
import de.bbajor.pvs.security.api.JwtAppUserPrincipal;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserId;

@ExtendWith(MockitoExtension.class)
class CurrentInstitutionServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void appUserPrincipalUsesDatabaseInstitutionWhenJwtClaimIsMissing() {
        Institution institution = new Institution().setInstitutionName("Praxis");
        institution.setId(42L);
        UserAccount account = new UserAccount()
                .setUserId("oidc-subject")
                .setUsername("doctor")
                .setInstitution(institution);

        when(userAccountRepository.findByUserId("oidc-subject")).thenReturn(Optional.of(account));
        SecurityContextHolder.getContext().setAuthentication(authentication(jwtPrincipal(null)));

        CurrentInstitutionService service = new CurrentInstitutionService(userAccountRepository);

        assertThat(service.getCurrentInstitutionId()).contains(42L);
    }

    @Test
    void appUserPrincipalPrefersDatabaseInstitutionOverJwtClaim() {
        Institution institution = new Institution().setInstitutionName("Praxis");
        institution.setId(42L);
        UserAccount account = new UserAccount()
                .setUserId("oidc-subject")
                .setUsername("doctor")
                .setInstitution(institution);

        when(userAccountRepository.findByUserId("oidc-subject")).thenReturn(Optional.of(account));
        SecurityContextHolder.getContext().setAuthentication(authentication(jwtPrincipal(7L)));

        CurrentInstitutionService service = new CurrentInstitutionService(userAccountRepository);

        assertThat(service.getCurrentInstitutionId()).contains(42L);
    }

    private static TestingAuthenticationToken authentication(JwtAppUserPrincipal principal) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                principal,
                "n/a",
                principal.getAuthorities());
        authentication.setAuthenticated(true);
        return authentication;
    }

    private static JwtAppUserPrincipal jwtPrincipal(Long institutionId) {
        JwtAppUserInfo userInfo = new JwtAppUserInfo(
                UserId.of("oidc-subject"),
                "doctor",
                "Dr. Test",
                "doctor@example.test",
                Locale.ROOT,
                institutionId);
        return new JwtAppUserPrincipal(userInfo, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
