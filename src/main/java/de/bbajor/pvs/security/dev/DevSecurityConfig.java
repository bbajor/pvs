package de.bbajor.pvs.security.dev;

import de.bbajor.pvs.security.controlcenter.ControlCenterSecurityConfig;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.mfa.MfaAuthenticationFilter;
import de.bbajor.pvs.security.mfa.MfaService;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationFilter;
import de.bbajor.pvs.institution.security.VaadinInstitutionAuthenticationSuccessHandler;

/**
 * Security configuration for the development environment.
 * <p>
 * This configuration simplifies authentication during development by:
 * <ul>
 * <li>Using a simple login view for authentication</li>
 * <li>Providing predefined test users with fixed credentials</li>
 * <li>Supporting both in-memory test users and database-stored user accounts</li>
 * </ul>
 * </p>
 * <p>
 * This configuration is automatically activated when {@link ControlCenterSecurityConfig} is not active. It should
 * <strong>not</strong> be used in production environments, as it uses simplified security settings.
 * </p>
 * <p>
 * The predefined users are declared in the {@link SampleUsers} class. Additionally, users can be created through
 * the Benutzerverwaltung UI and will be stored in the database.
 * </p>
 * <p>
 * This configuration integrates with Vaadin's security framework through {@link VaadinSecurityConfigurer} to provide a
 * seamless login experience in the Vaadin UI.
 * </p>
 *
 * @see DevUserDetailsService The hybrid user details service implementation
 * @see DevUser Builder for creating development test users
 * @see SampleUsers User credentials for the predefined users
 */
@EnableWebSecurity
@Configuration
@Import({ VaadinAwareSecurityContextHolderStrategyConfiguration.class })
@ConditionalOnMissingBean({ControlCenterSecurityConfig.class, de.bbajor.pvs.security.prod.ProdSecurityConfig.class})
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "spring.profiles.active",
    havingValue = "dev",
    matchIfMissing = true  // Default to dev if no profile is set
)
class DevSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(DevSecurityConfig.class);

    DevSecurityConfig() {
        log.warn("Using DEVELOPMENT security configuration. This should not be used in production environments!");
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, 
            AuthenticationManager authenticationManager,
            UserAccountRepository userAccountRepository,
            MfaService mfaService) throws Exception {
        // Configure API endpoints and login path first, before Vaadin configurer applies anyRequest()
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/ai/**").permitAll()
                .requestMatchers("/api/egk/health").permitAll()
                .requestMatchers("/api/egk/**").authenticated()
                .requestMatchers("/" + DevLoginView.LOGIN_PATH).permitAll());
        
        // Add MFA filter before institution authentication filter
        MfaAuthenticationFilter mfaFilter = new MfaAuthenticationFilter(userAccountRepository, mfaService);
        http.addFilterBefore(mfaFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        
        // Add custom institution authentication filter before Vaadin security
        InstitutionAuthenticationFilter institutionAuthFilter = new InstitutionAuthenticationFilter("/" + DevLoginView.LOGIN_PATH);
        institutionAuthFilter.setAuthenticationManager(authenticationManager);
        
        // Configure success/failure handlers
        // Success handler uses JavaScript to navigate directly to dashboard
        // This preserves the SecurityContext in the Vaadin thread
        AuthenticationSuccessHandler successHandler = new VaadinInstitutionAuthenticationSuccessHandler("");
        AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler(
                "/" + DevLoginView.LOGIN_PATH + "?error");
        institutionAuthFilter.setAuthenticationSuccessHandler(successHandler);
        institutionAuthFilter.setAuthenticationFailureHandler(failureHandler);
        
        http.addFilterBefore(institutionAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        
        // Then apply Vaadin security configuration (which will add anyRequest().authenticated())
        return http
                .with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(DevLoginView.LOGIN_PATH))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserAccountRepository userAccountRepository) {
        // All credentials come from database - no in-memory sample users
        return new DevUserDetailsService(userAccountRepository);
    }

    @Bean
    VaadinServiceInitListener developmentLoginConfigurer(
            @Value("${vaadin.allow-production-mode-in-dev:false}") boolean allowProductionModeInDev) {
        return (serviceInitEvent) -> {
            if (serviceInitEvent.getSource().getDeploymentConfiguration().isProductionMode()) {
                if (allowProductionModeInDev) {
                    log.warn("Development profile is active but Vaadin is running in production mode. " +
                            "This is allowed for Docker-based development environments.");
                } else {
                    throw new IllegalStateException(
                            "Development profile is active but Vaadin is running in production mode. " +
                            "This indicates a configuration error - development profile should not be used in production. " +
                            "If you need this for Docker development, set 'vaadin.allow-production-mode-in-dev=true'.");
                }
            }
            var routeConfiguration = RouteConfiguration.forApplicationScope();
            routeConfiguration.setRoute(DevLoginView.LOGIN_PATH, DevLoginView.class);
        };
    }
}
