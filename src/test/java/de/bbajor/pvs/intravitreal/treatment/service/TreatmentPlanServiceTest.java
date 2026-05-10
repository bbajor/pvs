package de.bbajor.pvs.intravitreal.treatment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
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

    private TreatmentPlanService treatmentPlanService;

    @BeforeEach
    void setUp() {
        treatmentPlanService = new TreatmentPlanService();
        ReflectionTestUtils.setField(treatmentPlanService, "treatmentPlanRepository", treatmentPlanRepository);
        ReflectionTestUtils.setField(treatmentPlanService, "treatmentRepository", treatmentRepository);

        CurrentInstitutionService currentInstitutionService = new CurrentInstitutionService(userAccountRepository);
        new InstitutionContext(currentInstitutionService);
        InstitutionContext.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        InstitutionContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void findByIdWithDetailsRequiresInstitutionContext() {
        assertThrows(IllegalStateException.class, () -> treatmentPlanService.findByIdWithDetails(42L));

        verifyNoInteractions(treatmentPlanRepository, treatmentRepository);
    }

    @Test
    void findByIdWithDetailsUsesInstitutionScopedLookup() {
        InstitutionContext.setInstitutionId(7L);
        TreatmentPlan plan = new TreatmentPlan();
        plan.setId(42L);
        Treatment treatment = new Treatment();

        when(treatmentPlanRepository.findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(42L, 7L))
                .thenReturn(Optional.of(plan));
        when(treatmentRepository.findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(42L))
                .thenReturn(List.of(treatment));

        TreatmentPlan result = treatmentPlanService.findByIdWithDetails(42L);

        assertSame(plan, result);
        assertEquals(List.of(treatment), result.getTreatments());
        verify(treatmentPlanRepository, never()).findById(anyLong());
    }

    @Test
    void findByIdWithDetailsDoesNotFallbackToGlobalLookupWhenScopedLookupMisses() {
        InstitutionContext.setInstitutionId(7L);

        when(treatmentPlanRepository.findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(42L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> treatmentPlanService.findByIdWithDetails(42L));

        verify(treatmentPlanRepository, never()).findById(anyLong());
        verifyNoInteractions(treatmentRepository);
    }
}
