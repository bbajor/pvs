package de.bbajor.pvs.tenant.security;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.tenant.model.Tenant;
import de.bbajor.pvs.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Authentication provider that validates tenant, username, and password.
 */
@Component
@RequiredArgsConstructor
public class TenantAuthenticationProvider implements AuthenticationProvider {

    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof TenantAuthenticationToken)) {
            return null;
        }

        TenantAuthenticationToken token = (TenantAuthenticationToken) authentication;
        String tenantCode = token.getTenantCode();
        String username = token.getName();
        String password = token.getCredentials().toString();

        // Find tenant
        Optional<Tenant> tenantOpt = tenantRepository.findByTenantCode(tenantCode);
        if (tenantOpt.isEmpty()) {
            throw new BadCredentialsException("Invalid tenant code");
        }

        Tenant tenant = tenantOpt.get();
        if (!tenant.isActive()) {
            throw new BadCredentialsException("Tenant is not active");
        }

        // Find user
        Optional<UserAccount> userOpt = userAccountRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new BadCredentialsException("Invalid username or password");
        }

        UserAccount user = userOpt.get();

        // Check if user belongs to this tenant (or is a super admin)
        if (user.getTenant() != null && !user.getTenant().getId().equals(tenant.getId())) {
            throw new BadCredentialsException("User does not belong to this tenant");
        }

        // Validate password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("User is not enabled");
        }

        // Create authorities from roles
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        return new TenantAuthenticationToken(
                tenantCode,
                tenant.getId(),
                username,
                password,
                authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TenantAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
