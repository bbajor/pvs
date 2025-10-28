package de.bbajor.pvs.security.dev;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link UserDetailsService} for development environments.
 * <p>
 * This class provides a hybrid implementation that supports both:
 * <ul>
 * <li>Predefined in-memory {@link DevUser} instances (from {@link SampleUsers})</li>
 * <li>User accounts stored in the database ({@link UserAccount}) via the Benutzerverwaltung</li>
 * </ul>
 * </p>
 * <p>
 * This implementation is specifically designed for development and testing purposes. It allows the application to
 * function with predefined test users while also supporting user management through the UI.
 * </p>
 * <p>
 * The lookup order is:
 * <ol>
 * <li>First, check the in-memory DevUser collection</li>
 * <li>If not found, query the database for UserAccount entities</li>
 * <li>Throw UsernameNotFoundException if neither found</li>
 * </ol>
 * </p>
 *
 * @see DevUser The development user class stored in this service
 * @see UserAccount The database-stored user account entity
 * @see UserDetailsService Spring Security's interface for loading user authentication details
 */
final class DevUserDetailsService implements UserDetailsService {

    private final Map<String, UserDetails> userByUsername;
    private final UserAccountRepository userAccountRepository;

    /**
     * Creates a new service with the specified development users and user account repository.
     *
     * @param users
     *            the development users to include in this service
     * @param userAccountRepository
     *            repository for accessing user accounts from the database
     */
    DevUserDetailsService(Collection<DevUser> users, UserAccountRepository userAccountRepository) {
        this.userByUsername = new HashMap<>();
        users.forEach(user -> this.userByUsername.put(user.getAppUser().getPreferredUsername(), user));
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // First, try to find in-memory DevUser
        UserDetails devUser = userByUsername.get(username);
        if (devUser != null) {
            return devUser;
        }

        // If not found, try to find in database
        Optional<UserAccount> userAccount = userAccountRepository.findByUsername(username);
        if (userAccount.isPresent()) {
            return new UserAccountUserDetailsAdapter(userAccount.get());
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}
