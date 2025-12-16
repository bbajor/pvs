package de.bbajor.pvs.taskmanagement.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.taskmanagement.service.TaskService;

@Component
public class TimeSlotTaskScheduler {

    private static final Logger LOG = Logger.getLogger(TimeSlotTaskScheduler.class.getName());
    
    // Flag, um sicherzustellen, dass der Startup-Job nur einmal ausgeführt wird
    // (Spring DevTools kann ApplicationReadyEvent mehrfach auslösen)
    private static final AtomicBoolean startupJobExecuted = new AtomicBoolean(false);

    private final TaskService taskService;
    private final InstitutionRepository institutionRepository;

    public TimeSlotTaskScheduler(TaskService taskService, InstitutionRepository institutionRepository) {
        this.taskService = taskService;
        this.institutionRepository = institutionRepository;
    }

    // 1. Läuft täglich um Mitternacht
    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Berlin")
    public void runDaily() {
        LOG.info("Täglicher Task-Job gestartet - prüfe alle aktiven Institutionen");
        createTasksForAllActiveInstitutions();
    }

    // 2. Läuft beim Start der Anwendung einmal
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        // Stelle sicher, dass der Startup-Job nur einmal ausgeführt wird
        // (Spring DevTools kann ApplicationReadyEvent mehrfach auslösen)
        if (startupJobExecuted.compareAndSet(false, true)) {
            LOG.info("Anwendung gestartet — Initial-Tasks werden für alle aktiven Institutionen angelegt");
            createTasksForAllActiveInstitutions();
        } else {
            LOG.fine("Startup-Job wurde bereits ausgeführt - überspringe erneute Ausführung");
        }
    }

    /**
     * Creates daily tasks for all active institutions.
     * Iterates over all active institutions and creates tasks for each one separately.
     */
    private void createTasksForAllActiveInstitutions() {
        List<Institution> activeInstitutions = institutionRepository.findAll().stream()
                .filter(Institution::isActive)
                .toList();
        
        LOG.info("Gefundene aktive Institutionen: " + activeInstitutions.size());
        
        for (Institution institution : activeInstitutions) {
            try {
                LOG.info("Erstelle Tasks für Institution: " + institution.getInstitutionCode() + " (ID: " + institution.getId() + ")");
                taskService.createDailyTaskIfAnyForInstitution(institution.getId());
            } catch (Exception e) {
                LOG.severe("Fehler beim Erstellen von Tasks für Institution " + institution.getInstitutionCode() + ": " + e.getMessage());
                // Continue with next institution even if one fails
            }
        }
        
        LOG.info("Task-Job abgeschlossen");
    }
}
