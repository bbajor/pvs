package de.bbajor.pvs.security.mfa;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.shared.ApplicationConstants;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Filter that handles MFA verification after initial password authentication.
 * 
 * <p>
 * This filter intercepts requests after successful password authentication and checks if MFA is required.
 * If MFA is enabled for the user, it redirects to MFA verification or validates the MFA code.
 * </p>
 */
public class MfaAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MfaAuthenticationFilter.class);
    private static final String MFA_VERIFIED_SESSION_KEY = "MFA_VERIFIED";
    private static final String MFA_REQUIRED_SESSION_KEY = "MFA_REQUIRED";
    private static final String USERNAME_SESSION_KEY = "MFA_USERNAME";

    private final UserAccountRepository userAccountRepository;
    private final MfaService mfaService;

    public MfaAuthenticationFilter(UserAccountRepository userAccountRepository, MfaService mfaService) {
        this.userAccountRepository = userAccountRepository;
        this.mfaService = mfaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip for Vaadin internal requests
        if (isVaadinInternalRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If user is authenticated, check MFA status
        if (authentication != null && authentication.isAuthenticated() && session != null) {
            String username = authentication.getName();
            
            // Check if MFA is already verified in this session
            Boolean mfaVerified = (Boolean) session.getAttribute(MFA_VERIFIED_SESSION_KEY);
            if (Boolean.TRUE.equals(mfaVerified)) {
                // MFA already verified, continue
                filterChain.doFilter(request, response);
                return;
            }

            // Check if user has MFA enabled
            UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
            if (userAccount != null && userAccount.isMfaEnabled() && userAccount.getMfaSecret() != null) {
                // MFA is required but not yet verified
                // Store in session for MFA verification view
                session.setAttribute(MFA_REQUIRED_SESSION_KEY, true);
                session.setAttribute(USERNAME_SESSION_KEY, username);
                
                // If this is not the MFA verification endpoint, redirect to it
                String requestPath = request.getRequestURI();
                if (!requestPath.contains("/mfa-verify") && !requestPath.contains("/mfa-setup")) {
                    // For Vaadin, we'll handle this in the view layer
                    // The filter just marks that MFA is required
                }
            } else {
                // MFA not enabled, mark as verified (no MFA needed)
                session.setAttribute(MFA_VERIFIED_SESSION_KEY, true);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Marks MFA as verified in the session.
     */
    public static void markMfaVerified(HttpSession session) {
        if (session != null) {
            session.setAttribute(MFA_VERIFIED_SESSION_KEY, true);
            session.removeAttribute(MFA_REQUIRED_SESSION_KEY);
        }
    }

    /**
     * Checks if MFA is required for the current session.
     */
    public static boolean isMfaRequired(HttpSession session) {
        if (session == null) {
            return false;
        }
        return Boolean.TRUE.equals(session.getAttribute(MFA_REQUIRED_SESSION_KEY));
    }

    /**
     * Gets the username from session for MFA verification.
     */
    public static String getMfaUsername(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (String) session.getAttribute(USERNAME_SESSION_KEY);
    }

    /**
     * Checks if the request is a Vaadin internal request that should be skipped.
     */
    private boolean isVaadinInternalRequest(HttpServletRequest request) {
        String parameterValue = request.getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER);
        return parameterValue != null
                && RequestType.valueOf(parameterValue) == RequestType.HEARTBEAT;
    }
}
