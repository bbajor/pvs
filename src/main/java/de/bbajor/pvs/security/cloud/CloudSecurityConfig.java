package de.bbajor.pvs.security.cloud;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import de.bbajor.pvs.security.mfa.MfaAuthenticationFilter;
import de.bbajor.pvs.security.mfa.MfaAuthenticationProvider;
import de.bbajor.pvs.security.mfa.MfaService;
import de.bbajor.pvs.security.prod.service.ProdUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import de.bbajor.pvs.security.domain.UserAccountRepository;

/**
 * Cloud-specific security configuration.
 * Activated when profile is "cloud".
 * 
 * Features:
 * - HTTPS enforcement (via reverse proxy)
 * - Security headers (HSTS, CSP, etc.)
 * - Rate limiting for login endpoints
 * - CSRF protection
 * - MFA support
 */
@Configuration
@EnableWebSecurity
@Profile("cloud")
@Slf4j
public class CloudSecurityConfig {

    public CloudSecurityConfig() {
        log.info("Using CLOUD security configuration");
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserAccountRepository userAccountRepository,
            MfaService mfaService) throws Exception {
        
        // Configure API endpoints first
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/ai/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").authenticated() // Metrics require auth
                .anyRequest().authenticated());
        
        // Add MFA filter before Vaadin security
        MfaAuthenticationFilter mfaFilter = new MfaAuthenticationFilter(userAccountRepository, mfaService);
        http.addFilterBefore(mfaFilter, UsernamePasswordAuthenticationFilter.class);
        
        // Add rate limiting filter for login endpoints
        RateLimitingFilter rateLimitingFilter = new RateLimitingFilter();
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
        // In cloud, we trust the proxy headers
        http.requiresChannel(channel -> channel
                .requestMatchers(request -> isSecureRequest(request))
                .requiresSecure());
        
        // CSRF protection (enabled by default, but explicit for clarity)
        http.csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/ai/**") // AI endpoints may need CSRF exemption
                .csrfTokenRepository(new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository()));
        
        // Apply Vaadin security
        return http
                .with(VaadinSecurityConfigurer.vaadin(), configurer -> {
                    // Standard Vaadin login view
                })
                .build();
    }

    /**
     * Check if request should be treated as secure.
     * In cloud deployment, we trust X-Forwarded-Proto header from reverse proxy.
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
    AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}



