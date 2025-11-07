package de.bbajor.pvs.institution.repository;

import de.bbajor.pvs.institution.model.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Institution entities.
 * <p>
 * Institutions are stored in the central registry database.
 * This repository is used for institution lookup during login
 * and institution management.
 * </p>
 */
@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    /**
     * Find institution by institution code (used for login).
     * 
     * @param institutionCode the institution code
     * @return Optional containing the institution if found
     */
    Optional<Institution> findByInstitutionCode(String institutionCode);

    /**
     * Find institution by database name.
     * 
     * @param databaseName the database name
     * @return Optional containing the institution if found
     */
    Optional<Institution> findByDatabaseName(String databaseName);

    /**
     * Find institution by container name.
     * 
     * @param containerName the container name
     * @return Optional containing the institution if found
     */
    Optional<Institution> findByContainerName(String containerName);

    /**
     * Check if institution code exists.
     * 
     * @param institutionCode the institution code
     * @return true if institution with this code exists
     */
    boolean existsByInstitutionCode(String institutionCode);
}

