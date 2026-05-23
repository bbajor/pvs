package de.bbajor.pvs.institution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    private CurrentInstitutionService service;

    @BeforeEach
    void setUp() {
        service = new CurrentInstitutionService(userAccountRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        service.clearExecutionOverride();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentInstitutionId_resolvesJwtUserWithoutTenantClaimFromUserAccount() {
        authenticateJwtUser(null);
        when(userAccountRepository.findByUsernameOrEmail("alice"))
                .thenReturn(Optional.of(userAccount(7L)));

        assertEquals(Optional.of(7L), service.getCurrentInstitutionId());
    }

    @Test
    void getCurrentInstitutionId_usesDatabaseInstitutionInsteadOfJwtTenantClaim() {
        authenticateJwtUser(99L);
        when(userAccountRepository.findByUsernameOrEmail("alice"))
                .thenReturn(Optional.of(userAccount(7L)));

        assertEquals(Optional.of(7L), service.getCurrentInstitutionId());
    }

    @Test
    void getCurrentInstitutionId_doesNotTrustJwtTenantClaimWithoutUserAccount() {
        authenticateJwtUser(99L);
        when(userAccountRepository.findByUsernameOrEmail("alice"))
                .thenReturn(Optional.empty());
        when(userAccountRepository.findByUsernameOrEmail("alice@example.test"))
                .thenReturn(Optional.empty());

        assertTrue(service.getCurrentInstitutionId().isEmpty());
    }

    private void authenticateJwtUser(Long jwtInstitutionId) {
        JwtAppUserInfo appUser = new JwtAppUserInfo(
                UserId.of("oidc-subject"),
                "alice",
                "Alice Example",
                "alice@example.test",
                Locale.ROOT,
                jwtInstitutionId);
        JwtAppUserPrincipal principal = new JwtAppUserPrincipal(
                appUser,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities()));
    }

    private static UserAccount userAccount(Long institutionId) {
        Institution institution = new Institution();
        institution.setId(institutionId);
        return new UserAccount()
                .setUsername("alice")
                .setEnabled(true)
                .setInstitution(institution);
    }
}
