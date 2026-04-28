package de.bbajor.pvs.security.mfa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;

/**
 * Custom authentication provider that handles password authentication and MFA verification.
 * 
 * <p>
 * This provider extends the standard authentication flow to check for MFA requirements
 * after successful password authentication. If MFA is enabled, it requires an additional
 * MFA code to be provided.
 * </p>
 */
@Component
public class MfaAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(MfaAuthenticationProvider.class);

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountRepository userAccountRepository;
    private final MfaService mfaService;

    public MfaAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            UserAccountRepository userAccountRepository,
            MfaService mfaService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.userAccountRepository = userAccountRepository;
        this.mfaService = mfaService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String identifier = authentication.getName(); // Can be username or email
        String password = authentication.getCredentials().toString();

        log.debug("Authenticating user: {} (can be username or email)", identifier);

        // Load user details (UserDetailsService now supports username or email)
        UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);
        
        // Verify password
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            log.warn("Invalid password for user: {}", identifier);
            throw new BadCredentialsException("Invalid username/email or password");
        }

        // Check if user account exists and get MFA status
        UserAccount userAccount = userAccountRepository.findByUsernameOrEmail(identifier).orElse(null);
        if (userAccount == null) {
            log.warn("User account not found: {}", identifier);
            throw new BadCredentialsException("Invalid username/email or password");
        }
        
        String username = userAccount.getUsername(); // Use actual username from database

        // If MFA is enabled, check if MFA code is provided
        if (userAccount.isMfaEnabled() && userAccount.getMfaSecret() != null) {
            // Check if MFA code is in authentication details
            String mfaCode = null;
            if (authentication.getDetails() instanceof MfaAuthenticationDetails) {
                MfaAuthenticationDetails details = (MfaAuthenticationDetails) authentication.getDetails();
                mfaCode = details.getMfaCode();
            }

            if (mfaCode == null || mfaCode.isEmpty()) {
                log.debug("MFA enabled for user {}, but no MFA code provided", username);
                // Return a special authentication that indicates MFA is required
                // The actual MFA verification will happen in the filter/view layer
                throw new MfaRequiredException("MFA code required");
            }

            // Verify MFA code
            if (!mfaService.verifyCode(userAccount.getMfaSecret(), mfaCode)) {
                log.warn("Invalid MFA code for user: {}", username);
                throw new BadCredentialsException("Invalid MFA code");
            }

            log.debug("MFA code verified for user: {}", username);
        }

        // Create authenticated token
        UsernamePasswordAuthenticationToken authenticatedToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                password,
                userDetails.getAuthorities());

        authenticatedToken.setDetails(authentication.getDetails());

        log.debug("Authentication successful for user: {} (identifier was: {})", username, identifier);
        return authenticatedToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * Authentication details that can carry MFA code.
     */
    public static class MfaAuthenticationDetails {
        private final String mfaCode;

        public MfaAuthenticationDetails(String mfaCode) {
            this.mfaCode = mfaCode;
        }

        public String getMfaCode() {
            return mfaCode;
        }
    }

    /**
     * Exception thrown when MFA is required but not provided.
     */
    public static class MfaRequiredException extends AuthenticationException {
        public MfaRequiredException(String message) {
            super(message);
        }
    }
}
