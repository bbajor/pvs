package de.bbajor.pvs.taskmanagement.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.base.util.DateAndTimeUtils;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentAuditLog;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentAuditLogRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;
import de.bbajor.pvs.taskmanagement.domain.Task;
import de.bbajor.pvs.taskmanagement.domain.TaskRepository;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.access.prepost.PreAuthorize;

@Service
@PermitAll
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
    private Clock clock;

    @Transactional
    public void createTask(String description, @Nullable LocalDate dueDate, SurgicalCenterTimeSlot timeSlot) {
        if ("fail".equals(description)) {
            throw new RuntimeException("This is for testing the error handler");
        }
        var task = new Task();
        task.setDescription(description);
        task.setCreationDate(clock.instant());
        task.setDueDate(dueDate);
        task.setTimeSlot(timeSlot);
        taskRepository.saveAndFlush(task);
    }

    @Transactional
    public void createDailyTaskIfAny() {
        // 1. Find all timeslots containing not approved treatments until today
        List<Long> timeSlotIds = new ArrayList<>();
        List<Task> tasks = taskRepository.getTasksWhereExistsNotApprovedTreatment(LocalDate.now(clock));
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
                    + " im " + ts.getSurgicalCenter().getName() + " sind noch nicht überprüft worden.";
            // Setze das Datum eine Woche in die Zukunft
            LocalDate dueDate = ts.getDate().plusDays(7);
            createTask(description, dueDate, ts);
        });
    }

    @Transactional
    @PreAuthorize("hasRole('DOCTOR')")
    public void approveTreatment(Long treatmentId, String actorUserId, String actorUserName, boolean secondApproval) {
        Objects.requireNonNull(treatmentId);
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + treatmentId));
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
                Task task = taskRepository.findAll().stream()
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

    @Transactional(readOnly = true)
    public List<Task> list(Pageable pageable) {
        return taskRepository.findAllBy(pageable).toList();
    }

    @Transactional(readOnly = true)
    public List<Task> listByCompleted(Boolean completed, Pageable pageable) {
        if (completed == null) {
            return taskRepository.findAllBy(pageable).toList();
        }
        return taskRepository.findAllByCompleted(completed.booleanValue(), pageable).toList();
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

}
