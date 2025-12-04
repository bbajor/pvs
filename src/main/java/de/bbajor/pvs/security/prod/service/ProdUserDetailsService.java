package de.bbajor.pvs.security.prod.service;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;

/**
 * Production UserDetailsService that only uses database-stored users.
 * 
 * No development test users or convenience features.
 */
public class ProdUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(ProdUserDetailsService.class);
    private final UserAccountRepository userAccountRepository;

    public ProdUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user: {} (can be username or email)", username);
        
        // Support both username and email for login
        UserAccount account = userAccountRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        if (!account.isEnabled()) {
            log.warn("User account is disabled: {}", username);
            throw new UsernameNotFoundException("User account is disabled: " + username);
        }

        Set<GrantedAuthority> authorities = account.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(java.util.stream.Collectors.toSet());

        log.debug("Loaded user {} with roles: {}", username, authorities);

        return User.builder()
                .username(account.getUsername())
                .password(account.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!account.isEnabled())
                .build();
    }
}

