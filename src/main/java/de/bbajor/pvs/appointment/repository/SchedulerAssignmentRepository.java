package de.bbajor.pvs.appointment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.SchedulerAssignment;
import de.bbajor.pvs.security.domain.UserAccount;

/**
 * Repository for SchedulerAssignment entities.
 */
@Repository
public interface SchedulerAssignmentRepository extends JpaRepository<SchedulerAssignment, Long> {

    /**
     * Find all assignments for a scheduler.
     */
    List<SchedulerAssignment> findByScheduler(AppointmentScheduler scheduler);

    /**
     * Find all assignments for a specific user account.
     */
    List<SchedulerAssignment> findByUserAccount(UserAccount userAccount);

    /**
     * Find all assignments for a specific role.
     */
    List<SchedulerAssignment> findByRole(String role);

    /**
     * Find all schedulers assigned to a user account.
     */
    List<SchedulerAssignment> findByUserAccountId(Long userAccountId);

    /**
     * Check if a user has access to a scheduler.
     */
    boolean existsBySchedulerAndUserAccount(AppointmentScheduler scheduler, UserAccount userAccount);

    /**
     * Check if a role has access to a scheduler.
     */
    boolean existsBySchedulerAndRole(AppointmentScheduler scheduler, String role);
}
