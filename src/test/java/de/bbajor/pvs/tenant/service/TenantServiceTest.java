package de.bbajor.pvs.tenant.service;

import de.bbajor.pvs.tenant.model.Tenant;
import de.bbajor.pvs.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantService.
 */
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testTenant = new Tenant()
                .setTenantCode("TEST-1234")
                .setTenantName("Test Praxis")
                .setActive(true);
        testTenant.setId(1L);
    }

    @Test
    void testCreateTenant_shouldGenerateTenantCode() {
        // Given
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            tenant.setId(1L);
            return tenant;
        });

        // When
        Tenant created = tenantService.createTenant("Test Praxis");

        // Then
        assertNotNull(created);
        assertNotNull(created.getTenantCode());
        assertTrue(created.getTenantCode().startsWith("PRAX-"));
        assertEquals("Test Praxis", created.getTenantName());
        assertTrue(created.isActive());
        verify(tenantRepository, times(1)).save(any(Tenant.class));
    }

    @Test
    void testFindByCode_shouldReturnTenant() {
        // Given
        when(tenantRepository.findByTenantCode("TEST-1234")).thenReturn(Optional.of(testTenant));

        // When
        Optional<Tenant> found = tenantService.findByCode("TEST-1234");

        // Then
        assertTrue(found.isPresent());
        assertEquals("TEST-1234", found.get().getTenantCode());
        assertEquals("Test Praxis", found.get().getTenantName());
    }

    @Test
    void testFindByCode_shouldReturnEmptyForNonExistent() {
        // Given
        when(tenantRepository.findByTenantCode("NONEXISTENT")).thenReturn(Optional.empty());

        // When
        Optional<Tenant> found = tenantService.findByCode("NONEXISTENT");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void testDeactivate_shouldSetActiveToFalse() {
        // Given
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(testTenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        tenantService.deactivate(1L);

        // Then
        assertFalse(testTenant.isActive());
        verify(tenantRepository, times(1)).save(testTenant);
    }

    @Test
    void testGenerateTenantCode_shouldBeUnique() {
        // Given
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            if (tenant.getId() == null) {
                tenant.setId(System.currentTimeMillis()); // Simulate different IDs
            }
            return tenant;
        });

        // When
        Tenant tenant1 = tenantService.createTenant("Praxis 1");
        Tenant tenant2 = tenantService.createTenant("Praxis 2");

        // Then
        assertNotNull(tenant1.getTenantCode());
        assertNotNull(tenant2.getTenantCode());
        assertNotEquals(tenant1.getTenantCode(), tenant2.getTenantCode());
    }
}
