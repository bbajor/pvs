package de.bbajor.pvs.security.pin;

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
import de.bbajor.pvs.security.mfa.MfaService;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import lombok.RequiredArgsConstructor;

/**
 * Authentication provider for passwordless login using PIN + MFA code.
 * <p>
 * This provider allows normal users (non-SUPER_ADMIN, non-INSTITUTION_ADMIN) 
 * to login without password if they have MFA enabled and a PIN set.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class PinAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(PinAuthenticationProvider.class);

    private final InstitutionRepository institutionRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final MfaService mfaService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.debug("PinAuthenticationProvider.authenticate called");

        if (!(authentication instanceof PinAuthenticationToken)) {
            log.debug("Authentication is not PinAuthenticationToken, returning null");
            return null;
        }

        PinAuthenticationToken token = (PinAuthenticationToken) authentication;
        String institutionCode = token.getInstitutionCode();
        String identifier = token.getName(); // Can be username or email
        String pin = token.getPin();
        String mfaCode = token.getMfaCode();

        log.debug("Attempting PIN authentication for institution: {}, identifier: {}", institutionCode, identifier);

        // Find user by username or email
        Optional<UserAccount> userOpt = userAccountRepository.findByUsernameOrEmail(identifier);
        if (userOpt.isEmpty()) {
            log.warn("PIN login failed: User not found: {}", identifier);
            throw new BadCredentialsException("Invalid username/email or PIN");
        }

        UserAccount user = userOpt.get();
        String username = user.getUsername(); // Use actual username from database
        log.debug("User found: {} (Institution: {})", username, 
                user.getInstitution() != null ? user.getInstitution().getInstitutionCode() : "null");

        // PIN login is only for normal users (not SUPER_ADMIN or INSTITUTION_ADMIN)
        boolean hasAdminRole = user.getRoles() != null 
                && (user.getRoles().contains(AppRoles.SUPER_ADMIN) 
                    || user.getRoles().contains(AppRoles.INSTITUTION_ADMIN));
        if (hasAdminRole) {
            log.debug("PIN login not allowed for admin users: {}", username);
            throw new BadCredentialsException("PIN login is not available for admin users");
        }

        // Check if MFA is enabled
        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            log.warn("PIN login failed: MFA not enabled for user: {}", username);
            throw new BadCredentialsException("MFA must be enabled for PIN login");
        }

        // Check if PIN is set
        if (user.getPinHash() == null || user.getPinHash().isEmpty()) {
            log.warn("PIN login failed: PIN not set for user: {}", username);
            throw new BadCredentialsException("PIN not set. Please set up MFA with PIN first.");
        }

        // Validate PIN
        if (!passwordEncoder.matches(pin, user.getPinHash())) {
            log.warn("PIN login failed: Invalid PIN for user: {}", username);
            throw new BadCredentialsException("Invalid PIN");
        }

        // Validate MFA code
        if (mfaCode == null || mfaCode.isEmpty() || mfaCode.length() != 6) {
            log.warn("PIN login failed: MFA code required for user: {}", username);
            throw new BadCredentialsException("MFA code is required");
        }

        if (!mfaService.verifyCode(user.getMfaSecret(), mfaCode)) {
            log.warn("PIN login failed: Invalid MFA code for user: {}", username);
            throw new BadCredentialsException("Invalid MFA code");
        }

        // For regular users, require institution code
        if (institutionCode == null || institutionCode.trim().isEmpty()) {
            log.warn("PIN login failed: Institution code required for user: {}", username);
            throw new BadCredentialsException("Institution code is required");
        }

        // Find Institution
        Optional<Institution> institutionOpt = institutionRepository.findByInstitutionCode(institutionCode);
        Long institutionId = null;

        if (institutionOpt.isPresent()) {
            Institution institution = institutionOpt.get();
            if (!institution.isActive()) {
                log.warn("PIN login failed: Institution not active: {}", institutionCode);
                throw new BadCredentialsException("Institution is not active");
            }
            institutionId = institution.getId();
            log.debug("Institution found: {} (ID: {})", institutionCode, institutionId);
        } else {
            log.warn("PIN login failed: Institution not found: {}", institutionCode);
            throw new BadCredentialsException("Institution not found");
        }

        // Check if user belongs to this institution
        if (user.getInstitution() != null && !institutionId.equals(user.getInstitution().getId())) {
            log.warn("PIN login failed: User {} belongs to institution {} but login attempted with institution {}",
                    username, user.getInstitution().getInstitutionCode(), institutionCode);
            throw new BadCredentialsException("User does not belong to this institution");
        }

        if (!user.isEnabled()) {
            log.warn("PIN login failed: User not enabled: {}", username);
            throw new BadCredentialsException("User is not enabled");
        }

        log.info("PIN authentication successful for user: {} (institution: {})", username, institutionCode);

        UserAccountUserDetailsAdapter principal = new UserAccountUserDetailsAdapter(user);

        return new InstitutionAuthenticationToken(
                institutionCode,
                institutionId,
                principal,
                null, // No password for PIN login
                principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PinAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

