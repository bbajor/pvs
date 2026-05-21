package de.bbajor.pvs.institution.service;

import static org.assertj.core.api.Assertions.assertThat;
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

    private CurrentInstitutionService currentInstitutionService;

    @BeforeEach
    void setUp() {
        currentInstitutionService = new CurrentInstitutionService(userAccountRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        currentInstitutionService.clearExecutionOverride();
    }

    @Test
    void getCurrentInstitutionId_usesDatabaseInstitutionInsteadOfJwtClaim() {
        UserAccount account = enabledAccount(7L);
        when(userAccountRepository.findByUsernameOrEmail("doctor@example.test")).thenReturn(Optional.of(account));
        authenticateJwtPrincipal("doctor@example.test", "doctor@example.test", 99L);

        Optional<Long> institutionId = currentInstitutionService.getCurrentInstitutionId();

        assertThat(institutionId).contains(7L);
    }

    @Test
    void getCurrentInstitutionId_fallsBackToEmailForDatabaseLookup() {
        UserAccount account = enabledAccount(11L);
        when(userAccountRepository.findByUsernameOrEmail("oidc-subject")).thenReturn(Optional.empty());
        when(userAccountRepository.findByUsernameOrEmail("doctor@example.test")).thenReturn(Optional.of(account));
        authenticateJwtPrincipal("oidc-subject", "doctor@example.test", null);

        Optional<Long> institutionId = currentInstitutionService.getCurrentInstitutionId();

        assertThat(institutionId).contains(11L);
    }

    @Test
    void getCurrentInstitutionId_failsClosedWhenJwtClaimHasNoEnabledUserAccount() {
        when(userAccountRepository.findByUsernameOrEmail("doctor@example.test")).thenReturn(Optional.empty());
        authenticateJwtPrincipal("doctor@example.test", "doctor@example.test", 99L);

        Optional<Long> institutionId = currentInstitutionService.getCurrentInstitutionId();

        assertThat(institutionId).isEmpty();
    }

    private static void authenticateJwtPrincipal(String preferredUsername, String email, Long institutionClaim) {
        var userInfo = new JwtAppUserInfo(
                UserId.of("oidc-subject"),
                preferredUsername,
                "Dr Test",
                email,
                Locale.ROOT,
                institutionClaim);
        var principal = new JwtAppUserPrincipal(userInfo, List.of());
        var authentication = new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static UserAccount enabledAccount(Long institutionId) {
        Institution institution = new Institution();
        institution.setId(institutionId);
        return new UserAccount()
                .setUsername("doctor@example.test")
                .setPasswordHash("unused")
                .setEnabled(true)
                .setInstitution(institution);
    }
}
