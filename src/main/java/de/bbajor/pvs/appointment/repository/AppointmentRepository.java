package de.bbajor.pvs.appointment.repository;

import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.appointment.model.Appointment;
import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.institution.repository.InstitutionAwareRepository;

/**
 * Repository for Appointment entities.
 * Tenant-aware to ensure data isolation.
 * <p>
 * Uses Location for data isolation.
 * </p>
 */
@Repository
public interface AppointmentRepository extends InstitutionAwareRepository<Appointment, Long> {

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
     * Find appointments for a institution within a date range.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Appointment → Patient → Location → Institution (primary path).
     * </p>
     */
    @Query("SELECT a FROM Appointment a WHERE " +
       "a.patient.location IS NOT NULL AND a.patient.location.institution.id = :institutionId " +
           "AND a.startTime >= :start AND a.startTime <= :end ORDER BY a.startTime ASC")
    List<Appointment> findByInstitutionIdAndDateRange(
        @Param("institutionId") Long institutionId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );


    /**
     * Find all appointments for a scheduler belonging to an institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Appointment → Patient → Location → Institution (primary path).
     * </p>
     */
    @Query("SELECT a FROM Appointment a WHERE a.scheduler = :scheduler AND " +
           "a.patient.location IS NOT NULL AND a.patient.location.institution.id = :institutionId")
    List<Appointment> findBySchedulerAndInstitutionId(
        @Param("scheduler") AppointmentScheduler scheduler,
        @Param("institutionId") Long institutionId
    );
    
    /**
     * Find all appointments for an institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Appointment → Patient → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Query("SELECT a FROM Appointment a WHERE " +
           "a.patient.location IS NOT NULL AND a.patient.location.institution.id = :institutionId")
    List<Appointment> findByInstitutionId(@Param("institutionId") Long institutionId);
    
    /**
     * Find appointment by ID and institution (institution-safe access).
     * <p>
     * Data isolation: All filtering is done via institution.
     * Appointment → Patient → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Query("SELECT a FROM Appointment a WHERE a.id = :id AND " +
           "a.patient.location IS NOT NULL AND a.patient.location.institution.id = :institutionId")
    Optional<Appointment> findByIdAndInstitutionId(@Param("id") Long id, @Param("institutionId") Long institutionId);
    
    /**
     * Count appointments for an institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Appointment → Patient → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Query("SELECT COUNT(a) FROM Appointment a WHERE " +
           "a.patient.location IS NOT NULL AND a.patient.location.institution.id = :institutionId")
    long countByInstitutionId(@Param("institutionId") Long institutionId);
    
    /**
     * Check if appointment exists for institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Appointment → Patient → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a WHERE a.id = :id AND " +
           "a.patient.location IS NOT NULL AND a.patient.location.institution.id = :institutionId")
    boolean existsByIdAndInstitutionId(@Param("id") Long id, @Param("institutionId") Long institutionId);
    
    /**
     * Delete all appointments for an institution.
     * USE WITH CAUTION - for institution deletion/cleanup only.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Appointment → Patient → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Modifying
    @Query("DELETE FROM Appointment a WHERE " +
           "a.patient.location IS NOT NULL AND a.patient.location.institution.id = :institutionId")
    void deleteByInstitutionId(@Param("institutionId") Long institutionId);
}
