package de.bbajor.pvs.security.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;

@ExtendWith(MockitoExtension.class)
class JwtUserAccountAuthenticationConverterTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private JwtUserAccountAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JwtUserAccountAuthenticationConverter(userAccountRepository);
    }

    @Test
    void usesDatabaseUserForRolesAndInstitution() {
        UserAccount account = user("oidc-sub", "arzt", true, 42L, "USER");
        when(userAccountRepository.findAllByUserId("oidc-sub")).thenReturn(List.of(account));

        Jwt jwt = jwt("oidc-sub")
                .claim("roles", List.of("SUPER_ADMIN"))
                .claim("institution_id", 999L)
                .build();

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        assertThat(authentication.getPrincipal()).isInstanceOf(UserAccountUserDetailsAdapter.class);

        UserAccountUserDetailsAdapter principal = (UserAccountUserDetailsAdapter) authentication.getPrincipal();
        assertThat(principal.getUsername()).isEqualTo("arzt");
        assertThat(principal.getInstitutionId()).contains(42L);
        verify(userAccountRepository, never()).findAllByUsernameOrEmailOrderByInstitutionFirst("arzt");
    }

    @Test
    void rejectsJwtWithoutMatchingUserAccount() {
        when(userAccountRepository.findAllByUserId("missing-sub")).thenReturn(List.of());

        assertThatThrownBy(() -> converter.convert(jwt("missing-sub").build()))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsDisabledUserAccount() {
        UserAccount account = user("oidc-sub", "arzt", false, 42L, "USER");
        when(userAccountRepository.findAllByUserId("oidc-sub")).thenReturn(List.of(account));

        assertThatThrownBy(() -> converter.convert(jwt("oidc-sub").build()))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void rejectsAmbiguousUsernameFallback() {
        UserAccount first = user(null, "shared", true, 1L, "USER");
        UserAccount second = user(null, "shared", true, 2L, "USER");
        when(userAccountRepository.findAllByUserId("oidc-sub")).thenReturn(List.of());
        when(userAccountRepository.findAllByUsernameOrEmailOrderByInstitutionFirst("shared"))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> converter.convert(jwt("oidc-sub")
                .claim("preferred_username", "shared")
                .build()))
                .isInstanceOf(BadCredentialsException.class);
    }

    private static Jwt.Builder jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject);
    }

    private static UserAccount user(String userId, String username, boolean enabled, Long institutionId, String role) {
        Institution institution = new Institution()
                .setInstitutionCode("INST-" + institutionId)
                .setInstitutionName("Praxis " + institutionId);
        institution.setId(institutionId);

        UserAccount account = new UserAccount()
                .setUserId(userId)
                .setUsername(username)
                .setPasswordHash("n/a")
                .setEnabled(enabled)
                .setInstitution(institution)
                .setRoles(Set.of(role));
        account.setId(institutionId);
        return account;
    }
}
