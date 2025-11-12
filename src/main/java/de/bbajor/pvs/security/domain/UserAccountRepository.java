package de.bbajor.pvs.security.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    
    /**
     * Loads all UserAccounts with their preferredLocation eagerly fetched.
     * This prevents LazyInitializationException when accessing location data
     * outside of a Hibernate session (e.g., in Vaadin Grid rendering).
     */
    @Query("SELECT DISTINCT ua FROM UserAccount ua LEFT JOIN FETCH ua.preferredLocation")
    List<UserAccount> findAllWithPreferredLocation();
    
    /**
     * Loads UserAccounts for a specific institution with their preferredLocation eagerly fetched.
     */
    @Query("SELECT DISTINCT ua FROM UserAccount ua LEFT JOIN FETCH ua.preferredLocation WHERE ua.institution.id = :institutionId")
    List<UserAccount> findAllByInstitutionIdWithPreferredLocation(@Param("institutionId") Long institutionId);
}
