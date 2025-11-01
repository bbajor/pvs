package de.bbajor.pvs.tenant.security;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.tenant.model.Tenant;
import de.bbajor.pvs.tenant.repository.TenantRepository;
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
 * Unit tests for TenantAuthenticationProvider.
 */
@ExtendWith(MockitoExtension.class)
class TenantAuthenticationProviderTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantAuthenticationProvider authenticationProvider;

    private Tenant testTenant;
    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        testTenant = new Tenant()
                .setTenantCode("TEST-1234")
                .setTenantName("Test Praxis")
                .setActive(true);
        testTenant.setId(1L);

        testUser = new UserAccount()
                .setUsername("testuser")
                .setPasswordHash("$2a$10$hashedpassword")
                .setEnabled(true)
                .setTenant(testTenant)
                .setRoles(Set.of("USER"));
        testUser.setId(1L);
    }

    @Test
    void testAuthenticate_validCredentials_shouldSucceed() {
        // Given
        TenantAuthenticationToken token = new TenantAuthenticationToken(
                "TEST-1234", "testuser", "password123");

        when(tenantRepository.findByTenantCode("TEST-1234")).thenReturn(Optional.of(testTenant));
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);

        // When
        Authentication result = authenticationProvider.authenticate(token);

        // Then
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals("testuser", result.getName());
        assertTrue(result instanceof TenantAuthenticationToken);
        TenantAuthenticationToken resultToken = (TenantAuthenticationToken) result;
        assertEquals("TEST-1234", resultToken.getTenantCode());
        assertEquals(1L, resultToken.getTenantId());
    }

    @Test
    void testAuthenticate_invalidTenantCode_shouldFail() {
        // Given
        TenantAuthenticationToken token = new TenantAuthenticationToken(
                "INVALID", "testuser", "password123");

        when(tenantRepository.findByTenantCode("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(token);
        });
    }

    @Test
    void testAuthenticate_inactiveTenant_shouldFail() {
        // Given
        testTenant.setActive(false);
        TenantAuthenticationToken token = new TenantAuthenticationToken(
                "TEST-1234", "testuser", "password123");

        when(tenantRepository.findByTenantCode("TEST-1234")).thenReturn(Optional.of(testTenant));

        // When & Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(token);
        });
        assertEquals("Tenant is not active", exception.getMessage());
    }

    @Test
    void testAuthenticate_invalidUsername_shouldFail() {
        // Given
        TenantAuthenticationToken token = new TenantAuthenticationToken(
                "TEST-1234", "nonexistent", "password123");

        when(tenantRepository.findByTenantCode("TEST-1234")).thenReturn(Optional.of(testTenant));
        when(userAccountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(token);
        });
    }

    @Test
    void testAuthenticate_invalidPassword_shouldFail() {
        // Given
        TenantAuthenticationToken token = new TenantAuthenticationToken(
                "TEST-1234", "testuser", "wrongpassword");

        when(tenantRepository.findByTenantCode("TEST-1234")).thenReturn(Optional.of(testTenant));
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword")).thenReturn(false);

        // When & Then
        assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(token);
        });
    }

    @Test
    void testAuthenticate_userFromDifferentTenant_shouldFail() {
        // Given
        Tenant otherTenant = new Tenant()
                .setTenantCode("OTHER-5678")
                .setTenantName("Other Praxis")
                .setActive(true);
        otherTenant.setId(2L);

        testUser.setTenant(otherTenant);

        TenantAuthenticationToken token = new TenantAuthenticationToken(
                "TEST-1234", "testuser", "password123");

        when(tenantRepository.findByTenantCode("TEST-1234")).thenReturn(Optional.of(testTenant));
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(token);
        });
        assertEquals("User does not belong to this tenant", exception.getMessage());
    }

    @Test
    void testAuthenticate_superAdminWithNullTenant_shouldSucceed() {
        // Given
        UserAccount superAdmin = new UserAccount()
                .setUsername("superadmin")
                .setPasswordHash("$2a$10$hashedpassword")
                .setEnabled(true)
                .setTenant(null) // Super-Admin has no tenant
                .setRoles(Set.of("SUPER_ADMIN", "ADMIN", "USER"));
        superAdmin.setId(999L);

        TenantAuthenticationToken token = new TenantAuthenticationToken(
                "TEST-1234", "superadmin", "password123");

        when(tenantRepository.findByTenantCode("TEST-1234")).thenReturn(Optional.of(testTenant));
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
        TenantAuthenticationToken token = new TenantAuthenticationToken(
                "TEST-1234", "testuser", "password123");

        when(tenantRepository.findByTenantCode("TEST-1234")).thenReturn(Optional.of(testTenant));
        when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);

        // When & Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authenticationProvider.authenticate(token);
        });
        assertEquals("User is not enabled", exception.getMessage());
    }
}
