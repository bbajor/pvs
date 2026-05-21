package de.bbajor.pvs.intravitreal.treatment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
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

@ExtendWith(MockitoExtension.class)
class TreatmentPlanServiceTest {

    @Mock
    private TreatmentPlanRepository treatmentPlanRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private CurrentInstitutionService currentInstitutionService;

    private TreatmentPlanService treatmentPlanService;

    @BeforeEach
    void setUp() {
        treatmentPlanService = new TreatmentPlanService();
        ReflectionTestUtils.setField(treatmentPlanService, "treatmentPlanRepository", treatmentPlanRepository);
        ReflectionTestUtils.setField(treatmentPlanService, "treatmentRepository", treatmentRepository);
        new InstitutionContext(currentInstitutionService);
    }

    @AfterEach
    void tearDown() {
        InstitutionContext.clear();
        clearInstitutionContextDelegate();
    }

    @Test
    void findByIdWithDetails_withoutInstitutionContextDoesNotUseUnscopedLookup() {
        when(currentInstitutionService.getCurrentInstitutionId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> treatmentPlanService.findByIdWithDetails(123L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No institution");

        verifyNoInteractions(treatmentPlanRepository, treatmentRepository);
    }

    @Test
    void findByIdWithDetails_whenScopedQueryMissesDoesNotFallbackToFindById() {
        when(currentInstitutionService.getCurrentInstitutionId()).thenReturn(Optional.of(7L));
        when(treatmentPlanRepository.findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(123L, 7L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> treatmentPlanService.findByIdWithDetails(123L))
                .isInstanceOf(NoSuchElementException.class);

        verify(treatmentPlanRepository).findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(123L, 7L);
        verify(treatmentPlanRepository, never()).findById(123L);
        verifyNoInteractions(treatmentRepository);
    }

    private static void clearInstitutionContextDelegate() {
        try {
            Field delegate = InstitutionContext.class.getDeclaredField("delegate");
            delegate.setAccessible(true);
            delegate.set(null, null);
        } catch (ReflectiveOperationException ignored) {
            // Best-effort cleanup for the static compatibility bridge.
        }
    }
}
