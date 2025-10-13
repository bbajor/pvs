package de.bbajor.pvs.taskmanagement.util;

import java.util.logging.Logger;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.ui.component.TimelineView;
import de.bbajor.pvs.taskmanagement.service.TaskService;

@Component
public class TimeSlotTaskScheduler {

    private static final Logger LOG = Logger.getLogger(TimelineView.class.getName());

    private final TaskService taskService;

    public TimeSlotTaskScheduler(TaskService taskService) {
        this.taskService = taskService;
    }

    // 1. Läuft täglich um Mitternacht
    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Berlin")
    public void runDaily() {
        LOG.info("Täglicher Task-Job gestartet");
        taskService.createDailyTask();
    }

    // 2. Läuft beim Start der Anwendung einmal
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        LOG.info("Anwendung gestartet — Initial-Task wird angelegt");
        taskService.createDailyTask();
    }
}
