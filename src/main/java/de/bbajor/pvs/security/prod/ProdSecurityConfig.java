package de.bbajor.pvs.security.prod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
    "${spring.profiles.active:dev} == 'test' || ${spring.profiles.active:dev} == 'prod'"
)
public class ProdSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(ProdSecurityConfig.class);

    public ProdSecurityConfig() {
        log.info("Using PRODUCTION security configuration for test/prod environments");
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configure API endpoints first
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/ai/**").permitAll());
        
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

