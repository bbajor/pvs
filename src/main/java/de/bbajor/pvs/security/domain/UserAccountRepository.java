package de.bbajor.pvs.security.domain;

import java.util.List;
import java.util.Optional;

import de.bbajor.pvs.location.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    
    Optional<UserAccount> findByEmail(String email);
    
    /**
     * Finds a user by username or email address.
     * This allows users to login with either their username or email.
     */
    @Query("SELECT ua FROM UserAccount ua WHERE ua.username = :identifier OR ua.email = :identifier")
    Optional<UserAccount> findByUsernameOrEmail(@Param("identifier") String identifier);
    
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

    /**
     * Count users that reference a location as preferredLocation.
     */
    long countByPreferredLocation(Location preferredLocation);
}
