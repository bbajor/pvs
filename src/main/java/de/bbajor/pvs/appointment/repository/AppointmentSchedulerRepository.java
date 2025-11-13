package de.bbajor.pvs.appointment.repository;

import java.util.List;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.institution.repository.InstitutionAwareRepository;

/**
 * Repository for AppointmentScheduler entities.
 * Institution-aware to ensure data isolation.
 * <p>
 * Data isolation: All filtering is done via institution.
 * AppointmentScheduler → Location → Institution (primary path).
 * </p>
 */
@Repository
public interface AppointmentSchedulerRepository extends InstitutionAwareRepository<AppointmentScheduler, Long> {

    /**
     * Find all schedulers for a specific location (new model).
     */
    List<AppointmentScheduler> findByLocation(Location location);

    /**
     * Find all active schedulers for a location (new model).
     */
    List<AppointmentScheduler> findByLocationAndActiveTrue(Location location);

    /**
     * Find all schedulers by location ID (new model).
     */
    List<AppointmentScheduler> findByLocationId(Long locationId);

    /**
     * Find active schedulers by location ID (new model).
     */
    List<AppointmentScheduler> findByLocationIdAndActiveTrue(Long locationId);

    /**
     * Find all schedulers for a institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * AppointmentScheduler → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Query("SELECT s FROM AppointmentScheduler s WHERE " +
           "s.location IS NOT NULL AND s.location.institution.id = :institutionId")
    List<AppointmentScheduler> findByInstitutionId(@Param("institutionId") Long institutionId);
    
    /**
     * Find scheduler by ID and institution (institution-safe access).
     * <p>
     * Data isolation: All filtering is done via institution.
     * AppointmentScheduler → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Query("SELECT s FROM AppointmentScheduler s WHERE s.id = :id AND " +
           "s.location IS NOT NULL AND s.location.institution.id = :institutionId")
    Optional<AppointmentScheduler> findByIdAndInstitutionId(@Param("id") Long id, @Param("institutionId") Long institutionId);
    
    /**
     * Count schedulers for a institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * AppointmentScheduler → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Query("SELECT COUNT(s) FROM AppointmentScheduler s WHERE " +
           "s.location IS NOT NULL AND s.location.institution.id = :institutionId")
    long countByInstitutionId(@Param("institutionId") Long institutionId);
    
    /**
     * Check if scheduler exists for institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * AppointmentScheduler → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM AppointmentScheduler s WHERE s.id = :id AND " +
           "s.location IS NOT NULL AND s.location.institution.id = :institutionId")
    boolean existsByIdAndInstitutionId(@Param("id") Long id, @Param("institutionId") Long institutionId);
    
    /**
     * Delete all schedulers for an institution.
     * USE WITH CAUTION - for institution deletion/cleanup only.
     * <p>
     * Data isolation: All filtering is done via institution.
     * AppointmentScheduler → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Modifying
    @Query("DELETE FROM AppointmentScheduler s WHERE " +
           "s.location IS NOT NULL AND s.location.institution.id = :institutionId")
    void deleteByInstitutionId(@Param("institutionId") Long institutionId);

    /**
     * Find active schedulers for a institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * AppointmentScheduler → Location → Institution (primary path).
     * </p>
     */
    @Query("SELECT s FROM AppointmentScheduler s WHERE s.active = true AND " +
           "s.location IS NOT NULL AND s.location.institution.id = :institutionId")
    List<AppointmentScheduler> findActivByInstitutionId(@Param("institutionId") Long institutionId);
}
