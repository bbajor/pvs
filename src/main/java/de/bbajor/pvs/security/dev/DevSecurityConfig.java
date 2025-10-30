package de.bbajor.pvs.security.dev;

import de.bbajor.pvs.security.controlcenter.ControlCenterSecurityConfig;
import de.bbajor.pvs.security.domain.UserAccountRepository;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configure API endpoints first, before Vaadin configurer applies anyRequest()
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/ai/**").permitAll());
        
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
        return new DevUserDetailsService(SampleUsers.ALL_USERS, userAccountRepository);
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
