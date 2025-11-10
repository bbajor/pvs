package de.bbajor.pvs.institution.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.server.PathContainer;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom authentication filter that extracts institutionCode from login form and
 * creates InstitutionAuthenticationToken instead of
 * UsernamePasswordAuthenticationToken.
 */
public class InstitutionAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final Logger log = LoggerFactory.getLogger(InstitutionAuthenticationFilter.class);
    private static final String INSTITUTION_CODE_PARAM = "institutionCode";
    private static final String USERNAME_PARAM = "username";
    private static final String PASSWORD_PARAM = "password";

    public InstitutionAuthenticationFilter(String loginPath) {
        super(new PostRequestMatcher(loginPath));
        log.debug("InstitutionAuthenticationFilter initialized for path: {}", loginPath);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {

        String institutionCode = request.getParameter(INSTITUTION_CODE_PARAM);
        String username = request.getParameter(USERNAME_PARAM);
        String password = request.getParameter(PASSWORD_PARAM);

        log.debug("Attempting authentication - institutionCode: {}, username: {}", institutionCode, username);

        if (institutionCode == null || institutionCode.isEmpty()) {
            log.warn("Login attempt without institutionCode");
            throw new ServletException("Institution code is required");
        }

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            log.warn("Login attempt with missing credentials");
            throw new ServletException("Username and password are required");
        }

        // Create InstitutionAuthenticationToken instead of UsernamePasswordAuthenticationToken
        InstitutionAuthenticationToken authRequest = new InstitutionAuthenticationToken(
                institutionCode, username, password);

        log.debug("Created InstitutionAuthenticationToken for authentication");

        Authentication authResult = getAuthenticationManager().authenticate(authRequest);
        log.debug("Authentication result: {}", authResult != null ? "success" : "null");
        return authResult;
    }

    private static final class PostRequestMatcher implements RequestMatcher {

        private static final UrlPathHelper URL_PATH_HELPER = createUrlPathHelper();
        private final PathPattern pathPattern;

        private PostRequestMatcher(String pattern) {
            PathPatternParser parser = new PathPatternParser();
            parser.setMatchOptionalTrailingSeparator(true);
            this.pathPattern = parser.parse(normalizePattern(pattern));
        }

        @Override
        public boolean matches(HttpServletRequest request) {
            if (!"POST".equalsIgnoreCase(request.getMethod())) {
                return false;
            }
            String lookupPath = URL_PATH_HELPER.getPathWithinApplication(request);
            PathContainer path = PathContainer.parsePath(lookupPath);
            return pathPattern.matches(path);
        }

        private static String normalizePattern(String pattern) {
            if (pattern == null || pattern.isEmpty()) {
                throw new IllegalArgumentException("loginPath must not be null or empty");
            }
            return pattern.startsWith("/") ? pattern : "/" + pattern;
        }

        private static UrlPathHelper createUrlPathHelper() {
            UrlPathHelper helper = new UrlPathHelper();
            helper.setRemoveSemicolonContent(false);
            helper.setAlwaysUseFullPath(false);
            return helper;
        }
    }
}

