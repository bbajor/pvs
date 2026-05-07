package de.bbajor.pvs.intravitreal.treatment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentAuditLogRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentPlanRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.medication.repository.MedicationFavouriteRepository;
import de.bbajor.pvs.medication.service.MedicationFavouriteService;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.surgicalcenter.repository.SurgicalCenterTimeSlotRepository;

@ExtendWith(MockitoExtension.class)
class TreatmentPlanServiceTenantIsolationTest {

    private static final Long INSTITUTION_ID = 10L;

    @Mock
    private IvomDiagnosisService diagnosisService;
    @Mock
    private PatientService patientService;
    @Mock
    private SurgicalCenterTimeSlotRepository surgicalCenterTimeSlotRepository;
    @Mock
    private TreatmentPlanRepository treatmentPlanRepository;
    @Mock
    private TreatmentRepository treatmentRepository;
    @Mock
    private TreatmentAuditLogRepository auditLogRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private TreatmentPlanMapper treatmentPlanMapper;
    @Mock
    private TreatmentMapper treatmentMapper;
    @Mock
    private InstitutionRepository institutionRepository;
    @Mock
    private MedicationFavouriteRepository medicationFavouriteRepository;
    @Mock
    private MedicationFavouriteService medicationFavouriteService;
    @Mock
    private CurrentInstitutionService currentInstitutionService;

    @InjectMocks
    private TreatmentPlanService service;

    @BeforeEach
    void setUp() {
        lenient().when(currentInstitutionService.getCurrentInstitutionId()).thenReturn(Optional.of(INSTITUTION_ID));
        lenient().when(currentInstitutionService.getRequiredInstitutionId()).thenReturn(INSTITUTION_ID);
        lenient().when(currentInstitutionService.hasInstitution()).thenReturn(true);
        new InstitutionContext(currentInstitutionService);
    }

    @AfterEach
    void tearDown() {
        InstitutionContext.clear();
        clearInstitutionContextDelegate();
    }

    @Test
    void findByIdWithDetails_whenPlanIsOutsideInstitution_doesNotFallbackToGlobalLookup() {
        Long foreignPlanId = 99L;
        when(treatmentPlanRepository.findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(
                foreignPlanId, INSTITUTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByIdWithDetails(foreignPlanId))
                .isInstanceOf(NoSuchElementException.class);

        verify(treatmentPlanRepository, never()).findById(foreignPlanId);
    }

    @Test
    void saveTreatmentPlanInternal_whenExistingPlanIsOutsideInstitution_doesNotReassignOrSaveIt() {
        Long foreignPlanId = 99L;
        TreatmentPlan update = new TreatmentPlan();
        update.setId(foreignPlanId);
        update.setPatient(patientWithId(1));
        update.setTreatments(List.of());

        when(treatmentPlanRepository.findByIdAndInstitutionId(foreignPlanId, INSTITUTION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveTreatmentPlanInternal(update))
                .isInstanceOf(NoSuchElementException.class);

        verify(patientService, never()).findEntityById(any());
        verify(treatmentPlanRepository, never()).save(any());
    }

    @Test
    void saveNewTreatmentsForExistingPlanInternal_whenPlanIsOutsideInstitution_doesNotAttachTreatments() {
        Long foreignPlanId = 99L;
        when(treatmentPlanRepository.findByIdAndInstitutionId(foreignPlanId, INSTITUTION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveNewTreatmentsForExistingPlanInternal(List.of(new Treatment()), foreignPlanId))
                .isInstanceOf(NoSuchElementException.class);

        verify(treatmentRepository, never()).saveAll(any());
    }

    @Test
    void cancelTreatment_whenTreatmentIsOutsideInstitution_doesNotLoadOrSaveItGlobally() {
        Long foreignTreatmentId = 44L;
        when(treatmentRepository.findByIdAndInstitutionIdWithAllRelationships(foreignTreatmentId, INSTITUTION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelTreatment(foreignTreatmentId, "Patient krank"))
                .isInstanceOf(NoSuchElementException.class);

        verify(treatmentRepository, never()).findById(foreignTreatmentId);
        verify(treatmentRepository, never()).save(any());
    }

    private static Patient patientWithId(Integer id) {
        Patient patient = new Patient();
        patient.setId(id);
        return patient;
    }

    private static void clearInstitutionContextDelegate() {
        try {
            var f = InstitutionContext.class.getDeclaredField("delegate");
            f.setAccessible(true);
            f.set(null, null);
        } catch (Exception ignored) {
            // best-effort cleanup; tests should not depend on a global static delegate
        }
    }
}
