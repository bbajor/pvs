package de.bbajor.pvs.intravitreal.treatment.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentAuditLog;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentStatus;
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
    
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public TreatmentPlan findByIdWithDetails(Long id) {
        // Fetch the treatment plan with patient and diagnosis in a single query
        // Get current institution ID for secure access
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getRequiredInstitutionId();

        TreatmentPlan treatmentPlan = treatmentPlanRepository
                .findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(id, institutionId)
                .orElseThrow(() -> new NoSuchElementException("TreatmentPlan not found with id: " + id));

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
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getRequiredInstitutionId();
        return treatmentPlanRepository.findByInstitutionId(institutionId);
    }
    
    public org.springframework.data.domain.Slice<TreatmentPlan> findAll(org.springframework.data.domain.Pageable pageable) {
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getRequiredInstitutionId();
        return treatmentPlanRepository.findAllByInstitutionId(institutionId, pageable);
    }

    @Transactional
    private Collection<TreatmentPlan> findByPatient(Integer patientId) {
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getRequiredInstitutionId();
        return treatmentPlanRepository.findByInstitutionAndPatientId(institutionId, patientId);
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
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getRequiredInstitutionId();

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
    
    public org.springframework.data.domain.Slice<TreatmentPlan> findTreatmentPlans(String filter, org.springframework.data.domain.Pageable pageable) {
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getRequiredInstitutionId();

        if (filter == null || filter.trim().isEmpty()) {
            return findAll(pageable);
        }

        // For paging, use repository search method directly
        // Note: Birth year filtering is not supported with paging for simplicity
        return treatmentPlanRepository.searchInInstitution(institutionId, filter.trim(), pageable);
    }

    /**
     * Generate week list of treatments for the current institution. IMPORTANT:
     * Only returns treatments that belong to the current institution to comply
     * with data protection regulations (DSGVO).
     */
    @Transactional(readOnly = true)
    public List<Treatment> generateWeekList(LocalDate startDate) {
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getRequiredInstitutionId();

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
        Long institutionId = de.bbajor.pvs.institution.context.InstitutionContext.getRequiredInstitutionId();
        Optional<TreatmentPlan> treatmentPlan = treatmentPlanRepository.findByIdAndInstitutionId(treatmentPlanId, institutionId);
        if (treatmentPlan.isEmpty()) {
            throw new IllegalStateException("Treatment plan not accessible for current institution");
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
        // Ensure InstitutionContext is set before accessing patient data
        ensureInstitutionContextForTreatmentPlan(update);
        
        // 1. save treatmentplan without treatments
        TreatmentPlan current;
        if (update.getId() != null) {
            // Load the current treatment plan with all its treatments
            current = treatmentPlanRepository.findById(update.getId())
                    .orElseThrow(() -> new NoSuchElementException("TreatmentPlan not found: " + update.getId()));

            // WICHTIG: Existierende Treatments NICHT trennen, da sie bereits in der DB sind
            // (z.B. über NextTreatmentBookingDialog gebucht). Sie bleiben erhalten.
        } else {
            current = new TreatmentPlan();
        }

        // Set institution from patient (data isolation)
        // This will be set when patient is assigned below, but we need to ensure it's set early
        // for validation purposes
        // WICHTIG: Diagnosis muss immer aktualisiert werden, auch wenn sie null ist (Löschen)
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
        } else {
            // Diagnosis wurde auf null gesetzt (gelöscht) - setze sie auch im current auf null
            current.setDiagnosis(null);
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

        // Store institution before mapper call (mapper might overwrite it)
        Institution institutionToPreserve = current.getInstitution();

        // Update the treatment plan from the DTO
        treatmentPlanMapper.updateTreatmentPlan(update, current);

        // Restore institution after mapper call (mapper might have set it to null)
        if (institutionToPreserve != null) {
            current.setInstitution(institutionToPreserve);
        }

        // Save the treatment plan first
        TreatmentPlan saved = treatmentPlanRepository.save(current);

        // 2. Lade alle existierenden Treatments für diesen Plan (inkl. der, die bereits in der DB sind)
        List<Treatment> existingTreatments = treatmentRepository.findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(saved.getId());
        Set<Long> existingTreatmentIds = existingTreatments.stream()
                .map(Treatment::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        // 3. Erstelle und speichere nur neue Treatments aus update.getTreatments()
        // (die noch nicht in der DB existieren)
        List<Treatment> treatmentEntityList = new ArrayList<>();
        for (Treatment treatment : update.getTreatments()) {
            // Überspringe Treatments, die bereits in der DB existieren (haben eine ID)
            if (treatment.getId() != null && existingTreatmentIds.contains(treatment.getId())) {
                continue;
            }
            
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

        // Save all new treatments (nur die, die noch nicht existieren)
        if (!treatmentEntityList.isEmpty()) {
            treatmentRepository.saveAll(treatmentEntityList);
        }

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
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'TECH_USER', 'MEDICAL_STAFF', 'OWNER')")
    public void deleteTreatment(Long treatmentId) {
        // Stelle sicher, dass InstitutionContext gesetzt ist
        ensureInstitutionContextForTreatment(treatmentId);
        
        // Lade Treatment mit allen Beziehungen in einem Query, um sicherzustellen,
        // dass alles im Persistence-Kontext ist
        Treatment existing = treatmentRepository.findByIdWithAllRelationships(treatmentId)
                .orElseThrow(() -> new NoSuchElementException("Treatment not found: " + treatmentId));
        
        // Validierung: Nur löschen, wenn Termin mindestens 2 Tage in der Zukunft liegt
        LocalDate treatmentDate = existing.getDate();
        LocalDate today = LocalDate.now();
        if (treatmentDate == null || treatmentDate.isBefore(today) || treatmentDate.equals(today)) {
            throw new IllegalArgumentException("Behandlung kann nur gelöscht werden, wenn der Termin mindestens 2 Tage in der Zukunft liegt");
        }
        
        // Prüfe, ob mindestens 2 Tage bis zum Termin verbleiben
        long daysUntilTreatment = java.time.temporal.ChronoUnit.DAYS.between(today, treatmentDate);
        if (daysUntilTreatment < 2) {
            throw new IllegalArgumentException("Behandlung kann nicht mehr gelöscht werden: Weniger als 2 Tage bis zum Termin. Bitte verwenden Sie 'Absagen'.");
        }
        
        // WICHTIG: Setze treatment_id in bestehenden Audit-Logs auf null, bevor das Treatment gelöscht wird
        // Dies verhindert Foreign Key Constraint-Verletzungen
        List<TreatmentAuditLog> existingLogs = auditLogRepository.findByTreatmentOrderByActionTimestampAsc(existing);
        for (TreatmentAuditLog existingLog : existingLogs) {
            existingLog.setTreatment(null);
            auditLogRepository.save(existingLog);
        }
        entityManager.flush(); // Flushe sofort, damit die Änderungen gespeichert sind
        
        // Audit-Log VOR dem Löschen erstellen und speichern
        // Setze treatment explizit auf null, da das Treatment gleich gelöscht wird
        TreatmentAuditLog log = new TreatmentAuditLog();
        log.setTreatment(null); // Explizit null setzen
        log.setActionType(TreatmentAuditLog.ActionType.DELETE);
        log.setActionTimestamp(java.time.LocalDateTime.now());
        currentUser.get().ifPresent(actor -> {
            log.setActorUserId(actor.getUserId().toString());
            log.setActorUserName(actor.getPreferredUsername());
        });
        // Speichere wichtige Informationen in den Details, da die Treatment-Referenz null ist
        String details = String.format("Geplante Behandlung fristgerecht gelöscht (Treatment-ID: %d, Datum: %s)", 
                treatmentId, 
                treatmentDate != null ? treatmentDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy", java.util.Locale.GERMAN)) : "unbekannt");
        log.setDetails(details);
        
        auditLogRepository.save(log);
        entityManager.flush(); // Flushe sofort, damit das Audit-Log vor dem Löschen gespeichert ist
        
        // Jetzt erst das Treatment löschen
        treatmentRepository.delete(existing);
    }
    
    /**
     * Sagt eine Behandlung ab. Kann verwendet werden, wenn weniger als 24 Stunden bis zum Termin verbleiben.
     * Der Termin wird nicht gelöscht, sondern als abgesagt markiert.
     * 
     * @param treatmentId Die ID der Behandlung
     * @param cancellationReason Der Grund für die Absage (muss angegeben werden)
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'TECH_USER', 'MEDICAL_STAFF', 'OWNER')")
    public void cancelTreatment(Long treatmentId, String cancellationReason) {
        Objects.requireNonNull(cancellationReason, "Absagegrund muss angegeben werden");
        if (cancellationReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Absagegrund darf nicht leer sein");
        }
        
        // Stelle sicher, dass InstitutionContext gesetzt ist
        ensureInstitutionContextForTreatment(treatmentId);
        
        Treatment existing = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new NoSuchElementException("Treatment not found: " + treatmentId));
        
        // Validierung: Nur absagen, wenn Termin in der Zukunft liegt
        LocalDate treatmentDate = existing.getDate();
        LocalDate today = LocalDate.now();
        if (treatmentDate == null || treatmentDate.isBefore(today)) {
            throw new IllegalArgumentException("Behandlung kann nicht abgesagt werden: Termin liegt in der Vergangenheit");
        }
        
        // Setze Status auf PATIENT_CANCELLED
        existing.setTreatmentStatus(TreatmentStatus.PATIENT_CANCELLED);
        
        // Speichere Absagegrund in additionalInfo (mit Präfix für bessere Erkennbarkeit)
        String existingInfo = existing.getAdditionalInfo() != null ? existing.getAdditionalInfo() : "";
        String cancellationInfo = "ABSAGE: " + cancellationReason.trim();
        if (!existingInfo.isEmpty()) {
            existing.setAdditionalInfo(existingInfo + "\n\n" + cancellationInfo);
        } else {
            existing.setAdditionalInfo(cancellationInfo);
        }
        
        // Audit-Log erstellen
        TreatmentAuditLog log = new TreatmentAuditLog();
        log.setTreatment(existing);
        log.setActionType(TreatmentAuditLog.ActionType.DELETE); // Verwende DELETE für Absage
        log.setActionTimestamp(java.time.LocalDateTime.now());
        currentUser.get().ifPresent(actor -> {
            log.setActorUserId(actor.getUserId().toString());
            log.setActorUserName(actor.getPreferredUsername());
        });
        log.setDetails("Behandlung abgesagt. Grund: " + cancellationReason.trim());
        auditLogRepository.save(log);
        
        // Behandlung speichern
        treatmentRepository.save(existing);
    }
    
    /**
     * Finish a treatment plan by setting the finishedDate.
     * A treatment plan can only be finished if no future treatment appointments are scheduled.
     * <p>
     * Data isolation: Ensures the treatment plan belongs to the current institution.
     * </p>
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'TECH_USER')")
    public void finishTreatmentPlan(Long treatmentPlanId) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot finish treatment plan without institution context");
        }
        
        TreatmentPlan treatmentPlan = treatmentPlanRepository
                .findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(treatmentPlanId, institutionId)
                .orElseThrow(() -> new NoSuchElementException(
                        "TreatmentPlan not found or does not belong to current institution: " + treatmentPlanId));
        
        // Prüfe, ob bereits abgeschlossen
        if (treatmentPlan.getFinishedDate() != null) {
            throw new IllegalStateException("Treatment plan is already finished");
        }
        
        // Prüfe, ob noch zukünftige Termine anstehen
        List<Treatment> treatments = treatmentRepository
                .findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(treatmentPlanId);
        LocalDate today = LocalDate.now();
        boolean hasFutureTreatments = treatments.stream()
                .anyMatch(t -> t.getDate() != null && t.getDate().isAfter(today));
        
        if (hasFutureTreatments) {
            throw new IllegalStateException(
                    "Treatment plan cannot be finished: There are still future treatment appointments scheduled");
        }
        
        // Setze finishedDate
        treatmentPlan.setFinishedDate(today);
        treatmentPlanRepository.save(treatmentPlan);
    }

    /**
     * Ensures InstitutionContext is set before accessing treatment data.
     * Tries to get institution from Treatment if available.
     */
    private void ensureInstitutionContextForTreatment(Long treatmentId) {
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Treatment treatment = treatmentRepository.findById(treatmentId).orElse(null);
        if (treatment != null && treatment.getTreatmentPlan() != null 
                && treatment.getTreatmentPlan().getInstitution() != null
                && treatment.getTreatmentPlan().getInstitution().getId() != null) {
            InstitutionContext.setInstitutionId(treatment.getTreatmentPlan().getInstitution().getId());
        }
    }
    
    /**
     * Ensures InstitutionContext is set before accessing patient data.
     * Tries to get institution from TreatmentPlan if available.
     * Note: Does not access patient data to avoid circular dependency.
     */
    private void ensureInstitutionContextForTreatmentPlan(TreatmentPlan treatmentPlan) {
        // Only set if not already set
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        // Try to get institution from TreatmentPlan
        if (treatmentPlan != null) {
            // First try from existing TreatmentPlan's institution
            if (treatmentPlan.getInstitution() != null && treatmentPlan.getInstitution().getId() != null) {
                InstitutionContext.setInstitutionId(treatmentPlan.getInstitution().getId());
                return;
            }
            
            // If TreatmentPlan has an ID, load it to get the institution
            // This doesn't require InstitutionContext as it's a direct repository call
            if (treatmentPlan.getId() != null) {
                Optional<TreatmentPlan> existingPlan = treatmentPlanRepository.findById(treatmentPlan.getId());
                if (existingPlan.isPresent() && existingPlan.get().getInstitution() != null 
                    && existingPlan.get().getInstitution().getId() != null) {
                    InstitutionContext.setInstitutionId(existingPlan.get().getInstitution().getId());
                    return;
                }
            }
        }
        
        // If we still don't have a context, it will be set later when patient is loaded
        // and the error will be thrown with a clear message
    }

    public List<MedicationFavourite> getFavouriteMedications() {
        Long institutionId = InstitutionContext.getInstitutionId();
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
