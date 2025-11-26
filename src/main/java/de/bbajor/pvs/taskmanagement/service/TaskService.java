package de.bbajor.pvs.taskmanagement.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.base.util.DateAndTimeUtils;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.service.InstitutionAccessValidator;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentAuditLog;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentAuditLogRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;
import de.bbajor.pvs.taskmanagement.domain.Task;
import de.bbajor.pvs.taskmanagement.domain.TaskRepository;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private SurgicalCenterService surgicalCenterService;
    @Autowired
    private TreatmentPlanService treatmentPlanService;
    @Autowired
    private TreatmentRepository treatmentRepository;
    @Autowired
    private TreatmentAuditLogRepository auditLogRepository;

    @Autowired
    private InstitutionAccessValidator institutionAccessValidator;
    
    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private Clock clock;

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'OWNER') or hasRole('SYSTEM')")
    public void createTask(String description, @Nullable LocalDate dueDate, SurgicalCenterTimeSlot timeSlot) {
        createTaskInternal(description, dueDate, timeSlot);
    }

    /**
     * Internal method for creating tasks without security checks.
     * Used by scheduled tasks and other internal operations.
     * 
     * Note: Only one Task can exist per TimeSlot due to @OneToOne relationship.
     * If a Task already exists for the TimeSlot, it will not create a duplicate.
     */
    @Transactional
    public void createTaskInternal(String description, @Nullable LocalDate dueDate, SurgicalCenterTimeSlot timeSlot) {
        if ("fail".equals(description)) {
            throw new RuntimeException("This is for testing the error handler");
        }
        
        // Check if a task already exists for this time slot
        // Due to @OneToOne relationship, only one task per time slot is allowed
        if (timeSlot != null && timeSlot.getId() != null) {
            java.util.Optional<Task> existingTask = taskRepository.findByTimeSlotId(timeSlot.getId());
            
            if (existingTask.isPresent()) {
                // Task already exists for this time slot - skip creation
                org.slf4j.LoggerFactory.getLogger(TaskService.class)
                        .debug("Task already exists for time slot {} - skipping creation", timeSlot.getId());
                return;
            }
        }
        
        var task = new Task();
        task.setDescription(description);
        task.setCreationDate(clock.instant());
        task.setDueDate(dueDate);
        task.setTimeSlot(timeSlot);
        taskRepository.saveAndFlush(task);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'OWNER') or hasRole('SYSTEM')")
    public void createDailyTaskIfAny() {
        createDailyTaskIfAnyInternal();
    }

    /**
     * Internal method for creating daily tasks without security checks.
     * Used by scheduled tasks and startup listeners.
     * Creates tasks for all institutions separately.
     * If institutionId is null, uses InstitutionContext.
     */
    @Transactional
    public void createDailyTaskIfAnyInternal() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            // No institution context - cannot create tasks
            return;
        }
        createDailyTaskIfAnyForInstitution(institutionId);
    }

    /**
     * Creates daily tasks for a specific institution.
     * Used by scheduled tasks to iterate over all institutions.
     */
    @Transactional
    public void createDailyTaskIfAnyForInstitution(Long institutionId) {
        if (institutionId == null) {
            return;
        }
        
        // Temporarily set InstitutionContext for this operation
        Long previousInstitutionId = InstitutionContext.getInstitutionId();
        try {
            InstitutionContext.setInstitutionId(institutionId);
            
            // 1. Find all timeslots containing not approved treatments until today for this institution
            List<Long> timeSlotIds = new ArrayList<>();
            List<Task> tasks = taskRepository.getTasksWhereExistsNotApprovedTreatment(institutionId, LocalDate.now(clock));
            // Collect the time slot IDs from the tasks, not the task IDs
            tasks.stream()
                    .map(Task::getTimeSlot)
                    .filter(ts -> ts != null && ts.getId() != null)
                    .map(SurgicalCenterTimeSlot::getId)
                    .forEach(timeSlotIds::add);
            List<SurgicalCenterTimeSlot> newTimeSlotsforNewTasks = surgicalCenterService
                    .getNewTimeSlotsContainingNotApprovedTreatments(timeSlotIds);
            newTimeSlotsforNewTasks.forEach(ts -> {
                String description = "Behandlungen vom "
                        + DateAndTimeUtils.getGermanDateTimeFormatter().format(ts.getDate()) + " um " + ts.getStartTime()
                        + " im " + ts.getSurgicalCenter().getName() + " wurden noch nicht dokumentiert.";
                // Setze das Datum eine Woche in die Zukunft
                LocalDate dueDate = ts.getDate().plusDays(7);
                createTaskInternal(description, dueDate, ts);
            });
        } finally {
            // Restore previous InstitutionContext
            if (previousInstitutionId != null) {
                InstitutionContext.setInstitutionId(previousInstitutionId);
            } else {
                InstitutionContext.clear();
            }
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MEDICAL_STAFF', 'DOCTOR', 'OWNER')")
    public void approveTreatment(Long treatmentId, String actorUserId, String actorUserName, boolean secondApproval) {
        Objects.requireNonNull(treatmentId);
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + treatmentId));
        
        // Get current user's authentication
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Keine Authentifizierung gefunden.");
        }
        
        // Get current user's roles
        java.util.Set<String> userRoles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(java.util.stream.Collectors.toSet());
        
        // Get current user account to check if they are a treating doctor
        de.bbajor.pvs.security.domain.UserAccount currentUser = userAccountRepository
                .findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Benutzer nicht gefunden: " + auth.getName()));
        
        if (!secondApproval) {
            // First approval: Must be done by a treating doctor
            if (treatment.getTreatingDoctors() == null || treatment.getTreatingDoctors().isEmpty()) {
                throw new IllegalStateException(
                    "Die Behandlung hat keinen zugewiesenen behandelnden Arzt. " +
                    "Bitte weisen Sie zuerst einen behandelnden Arzt zu.");
            }
            
            boolean isTreatingDoctor = treatment.getTreatingDoctors().stream()
                    .anyMatch(doctor -> doctor.getId().equals(currentUser.getId()));
            
            if (!isTreatingDoctor) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "Die erste Genehmigung muss vom behandelnden Arzt durchgeführt werden. " +
                    "Sie sind nicht als behandelnder Arzt für diese Behandlung zugewiesen.");
            }
        } else {
            // Second approval: Can be done by MFA, OWNER, or another doctor (not the first approver)
            if (treatment.getApprovedByUserId() == null) {
                throw new IllegalStateException(
                    "Die Behandlung wurde noch nicht erstmalig genehmigt. " +
                    "Bitte führen Sie zuerst die erste Genehmigung durch.");
            }
            
            // Check if current user is the first approver
            if (actorUserId != null && actorUserId.equals(treatment.getApprovedByUserId())) {
                throw new IllegalStateException(
                    "Die Zweitprüfung darf nicht vom selben Benutzer durchgeführt werden, " +
                    "der die erste Genehmigung durchgeführt hat.");
            }
            
            // Check if user has valid role for second approval
            boolean hasValidRole = userRoles.contains("MEDICAL_STAFF") || 
                                  userRoles.contains("OWNER") || 
                                  userRoles.contains("DOCTOR");
            
            if (!hasValidRole) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "Die Zweitprüfung kann nur von MFA, Inhaber oder einem Arzt durchgeführt werden. " +
                    "Ihre Rolle: " + String.join(", ", userRoles));
            }
        }
        
        // Validate institution context: ensure treatment belongs to current institution
        if (treatment.getTreatmentPlan() == null || treatment.getTreatmentPlan().getInstitution() == null) {
            throw new IllegalStateException("Treatment " + treatmentId + " has no treatment plan or institution");
        }
        Long treatmentInstitutionId = treatment.getTreatmentPlan().getInstitution().getId();
        institutionAccessValidator.validateInstitutionAccess(treatmentInstitutionId, "Treatment", treatmentId);
        
        if (!secondApproval) {
            treatment.setApprovalDate(LocalDate.now(clock));
            treatment.setApprovalDateTime(clock.instant().atZone(clock.getZone()).toLocalDateTime());
            treatment.setApprovedByUserId(actorUserId);
            treatment.setApprovedByUserName(actorUserName);
        } else {
            if (actorUserId != null && actorUserId.equals(treatment.getApprovedByUserId())) {
                throw new IllegalStateException("Zweitprüfung darf nicht vom selben Arzt durchgeführt werden.");
            }
            treatment.setSecondApprovalDateTime(clock.instant().atZone(clock.getZone()).toLocalDateTime());
            treatment.setSecondApprovedByUserId(actorUserId);
            treatment.setSecondApprovedByUserName(actorUserName);
        }
        treatmentRepository.save(treatment);

        TreatmentAuditLog log = new TreatmentAuditLog();
        log.setTreatment(treatment);
        log.setActionType(secondApproval ? TreatmentAuditLog.ActionType.APPROVE_SECOND
                : TreatmentAuditLog.ActionType.APPROVE);
        log.setActionTimestamp(clock.instant().atZone(clock.getZone()).toLocalDateTime());
        log.setActorUserId(actorUserId);
        log.setActorUserName(actorUserName);
        auditLogRepository.save(log);

        // if all treatments of the same time slot are approved, mark the task completed
        if (treatment.getSurgicalCenterTimeSlot() != null) {
            var slotId = treatment.getSurgicalCenterTimeSlot().getId();
            List<Treatment> forSlot = treatmentRepository.findByTimeSlotId(slotId);
            boolean allApproved = forSlot.stream().allMatch(t -> t.getApprovalDate() != null);
            if (allApproved) {
                // Find task by time slot ID with institution filtering
                Long institutionId = InstitutionContext.getInstitutionId();
                if (institutionId != null) {
                    // Use institution-aware query
                    Task task = taskRepository.findAllByInstitutionId(institutionId, 
                            org.springframework.data.domain.Pageable.unpaged())
                            .getContent().stream()
                            .filter(tsk -> tsk.getTimeSlot() != null && tsk.getTimeSlot().getId().equals(slotId))
                            .findFirst().orElse(null);
                    if (task != null && !task.isCompleted()) {
                        task.setCompleted(true);
                        task.setCompletedAt(clock.instant().atZone(clock.getZone()).toLocalDateTime());
                        task.setCompletedByUserId(actorUserId);
                        task.setCompletedByUserName(actorUserName);
                        taskRepository.save(task);
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Task> list(Pageable pageable) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            // No institution context - return empty list to enforce data isolation
            org.slf4j.LoggerFactory.getLogger(TaskService.class)
                    .warn("TaskService.list() called without institution context - returning empty list");
            return List.of();
        }
        List<Task> tasks = taskRepository.findAllByInstitutionId(institutionId, pageable).toList();
        org.slf4j.LoggerFactory.getLogger(TaskService.class)
                .debug("TaskService.list() found {} tasks for institution ID: {}", tasks.size(), institutionId);
        return tasks;
    }

    @Transactional(readOnly = true)
    public List<Task> listByCompleted(Boolean completed, Pageable pageable) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            // No institution context - return empty list to enforce data isolation
            org.slf4j.LoggerFactory.getLogger(TaskService.class)
                    .warn("TaskService.listByCompleted() called without institution context - returning empty list");
            return List.of();
        }
        
        List<Task> tasks;
        if (completed == null) {
            tasks = taskRepository.findAllByInstitutionId(institutionId, pageable).toList();
        } else {
            tasks = taskRepository.findAllByInstitutionIdAndCompleted(institutionId, completed, pageable).toList();
        }
        org.slf4j.LoggerFactory.getLogger(TaskService.class)
                .debug("TaskService.listByCompleted(completed={}) found {} tasks for institution ID: {}", 
                        completed, tasks.size(), institutionId);
        return tasks;
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public void updateTreatmentAdditionalInfo(Long treatmentId, String additionalInfo) {
        Objects.requireNonNull(treatmentId);
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + treatmentId));
        
        // Validate institution context: ensure treatment belongs to current institution
        if (treatment.getTreatmentPlan() == null || treatment.getTreatmentPlan().getInstitution() == null) {
            throw new IllegalStateException("Treatment " + treatmentId + " has no treatment plan or institution");
        }
        Long treatmentInstitutionId = treatment.getTreatmentPlan().getInstitution().getId();
        institutionAccessValidator.validateInstitutionAccess(treatmentInstitutionId, "Treatment", treatmentId);
        
        treatment.setAdditionalInfo(additionalInfo);
        treatmentRepository.save(treatment);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MEDICAL_STAFF', 'DOCTOR', 'OWNER', 'ADMIN')")
    public void updateTreatmentPatientAppeared(Long treatmentId, Boolean patientAppeared) {
        Objects.requireNonNull(treatmentId);
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + treatmentId));
        
        // Validate institution context: ensure treatment belongs to current institution
        if (treatment.getTreatmentPlan() == null || treatment.getTreatmentPlan().getInstitution() == null) {
            throw new IllegalStateException("Treatment " + treatmentId + " has no treatment plan or institution");
        }
        Long treatmentInstitutionId = treatment.getTreatmentPlan().getInstitution().getId();
        institutionAccessValidator.validateInstitutionAccess(treatmentInstitutionId, "Treatment", treatmentId);
        
        treatment.setPatientAppeared(patientAppeared);
        // Update treatmentStatus for backward compatibility
        if (patientAppeared != null) {
            if (patientAppeared) {
                if (treatment.getTreatmentStatus() == null) {
                    treatment.setTreatmentStatus(de.bbajor.pvs.intravitreal.treatment.model.TreatmentStatus.PATIENT_APPEARED_SUCCESSFUL);
                }
            } else {
                if (treatment.getTreatmentStatus() == null) {
                    treatment.setTreatmentStatus(de.bbajor.pvs.intravitreal.treatment.model.TreatmentStatus.PATIENT_NO_SHOW);
                }
            }
        }
        treatmentRepository.save(treatment);
    }
    
    @Transactional
    @PreAuthorize("hasAnyRole('MEDICAL_STAFF', 'DOCTOR', 'OWNER', 'ADMIN')")
    public void updateTreatmentStatus(Long treatmentId, de.bbajor.pvs.intravitreal.treatment.model.TreatmentStatus treatmentStatus) {
        Objects.requireNonNull(treatmentId);
        Objects.requireNonNull(treatmentStatus);
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + treatmentId));
        
        // Validate institution context: ensure treatment belongs to current institution
        if (treatment.getTreatmentPlan() == null || treatment.getTreatmentPlan().getInstitution() == null) {
            throw new IllegalStateException("Treatment " + treatmentId + " has no treatment plan or institution");
        }
        Long treatmentInstitutionId = treatment.getTreatmentPlan().getInstitution().getId();
        institutionAccessValidator.validateInstitutionAccess(treatmentInstitutionId, "Treatment", treatmentId);
        
        treatment.setTreatmentStatus(treatmentStatus);
        // Update patientAppeared for backward compatibility
        treatment.setPatientAppeared(treatmentStatus == de.bbajor.pvs.intravitreal.treatment.model.TreatmentStatus.PATIENT_APPEARED_SUCCESSFUL
                || treatmentStatus == de.bbajor.pvs.intravitreal.treatment.model.TreatmentStatus.PATIENT_APPEARED_NEEDS_RETREATMENT
                || treatmentStatus == de.bbajor.pvs.intravitreal.treatment.model.TreatmentStatus.PATIENT_APPEARED_NO_TREATMENT);
        treatmentRepository.save(treatment);
    }

    @Transactional(readOnly = true)
    public boolean canBookFollowUpTreatment(Long treatmentId) {
        Objects.requireNonNull(treatmentId);
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + treatmentId));
        
        // Validate institution context
        if (treatment.getTreatmentPlan() == null || treatment.getTreatmentPlan().getInstitution() == null) {
            throw new IllegalStateException("Treatment " + treatmentId + " has no treatment plan or institution");
        }
        Long treatmentInstitutionId = treatment.getTreatmentPlan().getInstitution().getId();
        institutionAccessValidator.validateInstitutionAccess(treatmentInstitutionId, "Treatment", treatmentId);
        
        // Check if treatment's time slot has an open task
        if (treatment.getSurgicalCenterTimeSlot() == null) {
            return false;
        }
        
        Long timeSlotId = treatment.getSurgicalCenterTimeSlot().getId();
        List<Task> tasks = taskRepository.findAllByInstitutionId(treatmentInstitutionId, 
                org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(task -> task.getTimeSlot() != null && task.getTimeSlot().getId().equals(timeSlotId))
                .filter(task -> !task.isCompleted())
                .toList();
        
        if (tasks.isEmpty()) {
            return false; // Task is completed or doesn't exist
        }
        
        // Check if there's already a follow-up treatment for this treatment
        TreatmentPlan treatmentPlan = treatment.getTreatmentPlan();
        if (treatmentPlan == null || treatmentPlan.getId() == null) {
            return false;
        }
        
        // Check if there's already a follow-up treatment (treatment with same plan and side of eye, 
        // but with a later date than the current treatment)
        LocalDate currentTreatmentDate = treatment.getDate();
        if (currentTreatmentDate == null) {
            return false;
        }
        
        List<Treatment> futureTreatments = treatmentRepository
                .findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(treatmentPlan.getId())
                .stream()
                .filter(t -> t.getSideOfEye() == treatment.getSideOfEye())
                .filter(t -> t.getDate() != null && t.getDate().isAfter(currentTreatmentDate))
                .toList();
        
        return futureTreatments.isEmpty(); // Can book if no future treatment exists
    }

    @Transactional(readOnly = true)
    @Nullable
    public Treatment findExistingFollowUpTreatment(Long treatmentId) {
        Objects.requireNonNull(treatmentId);
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + treatmentId));
        
        // Validate institution context
        if (treatment.getTreatmentPlan() == null || treatment.getTreatmentPlan().getInstitution() == null) {
            throw new IllegalStateException("Treatment " + treatmentId + " has no treatment plan or institution");
        }
        Long treatmentInstitutionId = treatment.getTreatmentPlan().getInstitution().getId();
        institutionAccessValidator.validateInstitutionAccess(treatmentInstitutionId, "Treatment", treatmentId);
        
        TreatmentPlan treatmentPlan = treatment.getTreatmentPlan();
        if (treatmentPlan == null || treatmentPlan.getId() == null) {
            return null;
        }
        
        LocalDate currentTreatmentDate = treatment.getDate();
        if (currentTreatmentDate == null) {
            return null;
        }
        
        // Find the earliest future treatment for the same plan and side of eye
        return treatmentRepository
                .findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(treatmentPlan.getId())
                .stream()
                .filter(t -> t.getSideOfEye() == treatment.getSideOfEye())
                .filter(t -> t.getDate() != null && t.getDate().isAfter(currentTreatmentDate))
                .min((a, b) -> a.getDate().compareTo(b.getDate()))
                .orElse(null);
    }

}
