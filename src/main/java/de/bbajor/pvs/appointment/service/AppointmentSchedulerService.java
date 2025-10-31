package de.bbajor.pvs.appointment.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.SchedulerAssignment;
import de.bbajor.pvs.appointment.repository.AppointmentSchedulerRepository;
import de.bbajor.pvs.appointment.repository.SchedulerAssignmentRepository;
import de.bbajor.pvs.practice.model.Practice;
import de.bbajor.pvs.security.domain.UserAccount;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing appointment schedulers.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentSchedulerService {

    private final AppointmentSchedulerRepository schedulerRepository;
    private final SchedulerAssignmentRepository assignmentRepository;

    /**
     * Find all schedulers for a practice.
     */
    public List<AppointmentScheduler> findByPractice(Practice practice) {
        return schedulerRepository.findByPractice(practice);
    }

    /**
     * Find all active schedulers for a practice.
     */
    public List<AppointmentScheduler> findActiveByPractice(Practice practice) {
        return schedulerRepository.findByPracticeAndActiveTrue(practice);
    }

    /**
     * Find scheduler by ID.
     */
    public Optional<AppointmentScheduler> findById(Long id) {
        return schedulerRepository.findById(id);
    }

    /**
     * Create or update a scheduler.
     */
    public AppointmentScheduler save(AppointmentScheduler scheduler) {
        return schedulerRepository.save(scheduler);
    }

    /**
     * Delete a scheduler.
     */
    public void delete(AppointmentScheduler scheduler) {
        schedulerRepository.delete(scheduler);
    }

    /**
     * Deactivate a scheduler instead of deleting it.
     */
    public void deactivate(AppointmentScheduler scheduler) {
        scheduler.setActive(false);
        schedulerRepository.save(scheduler);
    }

    /**
     * Assign a user to a scheduler.
     */
    public SchedulerAssignment assignUser(AppointmentScheduler scheduler, UserAccount userAccount) {
        SchedulerAssignment assignment = new SchedulerAssignment()
            .setScheduler(scheduler)
            .setUserAccount(userAccount);
        return assignmentRepository.save(assignment);
    }

    /**
     * Assign a role to a scheduler.
     */
    public SchedulerAssignment assignRole(AppointmentScheduler scheduler, String role) {
        SchedulerAssignment assignment = new SchedulerAssignment()
            .setScheduler(scheduler)
            .setRole(role);
        return assignmentRepository.save(assignment);
    }

    /**
     * Get all assignments for a scheduler.
     */
    public List<SchedulerAssignment> getAssignments(AppointmentScheduler scheduler) {
        return assignmentRepository.findByScheduler(scheduler);
    }

    /**
     * Get all schedulers assigned to a user.
     */
    public List<SchedulerAssignment> getSchedulersForUser(UserAccount userAccount) {
        return assignmentRepository.findByUserAccount(userAccount);
    }

    /**
     * Get all schedulers assigned to a role.
     */
    public List<SchedulerAssignment> getSchedulersForRole(String role) {
        return assignmentRepository.findByRole(role);
    }

    /**
     * Check if a user has access to a scheduler.
     */
    public boolean hasAccess(AppointmentScheduler scheduler, UserAccount userAccount) {
        // Check direct user assignment
        if (assignmentRepository.existsBySchedulerAndUserAccount(scheduler, userAccount)) {
            return true;
        }
        
        // Check role-based assignment
        for (String role : userAccount.getRoles()) {
            if (assignmentRepository.existsBySchedulerAndRole(scheduler, role)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Find all schedulers.
     */
    public List<AppointmentScheduler> findAll() {
        return schedulerRepository.findAll();
    }
}
