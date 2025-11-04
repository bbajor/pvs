package de.bbajor.pvs.institution.security;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import lombok.RequiredArgsConstructor;

/**
 * Authentication provider that validates institution, username, and password.
 * <p>
 * Supports only Institution (new model).
 * </p>
 */
@Component
@RequiredArgsConstructor
public class InstitutionAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(InstitutionAuthenticationProvider.class);

    private final InstitutionRepository institutionRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.debug("TenantAuthenticationProvider.authenticate called with: {}", authentication.getClass().getSimpleName());

        if (!(authentication instanceof InstitutionAuthenticationToken)) {
            log.debug("Authentication is not InstitutionAuthenticationToken, returning null");
            return null;
        }

        InstitutionAuthenticationToken token = (InstitutionAuthenticationToken) authentication;
        String institutionCode = token.getInstitutionCode();
        String username = token.getName();
        String password = token.getCredentials().toString();

        log.debug("Attempting authentication for institution: {}, username: {}", institutionCode, username);

        // Find user first to check roles
        Optional<UserAccount> userOpt = userAccountRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("Login failed: User not found: {}", username);
            throw new BadCredentialsException("Invalid username or password");
        }

        UserAccount user = userOpt.get();
        log.debug("User found: {} (Institution: {})", username, user.getInstitution() != null ? user.getInstitution().getInstitutionCode() : "null");

        // Check if user has SUPER_ADMIN or INSTITUTION_ADMIN role (can login without institution)
        boolean hasSuperAdminRole = user.getRoles() != null && (user.getRoles().contains(AppRoles.SUPER_ADMIN) || user.getRoles().contains(AppRoles.INSTITUTION_ADMIN));
        boolean isEmptyInstitutionCode = institutionCode == null || institutionCode.trim().isEmpty();

        // SUPER_ADMIN and INSTITUTION_ADMIN can login without institution code
        if (isEmptyInstitutionCode && hasSuperAdminRole) {
            log.debug("User {} has {} role, allowing login without institution", username, 
                    user.getRoles().contains(AppRoles.SUPER_ADMIN) ? AppRoles.SUPER_ADMIN : AppRoles.INSTITUTION_ADMIN);
            
            // Validate password
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                log.warn("Login failed: Invalid password for user: {}", username);
                throw new BadCredentialsException("Invalid username or password");
            }

            if (!user.isEnabled()) {
                log.warn("Login failed: User not enabled: {}", username);
                throw new BadCredentialsException("User is not enabled");
            }

            log.info("Authentication successful for {} user: {} (no institution)", 
                    user.getRoles().contains(AppRoles.SUPER_ADMIN) ? AppRoles.SUPER_ADMIN : AppRoles.INSTITUTION_ADMIN, username);
            UserAccountUserDetailsAdapter principal = new UserAccountUserDetailsAdapter(user);

            return new InstitutionAuthenticationToken(
                    "", // Empty institution code for SUPER_ADMIN/INSTITUTION_ADMIN
                    null, // No institution ID
                    principal,
                    password,
                    principal.getAuthorities());
        }

        // For regular users, require institution code
        if (isEmptyInstitutionCode) {
            log.warn("Login failed: Institution code required for user: {}", username);
            throw new BadCredentialsException("Institution code is required");
        }

        // Try to find Institution
        Optional<Institution> institutionOpt = institutionRepository.findByInstitutionCode(institutionCode);
        Long institutionId = null;

        if (institutionOpt.isPresent()) {
            Institution institution = institutionOpt.get();
            if (!institution.isActive()) {
                log.warn("Login failed: Institution not active: {}", institutionCode);
                throw new BadCredentialsException("Institution is not active");
            }
            institutionId = institution.getId();
            log.debug("Institution found: {} (ID: {})", institutionCode, institutionId);
        } else {
            log.warn("Institution not found: {}", institutionCode);
            throw new BadCredentialsException("Institution not found");
        }
        
        // Check if user belongs to this institution
        // SUPER_ADMIN can access any institution
        if (user.getInstitution() != null && !institutionId.equals(user.getInstitution().getId())) {
            // Allow SUPER_ADMIN to login with any institution
            if (user.getRoles() != null && user.getRoles().contains(AppRoles.SUPER_ADMIN)) {
                log.debug("User {} has SUPER_ADMIN role, allowing login with institution {}", username, institutionCode);
            } else {
                log.warn("Login failed: User {} belongs to institution {} but login attempted with institution {}",
                        username, user.getInstitution().getInstitutionCode(), institutionCode);
                throw new BadCredentialsException("User does not belong to this institution");
            }
        }

        // Validate password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed: Invalid password for user: {}", username);
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.isEnabled()) {
            log.warn("Login failed: User not enabled: {}", username);
            throw new BadCredentialsException("User is not enabled");
        }

        log.info("Authentication successful for user: {} (institution: {})", username, institutionCode);

        UserAccountUserDetailsAdapter principal = new UserAccountUserDetailsAdapter(user);

        return new InstitutionAuthenticationToken(
                institutionCode,
                institutionId,
                principal,
                password,
                principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return InstitutionAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

