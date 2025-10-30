package de.bbajor.pvs.security.prod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.prod.service.ProdUserDetailsService;

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
@Profile({"test", "prod"})
@org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(de.bbajor.pvs.security.controlcenter.ControlCenterSecurityConfig.class)
public class ProdSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(ProdSecurityConfig.class);

    public ProdSecurityConfig() {
        log.info("Using PRODUCTION security configuration for test/prod environments");
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configure API endpoints first
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/ai/**").permitAll()
                // Actuator Health-Check public (für Load Balancer & Health Monitoring)
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // Andere Actuator-Endpoints nur für authentifizierte User
                .requestMatchers("/actuator/**").authenticated());
        
        // Session-Management & Security-Features
        http.sessionManagement(session -> session
                .sessionFixation().newSession()  // Neue Session nach Login (Session-Fixation-Protection)
                .maximumSessions(1)              // Nur eine Session pro User
                .maxSessionsPreventsLogin(false) // Alte Session invalidieren
        );
        
        // Forward-Headers-Strategy für Reverse-Proxy (Traefik)
        http.headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())  // X-Frame-Options: DENY
        );
        
        // Apply Vaadin security with standard login (no dev conveniences)
        return http
                .with(VaadinSecurityConfigurer.vaadin(), configurer -> {
                    // Use standard Vaadin login view - no custom dev features
                })
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserAccountRepository userAccountRepository) {
        return new ProdUserDetailsService(userAccountRepository);
    }
}

