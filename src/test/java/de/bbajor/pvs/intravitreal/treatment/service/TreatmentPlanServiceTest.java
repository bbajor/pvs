package de.bbajor.pvs.intravitreal.treatment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentPlanRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.security.domain.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class TreatmentPlanServiceTest {

    private static final Long INSTITUTION_ID = 42L;
    private static final Long TREATMENT_PLAN_ID = 7L;

    @Mock
    private TreatmentPlanRepository treatmentPlanRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private TreatmentPlanService treatmentPlanService;

    private CurrentInstitutionService currentInstitutionService;

    @BeforeEach
    void setUp() {
        currentInstitutionService = new CurrentInstitutionService(userAccountRepository);
        new InstitutionContext(currentInstitutionService);
    }

    @AfterEach
    void tearDown() {
        InstitutionContext.clear();
        clearInstitutionContextDelegate();
    }

    @Test
    void findByIdWithDetails_withoutInstitutionContext_shouldFailClosed() {
        assertThatThrownBy(() -> treatmentPlanService.findByIdWithDetails(TREATMENT_PLAN_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No institution");

        verifyNoInteractions(treatmentPlanRepository, treatmentRepository);
    }

    @Test
    void findByIdWithDetails_withForeignInstitutionPlan_shouldNotUseGlobalFallback() {
        currentInstitutionService.setThreadLocalInstitutionId(INSTITUTION_ID);
        when(treatmentPlanRepository.findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(
                TREATMENT_PLAN_ID, INSTITUTION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> treatmentPlanService.findByIdWithDetails(TREATMENT_PLAN_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("TreatmentPlan not found");

        verify(treatmentPlanRepository).findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(
                TREATMENT_PLAN_ID, INSTITUTION_ID);
        verify(treatmentPlanRepository, never()).findById(TREATMENT_PLAN_ID);
        verifyNoInteractions(treatmentRepository);
    }

    @Test
    void findByIdWithDetails_withInstitutionContext_shouldLoadScopedPlanAndTreatments() {
        currentInstitutionService.setThreadLocalInstitutionId(INSTITUTION_ID);
        TreatmentPlan treatmentPlan = new TreatmentPlan();
        treatmentPlan.setId(TREATMENT_PLAN_ID);
        Treatment treatment = new Treatment();

        when(treatmentPlanRepository.findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(
                TREATMENT_PLAN_ID, INSTITUTION_ID))
                .thenReturn(Optional.of(treatmentPlan));
        when(treatmentRepository.findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(TREATMENT_PLAN_ID))
                .thenReturn(List.of(treatment));

        TreatmentPlan result = treatmentPlanService.findByIdWithDetails(TREATMENT_PLAN_ID);

        assertThat(result).isSameAs(treatmentPlan);
        assertThat(result.getTreatments()).containsExactly(treatment);
    }

    private static void clearInstitutionContextDelegate() {
        try {
            var field = InstitutionContext.class.getDeclaredField("delegate");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception ignored) {
            // Best-effort cleanup for the static legacy bridge.
        }
    }
}
