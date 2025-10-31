package de.bbajor.pvs.appointment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.practice.model.Practice;

/**
 * Repository for AppointmentScheduler entities.
 */
@Repository
public interface AppointmentSchedulerRepository extends JpaRepository<AppointmentScheduler, Long> {

    /**
     * Find all schedulers for a specific practice.
     */
    List<AppointmentScheduler> findByPractice(Practice practice);

    /**
     * Find all active schedulers for a practice.
     */
    List<AppointmentScheduler> findByPracticeAndActiveTrue(Practice practice);

    /**
     * Find all schedulers by practice ID.
     */
    List<AppointmentScheduler> findByPracticeId(Long practiceId);

    /**
     * Find active schedulers by practice ID.
     */
    List<AppointmentScheduler> findByPracticeIdAndActiveTrue(Long practiceId);
}
