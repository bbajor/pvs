package de.bbajor.pvs.institution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtPrincipalWithoutInstitutionClaimUsesEnabledUserAccountInstitution() {
        CurrentInstitutionService service = new CurrentInstitutionService(userAccountRepository);
        doReturn(Optional.of(userAccount(42L, true))).when(userAccountRepository).findByUsername("alice");
        setAuthentication(jwtPrincipal("alice", null));

        assertEquals(Optional.of(42L), service.getCurrentInstitutionId());
    }

    @Test
    void jwtPrincipalInstitutionClaimCannotOverrideUserAccountInstitution() {
        CurrentInstitutionService service = new CurrentInstitutionService(userAccountRepository);
        doReturn(Optional.of(userAccount(42L, true))).when(userAccountRepository).findByUsername("alice");
        setAuthentication(jwtPrincipal("alice", 999L));

        assertEquals(Optional.of(42L), service.getCurrentInstitutionId());
    }

    @Test
    void jwtPrincipalWithoutEnabledUserAccountFailsClosedEvenWithInstitutionClaim() {
        CurrentInstitutionService service = new CurrentInstitutionService(userAccountRepository);
        doReturn(Optional.of(userAccount(42L, false))).when(userAccountRepository).findByUsername("alice");
        setAuthentication(jwtPrincipal("alice", 999L));

        assertTrue(service.getCurrentInstitutionId().isEmpty());
    }

    private static void setAuthentication(JwtAppUserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                "token",
                principal.getAuthorities()));
    }

    private static JwtAppUserPrincipal jwtPrincipal(String username, Long institutionClaim) {
        var userInfo = new JwtAppUserInfo(
                UserId.of("sub-" + username),
                username,
                username,
                username + "@example.test",
                Locale.ROOT,
                institutionClaim);
        return new JwtAppUserPrincipal(userInfo, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static UserAccount userAccount(Long institutionId, boolean enabled) {
        Institution institution = new Institution()
                .setInstitutionCode("INST-" + institutionId)
                .setInstitutionName("Test Praxis")
                .setDatabaseName("pvs_inst_" + institutionId)
                .setContainerName("postgres-inst-" + institutionId);
        institution.setId(institutionId);

        return new UserAccount()
                .setUsername("alice")
                .setPasswordHash("unused")
                .setEnabled(enabled)
                .setInstitution(institution);
    }
}
