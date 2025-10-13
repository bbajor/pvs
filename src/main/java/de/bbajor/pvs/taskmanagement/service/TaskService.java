package de.bbajor.pvs.taskmanagement.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;
import de.bbajor.pvs.taskmanagement.domain.Task;
import de.bbajor.pvs.taskmanagement.domain.TaskRepository;
import jakarta.annotation.security.PermitAll;

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
    public void createDailyTask() {
        // 1. Find all timeslots containing not approved treatments until today
        List<Long> timeSlotIds = new ArrayList<>();
        List<Task> tasks = taskRepository.getTasksWhereExistsNotApprovedTreatment(LocalDate.now(clock));
        tasks.forEach(e -> timeSlotIds.add(e.getId()));
        List<SurgicalCenterTimeSlot> newTimeSlotsforNewTasks = surgicalCenterService
                .getNewTimeSlotsContainingNotApprovedTreatments(timeSlotIds);
        newTimeSlotsforNewTasks.forEach(ts -> {
            String description = "Behandlung im OP-Zeitfenster am " + ts.getDate() + " um " + ts.getStartTime() + " im "
                    + ts.getSurgicalCenter().getName() + " ist noch nicht freigegeben.";
            createTask(description, ts.getDate().withDayOfMonth(ts.getDate().getDayOfMonth() + 7)
                    .with(LocalDateTime.now(clock).toLocalTime()), ts);
        });
    }

    @Transactional(readOnly = true)
    public List<Task> list(Pageable pageable) {
        return taskRepository.findAllBy(pageable).toList();
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

}
