package de.bbajor.pvs.security.dev;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

/**
 * Implementation of {@link UserDetailsService} for development environments.
 * <p>
 * This class loads user details exclusively from the database ({@link UserAccount}).
 * All credentials come from the database, whether using H2 or PostgreSQL.
 * </p>
 * <p>
 * This implementation is designed for development environments and should not
 * be used in production. Test users should be created via the Benutzerverwaltung UI
 * or via TestDataInitializer.
 * </p>
 *
 * @see UserAccount The database-stored user account entity
 * @see UserDetailsService Spring Security's interface for loading user authentication details
 */
final class DevUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    /**
     * Creates a new service that loads users exclusively from the database.
     *
     * @param userAccountRepository
     *            repository for accessing user accounts from the database
     */
    DevUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Load user exclusively from database (support both username and email)
        Optional<UserAccount> userAccount = userAccountRepository.findByUsernameOrEmail(username);
        if (userAccount.isPresent()) {
            return new UserAccountUserDetailsAdapter(userAccount.get());
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}
