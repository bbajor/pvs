package de.bbajor.pvs.intravitreal.treatment.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentAuditLog;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentPlanRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentAuditLogRepository;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.medication.repository.MedicationFavouriteRepository;
import de.bbajor.pvs.medication.service.MedicationFavouriteService;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.repository.SurgicalCenterTimeSlotRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import de.bbajor.pvs.security.CurrentUser;

@Service
public class TreatmentPlanService {

    @Autowired
    private IvomDiagnosisService diagnosisService;
    @Autowired
    private PatientService patientService;

    @Autowired
    private SurgicalCenterTimeSlotRepository surgicalCenterTimeSlotRepository;
    @Autowired
    private TreatmentPlanRepository treatmentPlanRepository;
    @Autowired
    private TreatmentRepository treatmentRepository;
    @Autowired
    private TreatmentAuditLogRepository auditLogRepository;
    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private TreatmentPlanMapper treatmentPlanMapper;
    @Autowired
    private TreatmentMapper treatmentMapper;

    @Autowired
    private InstitutionRepository institutionRepository;
    @Autowired
    private MedicationFavouriteRepository medicationFavouriteRepository;
    @Autowired
    private MedicationFavouriteService medicationFavouriteService;

    @Transactional(readOnly = true)
    public TreatmentPlan findByIdWithDetails(Long id) {
        // Fetch the treatment plan with patient and diagnosis in a single query
        // Get current institution ID for secure access
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getInstitutionId();

        TreatmentPlan treatmentPlan = null;
        if (institutionId != null) {
            treatmentPlan = treatmentPlanRepository.findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(id, institutionId)
                    .orElse(null);

            // If not found via institution-aware query, check if it exists at all (for better error message)
            if (treatmentPlan == null) {
                Optional<TreatmentPlan> existsCheck = treatmentPlanRepository.findById(id);
                if (existsCheck.isPresent()) {
                    // TreatmentPlan exists but doesn't belong to current institution
                    throw new NoSuchElementException(
                            String.format("TreatmentPlan with id %d exists but does not belong to current institution (institutionId: %d)",
                                    id, institutionId));
                }
            }
        }

        // Fallback: if no institution context or not found via institution-aware query, try direct lookup
        if (treatmentPlan == null) {
            treatmentPlan = treatmentPlanRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("TreatmentPlan not found with id: " + id));
        }

        // Fetch treatments separately to avoid lazy loading issues
        List<Treatment> treatments = treatmentRepository
                .findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(id);

        // Set treatments without modifying the collection directly
        treatmentPlan.setTreatments(new ArrayList<>(treatments));

        return treatmentPlan;
    }

    /**
     * Find all treatment plans for the current institution. IMPORTANT: Only
     * returns treatment plans that belong to the current institution to comply
     * with data protection regulations (DSGVO).
     */
    public List<TreatmentPlan> findAll() {
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            // Return only treatment plans for current institution
            return treatmentPlanRepository.findByInstitutionId(institutionId);
        }
        // Fallback: If no institution context, return empty list (for super admin or during initialization)
        // In production, this should throw an exception or require explicit institution context
        return List.of(); // TODO: throw exception or require explicit institution context
    }

    @Transactional
    private Collection<TreatmentPlan> findByPatient(Integer patientId) {
        // Get current institution ID for secure access
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getInstitutionId();

        return institutionId != null
                ? treatmentPlanRepository.findByInstitutionAndPatientId(institutionId, patientId)
                : treatmentPlanRepository.findAll().stream()
                        .filter(tp -> tp.getPatient() != null && tp.getPatient().getId().equals(patientId))
                        .toList();
    }

    /**
     * Find treatment plans matching the search filter for the current
     * institution. IMPORTANT: Only searches within treatment plans that belong
     * to the current institution to comply with data protection regulations
     * (DSGVO).
     * 
     * Searches in: patient first name, patient last name, health insurance name
     * (billing and cost carrier), diagnosis name, additional information, and birth year.
     */
    public List<TreatmentPlan> findTreatmentPlans(String filter) {
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getInstitutionId();

        if (institutionId == null) {
            // No institution context - return empty list (for super admin or during initialization)
            return List.of(); // TODO: throw exception or require explicit institution context
        }

        if (filter == null || filter.trim().isEmpty()) {
            return treatmentPlanRepository.findByInstitutionId(institutionId);
        }

        // Use repository search method for efficient database-level filtering
        List<TreatmentPlan> results = new ArrayList<>(
                treatmentPlanRepository.searchInInstitution(institutionId, filter.trim()));
        
        // Additionally filter by birth year if search term is numeric
        try {
            Integer birthYear = Integer.parseInt(filter.trim());
            // Also include treatment plans matching birth year
            List<TreatmentPlan> allPlans = treatmentPlanRepository.findByInstitutionId(institutionId);
            List<TreatmentPlan> yearMatches = allPlans.stream()
                    .filter(tp -> tp.getBirth() != null && tp.getBirth().getYear() == birthYear)
                    .filter(tp -> !results.contains(tp)) // Avoid duplicates
                    .toList();
            results.addAll(yearMatches);
        } catch (NumberFormatException ignored) {
            // Not a number, ignore - search already handled by repository query
        }
        
        return results;
    }

    /**
     * Generate week list of treatments for the current institution. IMPORTANT:
     * Only returns treatments that belong to the current institution to comply
     * with data protection regulations (DSGVO).
     */
    @Transactional(readOnly = true)
    public List<Treatment> generateWeekList(LocalDate startDate) {
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getInstitutionId();

        if (institutionId == null) {
            // No institution context - return empty list
            return List.of(); // TODO: throw exception or require explicit institution context
        }

        LocalDate monday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = monday.plusDays(6);

        // Get treatments in date range filtered by institution directly in query
        // This is more efficient than loading all and filtering in memory
        List<Treatment> treatments = treatmentRepository
                .findTreatmentsByDateRangeAndInstitution(monday, endOfWeek, institutionId);

        return treatments;
    }

    /**
     * Get treatment slots for a treatment plan. IMPORTANT: Only returns
     * treatments if the treatment plan belongs to the current institution to
     * comply with data protection regulations (DSGVO).
     */
    @Transactional
    public List<Treatment> getTreatmentSlots(Long treatmentPlanId) {
        // First, verify that the treatment plan belongs to current institution
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getInstitutionId();

        if (institutionId != null) {
            // Check if treatment plan belongs to current institution
            Optional<TreatmentPlan> treatmentPlan = treatmentPlanRepository.findByIdAndInstitutionId(treatmentPlanId, institutionId);
            if (treatmentPlan.isEmpty()) {
                // Treatment plan doesn't belong to current institution - return empty list
                return List.of(); // TODO: throw exception or require explicit institution context
            }
        }

        List<Treatment> treatments = treatmentRepository
                .findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(treatmentPlanId);
        return treatments;
    }

    public TreatmentPlan loadTreatmentPlanWithFullDetails(Long id) {
        return findByIdWithDetails(id);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'TECH_USER')")
    public TreatmentPlan saveTreatmentPlan(TreatmentPlan update) throws NoSuchElementException {
        return saveTreatmentPlanInternal(update);
    }

    /**
     * Internal method for saving treatment plans without security checks. Used
     * by test data initialization and other internal operations.
     */
    @Transactional
    public TreatmentPlan saveTreatmentPlanInternal(TreatmentPlan update) throws NoSuchElementException {
        // 1. save treatmentplan without treatments
        TreatmentPlan current;
        if (update.getId() != null) {
            // Load the current treatment plan with all its treatments
            current = treatmentPlanRepository.findById(update.getId())
                    .orElseThrow(() -> new NoSuchElementException("TreatmentPlan not found: " + update.getId()));

            // Clear existing treatments from the plan to avoid orphans
            // First, detach existing treatments from the plan
            if (current.getTreatments() != null) {
                List<Treatment> existingTreatments = new ArrayList<>(current.getTreatments());
                for (Treatment existingTreatment : existingTreatments) {
                    // Don't remove from database yet, just detach from the plan
                    existingTreatment.setTreatmentPlan(null);
                }
                current.getTreatments().clear();
                // Explicitly save to ensure the detachment is persisted
                treatmentRepository.saveAll(existingTreatments);
            }
        } else {
            current = new TreatmentPlan();
        }

        // Set institution from patient (data isolation)
        // This will be set when patient is assigned below, but we need to ensure it's set early
        // for validation purposes
        if (update.getDiagnosis() != null) {
            if (update.getDiagnosis().getId() == null || update.getDiagnosis().getId() <= 0) {
                update.getDiagnosis().setId(null);
                // New diagnosis, save it first
                update.setDiagnosis(diagnosisService.save(update.getDiagnosis()));
                current.setDiagnosis(diagnosisService.getByDiagnoseId(update.getDiagnosis().getId()));
            } else {
                // Existing diagnosis, ensure it exists
                current.setDiagnosis(diagnosisService.getByDiagnoseId(update.getDiagnosis().getId()));
            }
        }

        if (update.getPatient() != null) {
            if (update.getPatient().getId() == null || update.getPatient().getId() <= 0) {
                throw new IllegalArgumentException("Patient ID is required");
            } else {
                Patient patient = patientService.findEntityById(update.getPatient().getId());
                current.setPatient(patient);

                // Set institution from patient (data isolation)
                // Get institution from location (new model)
                if (patient.getLocation() != null && patient.getLocation().getInstitution() != null) {
                    current.setInstitution(patient.getLocation().getInstitution());
                } else {
                    // Patient has no location with institution
                    throw new IllegalStateException(
                            String.format("Cannot save TreatmentPlan: Patient has no location with institution. "
                                    + "Patient ID: %d, Location: %s",
                                    patient.getId(),
                                    patient.getLocation() != null ? "present but no institution" : "null"));
                }
            }
        } else {
            throw new IllegalArgumentException("Patient information is required");
        }

        // Update the treatment plan from the DTO
        treatmentPlanMapper.updateTreatmentPlan(update, current);

        // Save the treatment plan first
        TreatmentPlan saved = treatmentPlanRepository.save(current);

        // 2. Create and save new treatments linked to the treatment plan
        List<Treatment> treatmentEntityList = new ArrayList<>();
        for (Treatment treatment : update.getTreatments()) {
            Treatment treatmentToSave = new Treatment();
            treatmentMapper.updateTreatmentEntity(treatment, treatmentToSave);
            SurgicalCenterTimeSlot surgicalCenterTimeSlot = surgicalCenterTimeSlotRepository
                    .getReferenceById(treatment.getSurgicalCenterTimeSlot().getId());
            if (treatment.getMedicationFavourite() == null || treatment.getMedicationFavourite().getId() == null) {
                throw new IllegalArgumentException("Medikamentenfavorit erforderlich, um eine Behandlung zu speichern.");
            }
            MedicationFavourite medicationFavourite = medicationFavouriteRepository
                    .getReferenceById(treatment.getMedicationFavourite().getId());
            treatmentToSave.setSurgicalCenterTimeSlot(surgicalCenterTimeSlot);
            treatmentToSave.setMedicationFavourite(medicationFavourite);
            treatmentToSave.setTreatmentPlan(saved);
            treatmentEntityList.add(treatmentToSave);
        }

        // Save all new treatments
        treatmentRepository.saveAll(treatmentEntityList);

        // Refresh the treatment plan to get the updated state with treatments
        TreatmentPlan savedTreatmentPlanDto = findByIdWithDetails(saved.getId());
        return savedTreatmentPlanDto;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'TECH_USER')")
    public List<Treatment> saveNewTreatmentsForExistingPlan(List<Treatment> treatmentsToCreate,
            Long treatmentPlanId) {
        return saveNewTreatmentsForExistingPlanInternal(treatmentsToCreate, treatmentPlanId);
    }

    /**
     * Internal method for saving treatments without security checks. Used by
     * test data initialization and other internal operations.
     */
    @Transactional
    public List<Treatment> saveNewTreatmentsForExistingPlanInternal(List<Treatment> treatmentsToCreate,
            Long treatmentPlanId) {

        // Get the treatment plan by ID, ensuring it exists
        TreatmentPlan treatmentPlan = treatmentPlanRepository.findById(treatmentPlanId)
                .orElseThrow(() -> new NoSuchElementException("TreatmentPlan not found with id: " + treatmentPlanId));

        treatmentsToCreate.forEach(e -> e.setTreatmentPlan(treatmentPlan));

        // Save all treatments in a single batch operation
        List<Treatment> saved = treatmentRepository.saveAll(treatmentsToCreate);

        // Audit creation (only if authentication context is available)
        try {
            var actor = currentUser.get().orElse(null);
            saved.forEach(t -> {
                TreatmentAuditLog log = new TreatmentAuditLog();
                log.setTreatment(t);
                log.setActionType(TreatmentAuditLog.ActionType.CREATE);
                log.setActionTimestamp(java.time.LocalDateTime.now());
                if (actor != null) {
                    log.setActorUserId(actor.getUserId().toString());
                    log.setActorUserName(actor.getPreferredUsername());
                }
                auditLogRepository.save(log);
            });
        } catch (Exception ex) {
            // Ignore authentication issues during test data initialization or other contexts without security
        }

        // Update the treatments collection in the treatment plan
        if (treatmentPlan.getTreatments() == null) {
            treatmentPlan.setTreatments(new ArrayList<>(saved));
        } else {
            treatmentPlan.getTreatments().addAll(saved);
        }
        treatmentPlanRepository.save(treatmentPlan);

        return saved;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'TECH_USER')")
    public void deleteTreatment(Long treatmentId) {
        Treatment existing = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new NoSuchElementException("Treatment not found: " + treatmentId));
        treatmentRepository.delete(existing);
        TreatmentAuditLog log = new TreatmentAuditLog();
        log.setTreatment(existing);
        log.setActionType(TreatmentAuditLog.ActionType.DELETE);
        log.setActionTimestamp(java.time.LocalDateTime.now());
        currentUser.get().ifPresent(actor -> {
            log.setActorUserId(actor.getUserId().toString());
            log.setActorUserName(actor.getPreferredUsername());
        });
        auditLogRepository.save(log);
    }

    public List<MedicationFavourite> getFavouriteMedications() {
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return List.of();
        }
        return getFavouriteMedicationsForInstitution(institutionId);
    }

    public List<MedicationFavourite> getFavouriteMedicationsForInstitution(Long institutionId) {
        if (institutionId == null) {
            return List.of();
        }
        // Verwende die Methode mit JOIN FETCH, um die Medication-Entity zu laden
        return medicationFavouriteRepository.findByInstitutionIdAndActiveTrueWithMedication(institutionId);
    }
}
