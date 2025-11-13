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

import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.mfa.MfaAuthenticationFilter;
import de.bbajor.pvs.security.mfa.MfaAuthenticationProvider;
import de.bbajor.pvs.security.mfa.MfaService;
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
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserAccountRepository userAccountRepository,
            MfaService mfaService) throws Exception {
        // Configure API endpoints first
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/ai/**").permitAll()
                .requestMatchers("/api/egk/health").permitAll()
                .requestMatchers("/api/egk/**").authenticated());
        
        // Add MFA filter before Vaadin security
        MfaAuthenticationFilter mfaFilter = new MfaAuthenticationFilter(userAccountRepository, mfaService);
        http.addFilterBefore(mfaFilter, UsernamePasswordAuthenticationFilter.class);
        
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
}

