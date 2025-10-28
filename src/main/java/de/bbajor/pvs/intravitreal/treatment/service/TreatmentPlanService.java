package de.bbajor.pvs.intravitreal.treatment.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentAuditLog;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentPlanRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentAuditLogRepository;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.repository.MedicationRepository;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.repository.SurgicalCenterTimeSlotRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.security.access.prepost.PreAuthorize;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.AppUserInfo;

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
    private MedicationRepository medicationRepository;
    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private TreatmentPlanMapper treatmentPlanMapper;
    @Autowired
    private TreatmentMapper treatmentMapper;
    
    @Transactional(readOnly = true)
    public TreatmentPlan findByIdWithDetails(Long id) {
        // Fetch the treatment plan with patient and diagnosis in a single query
        TreatmentPlan treatmentPlan = treatmentPlanRepository.findTreatmentPlanByIdWithPatientDiagnosis(id)
                .orElseThrow(() -> new NoSuchElementException("TreatmentPlan not found with id: " + id));

        // Fetch treatments separately to avoid lazy loading issues
        List<Treatment> treatments = treatmentRepository
                .findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(id);

        // Set treatments without modifying the collection directly
        treatmentPlan.setTreatments(new ArrayList<>(treatments));

        return treatmentPlan;
    }

    public List<TreatmentPlan> findAll() {
        return treatmentPlanRepository.findAll();
    }

    @Transactional
    private Collection<TreatmentPlan> findByPatient(Integer patientId) {
        return treatmentPlanRepository.findByPatientId(patientId);
    }

    public List<TreatmentPlan> findTreatmentPlans(String filter) {
        Specification<TreatmentPlan> spec = (root, query, cb) -> {
            String likeFilter = "%" + filter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("description")), likeFilter),
                    cb.like(cb.lower(root.get("additionalInformation")), likeFilter),
                    cb.like(cb.lower(root.get("patient").get("firstName")), likeFilter),
                    cb.like(cb.lower(root.get("patient").get("lastName")), likeFilter)));
            // TODO add more fields like name of health insurance, ivom type etc.
            try {
                Integer birthFilter = Integer.parseInt(filter);
                predicates.add(cb.equal(root.get("birth"), birthFilter));
            } catch (NumberFormatException ignored) {
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
        return treatmentPlanRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public List<Treatment> generateWeekList(LocalDate startDate) {
        LocalDate monday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = monday.plusDays(6);
        return treatmentRepository
                .findTreatmentsByDateRangeWithSurgicalCenterAndTreatmentPlan(monday, endOfWeek);
    }

    @Transactional
    public List<Treatment> getTreatmentSlots(Long treatmentPlanId) {
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
            Medication medication = medicationRepository.getReferenceById(treatment.getMedication().getId());
            treatmentToSave.setSurgicalCenterTimeSlot(surgicalCenterTimeSlot);
            treatmentToSave.setMedication(medication);
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

    public List<Medication> getFavouriteMedications() {
        return medicationRepository.findAllByIsFavouriteTrue();
    }
}
