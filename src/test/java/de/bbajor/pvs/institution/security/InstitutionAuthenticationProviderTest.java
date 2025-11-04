package de.bbajor.pvs.institution.security;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InstitutionAuthenticationProvider.
 */
@ExtendWith(MockitoExtension.class)
class InstitutionAuthenticationProviderTest {

    @Mock
    private InstitutionRepository institutionRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private InstitutionAuthenticationProvider authenticationProvider;

    private Institution testInstitution;
    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        testInstitution = new Institution()
                .setInstitutionCode("TEST-1234")
                .setInstitutionName("Test Praxis")
                .setActive(true)
                .setDatabaseName("pvs_inst_test_1234")
                .setContainerName("postgres-inst-test-1234");
        testInstitution.setId(1L);

        testUser = new UserAccount()
                .setUsername("testuser")
                .setPasswordHash("$2a$10$hashedpassword")
                .setEnabled(true)
                .setInstitution(testInstitution)
                .setRoles(Set.of("USER"));
        testUser.setId(1L);
    }

    @Test
    void testAuthenticate_validCredentials_shouldSucceed() {
        // Given
        InstitutionAuthenticationToken token = new InstitutionAuthenticationToken(
                "TEST-1234", "testuser", "password123");

        when(institutionRepository.findByInstitutionCode("TEST-1234")).thenReturn(Optional.of(testInstitution));
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);

        // When
        Authentication result = authenticationProvider.authenticate(token);

        // Then
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals("testuser", result.getName());
        assertTrue(result instanceof InstitutionAuthenticationToken);
        InstitutionAuthenticationToken resultToken = (InstitutionAuthenticationToken) result;
        assertEquals("TEST-1234", resultToken.getInstitutionCode());
        assertEquals(1L, resultToken.getInstitutionId());
    }

    @Test
    void testAuthenticate_invalidInstitutionCode_shouldFail() {
        // Given
        InstitutionAuthenticationToken token = new InstitutionAuthenticationToken(
                "INVALID", "testuser", "password123");

        when(institutionRepository.findByInstitutionCode("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(token);
        });
    }

    @Test
    void testAuthenticate_inactiveInstitution_shouldFail() {
        // Given
        testInstitution.setActive(false);
        InstitutionAuthenticationToken token = new InstitutionAuthenticationToken(
                "TEST-1234", "testuser", "password123");

        when(institutionRepository.findByInstitutionCode("TEST-1234")).thenReturn(Optional.of(testInstitution));

        // When & Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(token);
        });
        assertEquals("Institution is not active", exception.getMessage());
    }

    @Test
    void testAuthenticate_invalidUsername_shouldFail() {
        // Given
        InstitutionAuthenticationToken token = new InstitutionAuthenticationToken(
                "TEST-1234", "nonexistent", "password123");

        when(institutionRepository.findByInstitutionCode("TEST-1234")).thenReturn(Optional.of(testInstitution));
        when(userAccountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(token);
        });
    }

    @Test
    void testAuthenticate_invalidPassword_shouldFail() {
        // Given
        InstitutionAuthenticationToken token = new InstitutionAuthenticationToken(
                "TEST-1234", "testuser", "wrongpassword");

        when(institutionRepository.findByInstitutionCode("TEST-1234")).thenReturn(Optional.of(testInstitution));
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);

        // When & Then
        assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(token);
        });
    }

    @Test
    void testAuthenticate_userFromDifferentInstitution_shouldFail() {
        // Given
        Institution otherInstitution = new Institution()
                .setInstitutionCode("OTHER-5678")
                .setInstitutionName("Other Praxis")
                .setActive(true)
                .setDatabaseName("pvs_inst_other_5678")
                .setContainerName("postgres-inst-other-5678");
        otherInstitution.setId(2L);

        testUser.setInstitution(otherInstitution);

        InstitutionAuthenticationToken token = new InstitutionAuthenticationToken(
                "TEST-1234", "testuser", "password123");

        when(institutionRepository.findByInstitutionCode("TEST-1234")).thenReturn(Optional.of(testInstitution));
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(token);
        });
        assertEquals("User does not belong to this institution", exception.getMessage());
    }

    @Test
    void testAuthenticate_superAdminWithNullInstitution_shouldSucceed() {
        // Given
        UserAccount superAdmin = new UserAccount()
                .setUsername("superadmin")
                .setPasswordHash("$2a$10$hashedpassword")
                .setEnabled(true)
                .setInstitution(null) // Super-Admin has no institution
                .setRoles(Set.of("SUPER_ADMIN", "ADMIN", "USER"));
        superAdmin.setId(999L);

        InstitutionAuthenticationToken token = new InstitutionAuthenticationToken(
                "TEST-1234", "superadmin", "password123");

        when(institutionRepository.findByInstitutionCode("TEST-1234")).thenReturn(Optional.of(testInstitution));
        when(userAccountRepository.findByUsername("superadmin")).thenReturn(Optional.of(superAdmin));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);

        // When
        Authentication result = authenticationProvider.authenticate(token);

        // Then
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals("superadmin", result.getName());
    }

    @Test
    void testAuthenticate_disabledUser_shouldFail() {
        // Given
        testUser.setEnabled(false);
        InstitutionAuthenticationToken token = new InstitutionAuthenticationToken(
                "TEST-1234", "testuser", "password123");

        when(institutionRepository.findByInstitutionCode("TEST-1234")).thenReturn(Optional.of(testInstitution));
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);

        // When & Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(token);
        });
        assertEquals("User is not enabled", exception.getMessage());
    }
}

