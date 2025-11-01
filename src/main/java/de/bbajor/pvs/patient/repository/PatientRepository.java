package de.bbajor.pvs.patient.repository;

import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.tenant.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends TenantAwareRepository<Patient, Integer>, JpaSpecificationExecutor<Patient> {

    /**
     * Find patient by name and birth date within tenant.
     * Ensures uniqueness per tenant.
     */
    @Query("SELECT p FROM Patient p WHERE p.tenant.id = :tenantId " +
           "AND p.firstName = :firstName AND p.lastName = :lastName AND p.birth = :birth")
    Optional<Patient> findByTenantAndNameAndBirth(
            @Param("tenantId") Long tenantId,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("birth") LocalDate birth);

    /**
     * Find patient by insurance number within tenant.
     */
    @Query("SELECT p FROM Patient p WHERE p.tenant.id = :tenantId AND p.insuranceNumber = :insuranceNumber")
    Optional<Patient> findByTenantAndInsuranceNumber(
            @Param("tenantId") Long tenantId,
            @Param("insuranceNumber") String insuranceNumber);

    /**
     * Search patients by name within tenant.
     */
    @Query("SELECT p FROM Patient p WHERE p.tenant.id = :tenantId " +
           "AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Patient> searchByNameInTenant(
            @Param("tenantId") Long tenantId,
            @Param("searchTerm") String searchTerm);
}
