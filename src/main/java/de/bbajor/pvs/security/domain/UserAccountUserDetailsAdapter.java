package de.bbajor.pvs.security.domain;

import de.bbajor.pvs.security.AppUserInfo;
import de.bbajor.pvs.security.AppUserPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter that converts a {@link UserAccount} entity to Spring Security's {@link UserDetails}
 * and the application's {@link AppUserPrincipal} interface.
 * <p>
 * This adapter allows UserAccount entities stored in the database to be used for authentication
 * while maintaining compatibility with the application's user information model.
 */
public class UserAccountUserDetailsAdapter implements AppUserPrincipal, UserDetails {

    private final UserAccount userAccount;
    private final AppUserInfo appUserInfo;
    private final Collection<GrantedAuthority> authorities;

    public UserAccountUserDetailsAdapter(UserAccount userAccount) {
        this.userAccount = userAccount;
        this.appUserInfo = createAppUserInfo(userAccount);
        this.authorities = createAuthorities(userAccount.getRoles());
    }

    private AppUserInfo createAppUserInfo(UserAccount userAccount) {
        String userId = userAccount.getUserId();
        if (userId == null || userId.isEmpty()) {
            // Fallback: use username as userId if not set
            userId = userAccount.getUsername();
        }

        String fullName = userAccount.getFullName();
        if (fullName == null || fullName.isEmpty()) {
            // Fallback: use username as fullName if not set
            fullName = userAccount.getUsername();
        }

        return new UserAccountAppUserInfo(
                UserId.of(userId),
                userAccount.getUsername(),
                fullName,
                userAccount.getEmail(),
                ZoneId.systemDefault(),
                Locale.getDefault()
        );
    }

    private Collection<GrantedAuthority> createAuthorities(Set<String> roles) {
        Set<String> normalizedRoles = new LinkedHashSet<>(roles);
        // Backward compatibility: old role should behave like ADMIN.
        if (normalizedRoles.contains("INSTITUTION_ADMIN")) {
            normalizedRoles.add("ADMIN");
        }

        return normalizedRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    @Override
    public AppUserInfo getAppUser() {
        return appUserInfo;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return userAccount.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return userAccount.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return userAccount.isEnabled();
    }

    /**
     * Simple implementation of AppUserInfo for UserAccount entities.
     */
    private static record UserAccountAppUserInfo(
            UserId userId,
            String preferredUsername,
            String fullName,
            String email,
            ZoneId zoneId,
            Locale locale
    ) implements AppUserInfo {

        @Override
        public UserId getUserId() {
            return userId;
        }

        @Override
        public String getPreferredUsername() {
            return preferredUsername;
        }

        @Override
        public String getFullName() {
            return fullName;
        }

        @Override
        public String getEmail() {
            return email;
        }

        @Override
        public java.time.ZoneId getZoneId() {
            return zoneId;
        }

        @Override
        public Locale getLocale() {
            return locale;
        }
    }
}

