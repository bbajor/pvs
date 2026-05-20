package de.bbajor.pvs.security.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class ApiSecurityConfigTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Test
    void jwtAuthenticationUsesDatabaseRolesAndInstitution() {
        Institution institution = new Institution().setInstitutionName("Praxis");
        institution.setId(42L);
        UserAccount account = new UserAccount()
                .setUserId("oidc-subject")
                .setUsername("doctor")
                .setFullName("Dr. Test")
                .setEmail("doctor@example.test")
                .setEnabled(true)
                .setInstitution(institution)
                .setRoles(Set.of(AppRoles.USER));

        when(userAccountRepository.findByUserId("oidc-subject")).thenReturn(Optional.of(account));

        Authentication authentication = new ApiSecurityConfig()
                .jwtAuthConverter(userAccountRepository)
                .convert(jwtWithClaims("oidc-subject", "doctor", 7L, AppRoles.SUPER_ADMIN));

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        assertThat(authentication.getPrincipal())
                .isInstanceOfSatisfying(JwtAppUserPrincipal.class, principal -> {
                    assertThat(principal.getInstitutionId()).contains(42L);
                    assertThat(principal.getAppUser().getPreferredUsername()).isEqualTo("doctor");
                });
    }

    @Test
    void jwtAuthenticationDoesNotGrantTokenRolesOrInstitutionWithoutUserAccount() {
        when(userAccountRepository.findByUserId("oidc-subject")).thenReturn(Optional.empty());
        when(userAccountRepository.findByUsernameOrEmail("doctor")).thenReturn(Optional.empty());

        Authentication authentication = new ApiSecurityConfig()
                .jwtAuthConverter(userAccountRepository)
                .convert(jwtWithClaims("oidc-subject", "doctor", 7L, AppRoles.SUPER_ADMIN));

        assertThat(authentication.getAuthorities()).isEmpty();
        assertThat(authentication.getPrincipal())
                .isInstanceOfSatisfying(JwtAppUserPrincipal.class, principal ->
                        assertThat(principal.getInstitutionId()).isEmpty());
    }

    private static Jwt jwtWithClaims(String subject, String username, Long institutionId, String role) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.EPOCH)
                .expiresAt(Instant.EPOCH.plusSeconds(3600))
                .claim("preferred_username", username)
                .claim("institution_id", institutionId)
                .claim("roles", List.of(role))
                .build();
    }
}
