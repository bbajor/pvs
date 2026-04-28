package de.bbajor.pvs.location.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.institution.repository.InstitutionAwareRepository;
import de.bbajor.pvs.location.model.Location;

/**
 * Repository for Location entities.
 * <p>
 * Locations are stored in the institution's own database
 * (not in the registry database).
 * </p>
 */
@Repository
public interface LocationRepository extends InstitutionAwareRepository<Location, Long> {

    /**
     * Find all locations for an institution.
     * Note: This uses institution_id as a reference (no FK constraint in multi-DB).
     * 
     * @param institutionId the institution ID
     * @return list of locations for the institution
     */
    @Query("SELECT l FROM Location l WHERE l.institution.id = :institutionId")
    List<Location> findByInstitutionId(@Param("institutionId") Long institutionId);

    /**
     * Find location by institution and location name.
     * 
     * @param institutionId the institution ID
     * @param locationName the location name
     * @return Optional containing the location if found
     */
    @Query("SELECT l FROM Location l WHERE l.institution.id = :institutionId AND l.locationName = :locationName")
    Optional<Location> findByInstitutionIdAndLocationName(
            @Param("institutionId") Long institutionId,
            @Param("locationName") String locationName);

    /**
     * Find all locations for an institution filtered by active status.
     * 
     * @param institutionId the institution ID
     * @param active whether to return only active locations
     * @return list of locations matching the criteria
     */
    @Query("SELECT l FROM Location l WHERE l.institution.id = :institutionId AND l.active = :active")
    List<Location> findByInstitutionIdAndActive(
            @Param("institutionId") Long institutionId,
            @Param("active") boolean active);
    
    /**
     * Find all locations filtered by active status.
     * 
     * @param active whether to return only active locations
     * @return list of locations matching the criteria
     */
    @Query("SELECT l FROM Location l WHERE l.active = :active")
    List<Location> findByActive(@Param("active") boolean active);

    /**
    * Override from InstitutionAwareRepository.
     * Locations are filtered by institution_id.
     */
    @Override
    @Query("SELECT l FROM Location l WHERE l.id = :id AND l.institution.id = :institutionId")
    Optional<Location> findByIdAndInstitutionId(@Param("id") Long id, @Param("institutionId") Long institutionId);

    @Override
    @Query("SELECT COUNT(l) FROM Location l WHERE l.institution.id = :institutionId")
    long countByInstitutionId(@Param("institutionId") Long institutionId);

    @Override
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Location l WHERE l.id = :id AND l.institution.id = :institutionId")
    boolean existsByIdAndInstitutionId(@Param("id") Long id, @Param("institutionId") Long institutionId);

    @Override
    @Modifying
    @Query("DELETE FROM Location l WHERE l.institution.id = :institutionId")
    void deleteByInstitutionId(@Param("institutionId") Long institutionId);

    /**
     * Find the main location for an institution.
     */
    @Query("SELECT l FROM Location l WHERE l.institution.id = :institutionId AND l.mainLocation = true")
    List<Location> findMainLocationsByInstitutionId(@Param("institutionId") Long institutionId);
}

