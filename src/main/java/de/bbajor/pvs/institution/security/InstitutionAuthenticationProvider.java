package de.bbajor.pvs.institution.security;

import java.util.List;
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
        if (institutionCode != null && institutionCode.isBlank()) {
            institutionCode = null;
        }
        String identifier = token.getName(); // Can be username or email
        String password = token.getCredentials().toString();

        log.debug("Attempting authentication");

        // Find user by username or email
        Optional<UserAccount> userOpt = userAccountRepository.findByUsernameOrEmail(identifier);
        if (userOpt.isEmpty()) {
            log.warn("Login failed: user not found");
            throw new BadCredentialsException("Invalid username/email or password");
        }

        UserAccount user = userOpt.get();
        String username = user.getUsername(); // Use actual username from database
        log.debug("User found (institution assignment present: {})", user.getInstitution() != null);

        boolean hasSuperAdminRole = user.getRoles() != null && user.getRoles().contains(AppRoles.SUPER_ADMIN);
        if (!hasSuperAdminRole && user.getInstitution() == null) {
            log.warn("Login failed: user not assigned to an institution");
            throw new BadCredentialsException("Invalid username/email or password");
        }

        boolean isEmptyInstitutionCode = institutionCode == null;

        // Single-Tenant: genau eine Institution → Code optional
        if (isEmptyInstitutionCode && !hasSuperAdminRole) {
            List<Institution> institutions = institutionRepository.findAll();
            if (institutions.size() == 1) {
                Institution only = institutions.get(0);
                if (only.isActive()) {
                    institutionCode = only.getInstitutionCode();
                    isEmptyInstitutionCode = false;
                    log.debug("Single-tenant: resolved institution code to {}", institutionCode);
                }
            }
        }

        if (isEmptyInstitutionCode && hasSuperAdminRole) {
            log.debug("User {} has SUPER_ADMIN role, allowing login without institution", username);
            
            // Validate password
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                log.warn("Login failed: Invalid password for user: {}", username);
                throw new BadCredentialsException("Invalid username or password");
            }

            if (!user.isEnabled()) {
                log.warn("Login failed: User not enabled: {}", username);
                throw new BadCredentialsException("User is not enabled");
            }

            log.info("Authentication successful for SUPER_ADMIN user (no institution)");
            UserAccountUserDetailsAdapter principal = new UserAccountUserDetailsAdapter(user);

            return new InstitutionAuthenticationToken(
                    "", // Empty institution code for SUPER_ADMIN
                    null, // No institution ID
                    principal,
                    password,
                    principal.getAuthorities());
        }

        if (isEmptyInstitutionCode) {
            log.warn("Login failed: institution code required");
            throw new BadCredentialsException("Institution code is required");
        }

        // Try to find Institution
        Optional<Institution> institutionOpt = institutionRepository.findByInstitutionCode(institutionCode);
        Long institutionId = null;

        if (institutionOpt.isPresent()) {
            Institution institution = institutionOpt.get();
            if (!institution.isActive()) {
            log.warn("Login failed: institution not active");
                throw new BadCredentialsException("Institution is not active");
            }
            institutionId = institution.getId();
            log.debug("Institution resolved (ID present: {})", institutionId != null);
        } else {
            log.warn("Institution not found");
            throw new BadCredentialsException("Institution not found");
        }
        
        // Check if user belongs to this institution
        // SUPER_ADMIN can access any institution
        if (institutionId != null && user.getInstitution() != null && !institutionId.equals(user.getInstitution().getId())) {
            // Allow SUPER_ADMIN to login with any institution
            if (user.getRoles() != null && user.getRoles().contains(AppRoles.SUPER_ADMIN)) {
                log.debug("User {} has SUPER_ADMIN role, allowing login with institution {}", username, institutionCode);
            } else {
                log.warn("Login failed: user does not belong to institution");
                throw new BadCredentialsException("User does not belong to this institution");
            }
        }

        // Validate password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed: invalid password");
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.isEnabled()) {
            log.warn("Login failed: user not enabled");
            throw new BadCredentialsException("User is not enabled");
        }

        log.info("Authentication successful for user (institution set)");

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

