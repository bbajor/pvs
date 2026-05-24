package de.bbajor.pvs.intravitreal.treatment.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentPlanRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.security.domain.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class TreatmentPlanServiceSecurityTest {

    @Mock
    private TreatmentPlanRepository treatmentPlanRepository;
    @Mock
    private TreatmentRepository treatmentRepository;
    @Mock
    private UserAccountRepository userAccountRepository;

    private TreatmentPlanService service;
    private CurrentInstitutionService currentInstitutionService;

    @BeforeEach
    void setUp() {
        service = new TreatmentPlanService();
        ReflectionTestUtils.setField(service, "treatmentPlanRepository", treatmentPlanRepository);
        ReflectionTestUtils.setField(service, "treatmentRepository", treatmentRepository);
        currentInstitutionService = new CurrentInstitutionService(userAccountRepository);
        new InstitutionContext(currentInstitutionService);
        InstitutionContext.clear();
    }

    @AfterEach
    void tearDown() {
        InstitutionContext.clear();
    }

    @Test
    void findByIdWithDetails_requiresInstitutionContext() {
        assertThrows(IllegalStateException.class, () -> service.findByIdWithDetails(42L));

        verifyNoInteractions(treatmentPlanRepository, treatmentRepository);
    }

    @Test
    void findByIdWithDetails_doesNotFallbackToGlobalLookup() {
        when(treatmentPlanRepository.findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(42L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                currentInstitutionService.runWithInstitutionId(7L, () -> service.findByIdWithDetails(42L)));

        verify(treatmentPlanRepository).findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(42L, 7L);
        verify(treatmentPlanRepository, never()).findById(42L);
        verifyNoInteractions(treatmentRepository);
    }
}
