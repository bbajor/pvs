package de.bbajor.pvs.appointment.repository;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.OfficeHours;

/**
 * Repository for OfficeHours entities.
 */
@Repository
public interface OfficeHoursRepository extends JpaRepository<OfficeHours, Long> {

    /**
     * Find all office hours for a scheduler.
     */
    List<OfficeHours> findByScheduler(AppointmentScheduler scheduler);

    /**
     * Find active office hours for a scheduler.
     */
    List<OfficeHours> findBySchedulerAndActiveTrue(AppointmentScheduler scheduler);

    /**
     * Find office hours for a scheduler on a specific day of week.
     */
    List<OfficeHours> findBySchedulerAndDayOfWeek(AppointmentScheduler scheduler, DayOfWeek dayOfWeek);

    /**
     * Find active office hours for a scheduler on a specific day of week.
     */
    List<OfficeHours> findBySchedulerAndDayOfWeekAndActiveTrue(
        AppointmentScheduler scheduler, 
        DayOfWeek dayOfWeek
    );
}
