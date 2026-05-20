package de.bbajor.pvs.intravitreal.treatment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class TreatmentPlanServiceTest {

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
    }

    @AfterEach
    void tearDown() {
        InstitutionContext.clear();
        clearInstitutionContextDelegate();
    }

    @Test
    void findByIdWithDetailsRequiresInstitutionContextBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.findByIdWithDetails(99L))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(treatmentPlanRepository, treatmentRepository);
    }

    @Test
    void findByIdWithDetailsDoesNotFallbackToUnscopedLookup() {
        when(treatmentPlanRepository.findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(99L, 42L))
                .thenReturn(Optional.empty());

        currentInstitutionService.runWithInstitutionId(42L, () ->
                assertThatThrownBy(() -> service.findByIdWithDetails(99L))
                        .isInstanceOf(NoSuchElementException.class));

        verify(treatmentPlanRepository).findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(99L, 42L);
        verify(treatmentPlanRepository, never()).findById(99L);
        verifyNoInteractions(treatmentRepository);
    }

    private static void clearInstitutionContextDelegate() {
        try {
            var field = InstitutionContext.class.getDeclaredField("delegate");
            field.setAccessible(true);
            field.set(null, null);
        } catch (ReflectiveOperationException ignored) {
            // Tests should not depend on a global static delegate.
        }
    }
}
