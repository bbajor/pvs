package de.bbajor.pvs.institution.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Vaadin-specific authentication success handler for institution authentication.
 * <p>
 * This handler extends {@link SavedRequestAwareAuthenticationSuccessHandler} to
 * support Spring Security's saved request feature while redirecting to the
 * configured target URL.
 * <p>
 * According to Spring Security documentation and Vaadin routing, a simple HTTP
 * 302 redirect is the correct approach for form-based authentication. Vaadin's
 * router will handle the navigation correctly when the SecurityContext is
 * properly set.
 *
 * @see
 * <a href="https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html">Spring
 * Security Form Login</a>
 * @see <a href="https://vaadin.com/docs/latest/flow/routing">Vaadin Routing</a>
 */
public class VaadinInstitutionAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(VaadinInstitutionAuthenticationSuccessHandler.class);

    /**
     * Creates a new success handler that redirects to the specified target
     * route.
     *
     * @param targetRoute the route to redirect to after successful
     * authentication (e.g., "patient-search")
     */
    public VaadinInstitutionAuthenticationSuccessHandler(String targetRoute) {
        super();
        setDefaultTargetUrl("/" + targetRoute);
        // Always use default target URL to ensure navigation to patient-search
        // Saved requests might interfere with Vaadin routing
        setAlwaysUseDefaultTargetUrl(true);
        log.debug("VaadinInstitutionAuthenticationSuccessHandler initialized with targetRoute: {}", targetRoute);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        log.debug("Authentication successful for user: {} (institution: {})",
                authentication.getName(),
                authentication instanceof InstitutionAuthenticationToken institutionAuth
                        ? institutionAuth.getInstitutionCode()
                        : "unknown");

        String targetUrl = determineTargetUrl(request, response, authentication);
        log.debug("Redirecting to target URL: {}", targetUrl);

        super.onAuthenticationSuccess(request, response, authentication);
    }
}

