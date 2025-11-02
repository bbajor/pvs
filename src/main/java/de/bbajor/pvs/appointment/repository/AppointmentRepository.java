package de.bbajor.pvs.appointment.repository;

import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.appointment.model.Appointment;
import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.tenant.repository.TenantAwareRepository;

/**
 * Repository for Appointment entities.
 * Tenant-aware to ensure data isolation.
 */
@Repository
public interface AppointmentRepository extends TenantAwareRepository<Appointment, Long> {

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

    /**
     * Find appointments for a tenant within a date range.
     */
    @Query("SELECT a FROM Appointment a WHERE a.tenant.id = :tenantId " +
           "AND a.startTime >= :start AND a.startTime <= :end ORDER BY a.startTime ASC")
    List<Appointment> findByTenantIdAndDateRange(
        @Param("tenantId") Long tenantId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * Find appointment by ID and tenant (tenant-safe access).
     */
    @Query("SELECT a FROM Appointment a WHERE a.id = :id AND a.tenant.id = :tenantId")
    Optional<Appointment> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * Find all appointments for a scheduler belonging to a tenant.
     */
    @Query("SELECT a FROM Appointment a WHERE a.scheduler = :scheduler AND a.tenant.id = :tenantId")
    List<Appointment> findBySchedulerAndTenantId(
        @Param("scheduler") AppointmentScheduler scheduler,
        @Param("tenantId") Long tenantId
    );
}
