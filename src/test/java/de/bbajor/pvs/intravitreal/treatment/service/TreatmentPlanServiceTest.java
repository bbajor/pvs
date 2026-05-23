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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentPlanRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.security.domain.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class TreatmentPlanServiceTest {

    @Mock
    private TreatmentPlanRepository treatmentPlanRepository;
    @Mock
    private TreatmentRepository treatmentRepository;
    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private TreatmentPlanService service;

    private CurrentInstitutionService currentInstitutionService;

    @BeforeEach
    void setUp() {
        currentInstitutionService = new CurrentInstitutionService(userAccountRepository);
        new InstitutionContext(currentInstitutionService);
    }

    @AfterEach
    void tearDown() {
        InstitutionContext.clear();
    }

    @Test
    void findByIdWithDetails_withoutInstitutionContext_doesNotUseGlobalFallback() {
        assertThrows(IllegalStateException.class, () -> service.findByIdWithDetails(99L));

        verifyNoInteractions(treatmentPlanRepository, treatmentRepository);
    }

    @Test
    void findByIdWithDetails_whenPlanIsOutsideInstitution_doesNotUseGlobalFallback() {
        InstitutionContext.setInstitutionId(10L);
        when(treatmentPlanRepository.findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(99L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.findByIdWithDetails(99L));

        verify(treatmentPlanRepository).findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(99L, 10L);
        verify(treatmentPlanRepository, never()).findById(99L);
        verifyNoInteractions(treatmentRepository);
    }
}
