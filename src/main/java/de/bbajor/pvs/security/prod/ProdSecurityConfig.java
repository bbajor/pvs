package de.bbajor.pvs.security.prod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

import de.bbajor.pvs.institution.security.InstitutionAuthenticationFilter;
import de.bbajor.pvs.security.cloud.RateLimitingFilter;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.mfa.MfaAuthenticationFilter;
import de.bbajor.pvs.security.mfa.MfaAuthenticationProvider;
import de.bbajor.pvs.security.mfa.MfaService;
import de.bbajor.pvs.security.prod.ProdLoginView;
import de.bbajor.pvs.security.prod.service.ProdUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Production-ready security configuration for test and production environments.
 * 
 * <p>
 * This configuration provides secure authentication without development conveniences:
 * <ul>
 * <li>No test credentials displayed</li>
 * <li>No automatic login buttons</li>
 * <li>Standard Spring Security login form</li>
 * <li>Database-based user authentication only</li>
 * <li>Security headers (HSTS, CSP, X-Frame-Options, etc.)</li>
 * <li>Rate limiting for login endpoints</li>
 * <li>HTTPS enforcement (via reverse proxy)</li>
 * <li>CSRF protection</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Activated when profile is "test" or "prod" and ControlCenterSecurityConfig is not available.
 * </p>
 */
@EnableWebSecurity
@Configuration
@Import({ VaadinAwareSecurityContextHolderStrategyConfiguration.class })
@Profile({"test", "prod", "onpremise"})
@org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(de.bbajor.pvs.security.controlcenter.ControlCenterSecurityConfig.class)
public class ProdSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(ProdSecurityConfig.class);

    public ProdSecurityConfig() {
        log.info("Using production-style security configuration for test/prod/onpremise environments");
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserAccountRepository userAccountRepository,
            MfaService mfaService,
            RateLimitingFilter rateLimitingFilter) throws Exception {
        // Configure API endpoints first
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/ai/**").permitAll()
                .requestMatchers("/api/egk/health").permitAll()
                .requestMatchers("/api/egk/**").authenticated()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").authenticated() // Metrics require auth
                .requestMatchers("/" + ProdLoginView.LOGIN_PATH).permitAll()
                .anyRequest().authenticated());
        
        // Add institution authentication filter
        InstitutionAuthenticationFilter institutionAuthFilter = new InstitutionAuthenticationFilter("/" + ProdLoginView.LOGIN_PATH);
        http.addFilterBefore(institutionAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        // Add MFA filter before Vaadin security
        MfaAuthenticationFilter mfaFilter = new MfaAuthenticationFilter(userAccountRepository, mfaService);
        http.addFilterBefore(mfaFilter, UsernamePasswordAuthenticationFilter.class);
        
        // Add rate limiting filter for login endpoints
        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        
        // Security headers
        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'"))
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                        .maxAgeInSeconds(31536000))
                .referrerPolicy(referrer -> referrer
                        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(permissions -> permissions
                        .policy("geolocation=(), microphone=(), camera=()")));
        
        // HTTPS enforcement (assumes reverse proxy handles SSL termination)
        // Trust X-Forwarded-Proto header from reverse proxy
        http.requiresChannel(channel -> channel
                .requestMatchers(request -> isSecureRequest(request))
                .requiresSecure());
        
        // CSRF protection (enabled by default, but explicit for clarity)
        http.csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/ai/**") // AI endpoints may need CSRF exemption
                .csrfTokenRepository(new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository()));
        
        // Apply Vaadin security with production login view
        return http
                .with(VaadinSecurityConfigurer.vaadin(), configurer -> {
                    configurer.loginView(ProdLoginView.LOGIN_PATH);
                })
                .build();
    }
    
    /**
     * Check if request should be treated as secure.
     * In production deployment, we trust X-Forwarded-Proto header from reverse proxy.
     */
    private boolean isSecureRequest(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(forwardedProto) || request.isSecure();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserAccountRepository userAccountRepository) {
        return new ProdUserDetailsService(userAccountRepository);
    }

    @Bean
    MfaAuthenticationProvider mfaAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            UserAccountRepository userAccountRepository,
            MfaService mfaService) {
        return new MfaAuthenticationProvider(userDetailsService, passwordEncoder, userAccountRepository, mfaService);
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig,
            MfaAuthenticationProvider mfaAuthenticationProvider) throws Exception {
        return new ProviderManager(mfaAuthenticationProvider);
    }
    
    /**
     * Register the production login view route dynamically.
     * This ensures the route is available before Vaadin security tries to redirect to it.
     */
    @Bean
    org.springframework.boot.ApplicationRunner prodLoginViewRouteRegistrar() {
        return args -> {
            var routeConfiguration = RouteConfiguration.forApplicationScope();
            routeConfiguration.setRoute(ProdLoginView.LOGIN_PATH, ProdLoginView.class);
            log.info("Registered production login view at route: /{}", ProdLoginView.LOGIN_PATH);
        };
    }
}

