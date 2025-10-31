package de.bbajor.pvs.appointment.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.appointment.model.Appointment;
import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.patient.model.Patient;

/**
 * Repository for Appointment entities.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Find all appointments for a scheduler.
     */
    List<Appointment> findByScheduler(AppointmentScheduler scheduler);

    /**
     * Find all appointments for a scheduler within a date range.
     */
    List<Appointment> findBySchedulerAndStartTimeBetween(
        AppointmentScheduler scheduler, 
        LocalDateTime start, 
        LocalDateTime end
    );

    /**
     * Find all appointments for a patient.
     */
    List<Appointment> findByPatient(Patient patient);

    /**
     * Find all appointments for a patient within a date range.
     */
    List<Appointment> findByPatientAndStartTimeBetween(
        Patient patient,
        LocalDateTime start,
        LocalDateTime end
    );

    /**
     * Find appointments that overlap with a given time range.
     */
    @Query("SELECT a FROM Appointment a WHERE a.scheduler = :scheduler " +
           "AND ((a.startTime <= :endTime AND a.endTime >= :startTime))")
    List<Appointment> findOverlappingAppointments(
        @Param("scheduler") AppointmentScheduler scheduler,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find the next available appointment slot for a scheduler.
     */
    @Query("SELECT a FROM Appointment a WHERE a.scheduler = :scheduler " +
           "AND a.startTime >= :fromTime ORDER BY a.startTime ASC")
    List<Appointment> findUpcomingAppointments(
        @Param("scheduler") AppointmentScheduler scheduler,
        @Param("fromTime") LocalDateTime fromTime
    );
}
