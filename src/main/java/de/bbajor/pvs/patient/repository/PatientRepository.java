package de.bbajor.pvs.patient.repository;

import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.institution.repository.InstitutionAwareRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends InstitutionAwareRepository<Patient, Integer>, JpaSpecificationExecutor<Patient> {

    /**
     * Find patient by name and birth date within institution.
     * Ensures uniqueness per institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Patient → Location → Institution (primary path).
     * </p>
     */
    @Query("SELECT p FROM Patient p WHERE " +
           "p.location IS NOT NULL AND p.location.institution.id = :institutionId " +
           "AND p.firstName = :firstName AND p.lastName = :lastName AND p.birth = :birth")
    Optional<Patient> findByInstitutionAndNameAndBirth(
            @Param("institutionId") Long institutionId,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("birth") LocalDate birth);

    /**
     * Find patient by insurance number within institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Patient → Location → Institution (primary path).
     * </p>
     */
    @Query("SELECT p FROM Patient p WHERE " +
           "p.location IS NOT NULL AND p.location.institution.id = :institutionId " +
           "AND p.insuranceNumber = :insuranceNumber")
    Optional<Patient> findByInstitutionAndInsuranceNumber(
            @Param("institutionId") Long institutionId,
            @Param("insuranceNumber") String insuranceNumber);

    /**
     * Search patients by name within institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Patient → Location → Institution (primary path).
     * </p>
     */
    @Query("SELECT p FROM Patient p WHERE " +
           "p.location IS NOT NULL AND p.location.institution.id = :institutionId " +
           "AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Patient> searchByNameInInstitution(
            @Param("institutionId") Long institutionId,
            @Param("searchTerm") String searchTerm);
    
    /**
     * Find all patients for an institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Patient → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Query("SELECT p FROM Patient p WHERE " +
           "p.location IS NOT NULL AND p.location.institution.id = :institutionId")
    List<Patient> findByInstitutionId(@Param("institutionId") Long institutionId);
    
    /**
     * Find patient by ID and institution (institution-safe access).
     * <p>
     * Data isolation: All filtering is done via institution.
     * Patient → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Query("SELECT p FROM Patient p WHERE p.id = :id AND " +
           "p.location IS NOT NULL AND p.location.institution.id = :institutionId")
    Optional<Patient> findByIdAndInstitutionId(@Param("id") Integer id, @Param("institutionId") Long institutionId);
    
    /**
     * Count patients for an institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Patient → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Query("SELECT COUNT(p) FROM Patient p WHERE " +
           "p.location IS NOT NULL AND p.location.institution.id = :institutionId")
    long countByInstitutionId(@Param("institutionId") Long institutionId);
    
    /**
     * Check if patient exists for institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Patient → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Patient p WHERE p.id = :id AND " +
           "p.location IS NOT NULL AND p.location.institution.id = :institutionId")
    boolean existsByIdAndInstitutionId(@Param("id") Integer id, @Param("institutionId") Long institutionId);
    
    /**
     * Delete all patients for an institution.
     * USE WITH CAUTION - for institution deletion/cleanup only.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Patient → Location → Institution (primary path).
     * </p>
     */
    @Override
    @Modifying
    @Query("DELETE FROM Patient p WHERE " +
           "p.location IS NOT NULL AND p.location.institution.id = :institutionId")
    void deleteByInstitutionId(@Param("institutionId") Long institutionId);
}
