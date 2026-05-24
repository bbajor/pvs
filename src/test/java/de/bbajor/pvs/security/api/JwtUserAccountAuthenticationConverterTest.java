package de.bbajor.pvs.security.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;

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
    void convert_usesDatabaseInstitutionAndRolesInsteadOfJwtClaims() {
        Institution institution = new Institution();
        institution.setId(7L);
        UserAccount userAccount = new UserAccount()
                .setUsername("doctor")
                .setUserId("oidc-subject")
                .setEmail("doctor@example.test")
                .setFullName("Dr Test")
                .setEnabled(true)
                .setInstitution(institution)
                .setRoles(Set.of("USER"));

        Jwt jwt = jwtBuilder("oidc-subject")
                .claim("preferred_username", "doctor")
                .claim("email", "doctor@example.test")
                .claim("institution_id", 999L)
                .claim("roles", List.of("SUPER_ADMIN"))
                .build();

        when(userAccountRepository.findAllByUserIdOrderByInstitutionFirst("oidc-subject"))
                .thenReturn(List.of(userAccount));

        var authentication = converter.convert(jwt);
        var principal = (JwtAppUserPrincipal) authentication.getPrincipal();

        assertEquals(7L, principal.getInstitutionId().orElseThrow());
        assertEquals("doctor", principal.getAppUser().getPreferredUsername());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertFalse(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        verify(userAccountRepository, never()).findAllByUsernameOrEmailOrderByInstitutionFirst("doctor");
    }

    @Test
    void convert_rejectsJwtWithoutMatchingUserAccount() {
        Jwt jwt = jwtBuilder("missing-subject")
                .claim("preferred_username", "missing")
                .build();

        when(userAccountRepository.findAllByUserIdOrderByInstitutionFirst("missing-subject"))
                .thenReturn(List.of());
        when(userAccountRepository.findAllByUsernameOrEmailOrderByInstitutionFirst("missing"))
                .thenReturn(List.of());
        when(userAccountRepository.findAllByUsernameOrEmailOrderByInstitutionFirst("missing-subject"))
                .thenReturn(List.of());

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }

    @Test
    void convert_rejectsDisabledUserAccount() {
        UserAccount userAccount = new UserAccount()
                .setUsername("doctor")
                .setUserId("oidc-subject")
                .setEnabled(false)
                .setRoles(Set.of("USER"));
        Jwt jwt = jwtBuilder("oidc-subject").build();

        when(userAccountRepository.findAllByUserIdOrderByInstitutionFirst("oidc-subject"))
                .thenReturn(List.of(userAccount));

        assertThrows(DisabledException.class, () -> converter.convert(jwt));
    }

    @Test
    void convert_rejectsAmbiguousLegacyIdentifierFallback() {
        UserAccount first = new UserAccount().setUsername("shared").setEnabled(true);
        UserAccount second = new UserAccount().setUsername("shared").setEnabled(true);
        Jwt jwt = jwtBuilder("legacy-subject")
                .claim("preferred_username", "shared")
                .build();

        when(userAccountRepository.findAllByUserIdOrderByInstitutionFirst("legacy-subject"))
                .thenReturn(List.of());
        when(userAccountRepository.findAllByUsernameOrEmailOrderByInstitutionFirst("shared"))
                .thenReturn(List.of(first, second));

        assertThrows(BadCredentialsException.class, () -> converter.convert(jwt));
    }

    private static Jwt.Builder jwtBuilder(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject);
    }
}
