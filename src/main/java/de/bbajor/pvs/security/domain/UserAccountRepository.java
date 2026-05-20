package de.bbajor.pvs.security.domain;

import java.util.List;
import java.util.Optional;

import de.bbajor.pvs.location.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsernameAndInstitution_Id(String username, Long institutionId);

    @Query("""
            SELECT ua FROM UserAccount ua
            WHERE ua.username = :username
            ORDER BY CASE WHEN ua.institution IS NULL THEN 1 ELSE 0 END, ua.id
            """)
    List<UserAccount> findAllByUsernameOrderByInstitutionFirst(@Param("username") String username);

    default Optional<UserAccount> findByUsername(String username) {
        List<UserAccount> list = findAllByUsernameOrderByInstitutionFirst(username);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("""
            SELECT ua FROM UserAccount ua
            WHERE ua.userId = :userId
            ORDER BY CASE WHEN ua.institution IS NULL THEN 1 ELSE 0 END, ua.id
            """)
    List<UserAccount> findAllByUserIdOrderByInstitutionFirst(@Param("userId") String userId);

    default Optional<UserAccount> findByUserId(String userId) {
        List<UserAccount> list = findAllByUserIdOrderByInstitutionFirst(userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    Optional<UserAccount> findByEmail(String email);

    /**
     * Finds a user by username or email address.
     * Bei mehreren Treffern (z. B. gleicher Name mit/ohne Mandant)
     * wird der Account mit Institution bevorzugt.
     */
    @Query("""
            SELECT ua FROM UserAccount ua
            WHERE ua.username = :identifier OR ua.email = :identifier
            ORDER BY CASE WHEN ua.institution IS NULL THEN 1 ELSE 0 END, ua.id
            """)
    List<UserAccount> findAllByUsernameOrEmailOrderByInstitutionFirst(@Param("identifier") String identifier);

    default Optional<UserAccount> findByUsernameOrEmail(String identifier) {
        List<UserAccount> list = findAllByUsernameOrEmailOrderByInstitutionFirst(identifier);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
    
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
