package de.bbajor.pvs.institution.service;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InstitutionService.
 */
@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {

    @Mock
    private InstitutionRepository institutionRepository;

    @InjectMocks
    private InstitutionService institutionService;

    private Institution testInstitution;

    @BeforeEach
    void setUp() {
        testInstitution = new Institution()
                .setInstitutionCode("TEST-1234")
                .setInstitutionName("Test Praxis")
                .setActive(true)
                .setDatabaseName("pvs_inst_test_1234")
                .setContainerName("postgres-inst-test-1234");
        testInstitution.setId(1L);
    }

    @Test
    void testFindByCode_existingInstitution_shouldReturnInstitution() {
        // Given
        when(institutionRepository.findByInstitutionCode("TEST-1234")).thenReturn(Optional.of(testInstitution));

        // When
        Optional<Institution> result = institutionService.findByCode("TEST-1234");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testInstitution, result.get());
        verify(institutionRepository).findByInstitutionCode("TEST-1234");
    }

    @Test
    void testFindByCode_nonExistentInstitution_shouldReturnEmpty() {
        // Given
        when(institutionRepository.findByInstitutionCode("NONEXISTENT")).thenReturn(Optional.empty());

        // When
        Optional<Institution> result = institutionService.findByCode("NONEXISTENT");

        // Then
        assertFalse(result.isPresent());
        verify(institutionRepository).findByInstitutionCode("NONEXISTENT");
    }

    @Test
    void testFindAll_shouldReturnAllInstitutions() {
        // Given
        Institution institution1 = new Institution()
                .setInstitutionCode("INST-001")
                .setInstitutionName("Praxis 1")
                .setDatabaseName("pvs_inst_inst_001")
                .setContainerName("postgres-inst-inst-001");
        Institution institution2 = new Institution()
                .setInstitutionCode("INST-002")
                .setInstitutionName("Praxis 2")
                .setDatabaseName("pvs_inst_inst_002")
                .setContainerName("postgres-inst-inst-002");
        List<Institution> institutions = Arrays.asList(institution1, institution2);
        when(institutionRepository.findAll()).thenReturn(institutions);

        // When
        List<Institution> result = institutionService.findAll();

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(institution1));
        assertTrue(result.contains(institution2));
        verify(institutionRepository).findAll();
    }

    @Test
    void testCreateInstitution_shouldGenerateCodeAndSave() {
        // Given
        Institution newInstitution = new Institution()
                .setInstitutionCode("INST-ABCD1234")
                .setInstitutionName("New Praxis")
                .setActive(true)
                .setDatabaseName("pvs_inst_inst_abcd1234")
                .setContainerName("postgres-inst-inst-abcd1234");
        when(institutionRepository.save(any(Institution.class))).thenAnswer(invocation -> {
            Institution inst = invocation.getArgument(0);
            inst.setId(1L);
            return inst;
        });

        // When
        Institution result = institutionService.createInstitution("New Praxis");

        // Then
        assertNotNull(result);
        assertTrue(result.getInstitutionCode().startsWith("INST-"));
        assertEquals("New Praxis", result.getInstitutionName());
        assertTrue(result.isActive());
        assertNotNull(result.getDatabaseName());
        assertNotNull(result.getContainerName());
        verify(institutionRepository).save(any(Institution.class));
    }

    @Test
    void testSave_shouldCallRepository() {
        // Given
        when(institutionRepository.save(testInstitution)).thenReturn(testInstitution);

        // When
        Institution result = institutionService.save(testInstitution);

        // Then
        assertEquals(testInstitution, result);
        verify(institutionRepository).save(testInstitution);
    }

    @Test
    void testDeactivate_shouldSetActiveToFalse() {
        // Given
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(testInstitution));
        when(institutionRepository.save(testInstitution)).thenReturn(testInstitution);

        // When
        institutionService.deactivate(1L);

        // Then
        assertFalse(testInstitution.isActive());
        verify(institutionRepository).findById(1L);
        verify(institutionRepository).save(testInstitution);
    }

    @Test
    void testDeactivate_nonExistentInstitution_shouldNotThrowException() {
        // Given
        when(institutionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertDoesNotThrow(() -> institutionService.deactivate(999L));
        verify(institutionRepository).findById(999L);
        verify(institutionRepository, never()).save(any());
    }
}

