package de.bbajor.pvs.security.api;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.security.domain.UserId;

@Configuration
@EnableMethodSecurity(jsr250Enabled = true)
@EnableConfigurationProperties(ApiSecurityProperties.class)
public class ApiSecurityConfig {

    @Bean
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            ApiSecurityProperties props,
            CurrentInstitutionService currentInstitutionService) throws Exception {
        var institutionRequired = new InstitutionRequiredAuthorizationManager(currentInstitutionService);
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // health/info public, rest of actuator protected
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").authenticated()
                        // OpenAPI docs (optional)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // AI endpoints process clinical input and must stay behind the tenant gate.
                        .requestMatchers("/api/ai/**").access(institutionRequired)
                        // New API surface
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/me").authenticated()
                        .requestMatchers("/api/v1/**").access(institutionRequired)
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter(props))))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(ApiSecurityProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(props.corsAllowedOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        cfg.setExposedHeaders(List.of("WWW-Authenticate"));
        cfg.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private org.springframework.core.convert.converter.Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter(
            ApiSecurityProperties props) {
        return jwt -> {
            var authorities = extractAuthorities(jwt, props.rolesClaim());
            var principal = new JwtAppUserPrincipal(toAppUser(jwt), authorities);
            return new JwtAppAuthenticationToken(jwt, principal, authorities);
        };
    }

    private static JwtAppUserInfo toAppUser(Jwt jwt) {
        String subject = jwt.getSubject();
        String preferredUsername = firstNonBlank(jwt, "preferred_username", "username", "email", "sub");
        String fullName = firstNonBlank(jwt, "name", "given_name", "preferred_username", "email", "sub");
        String email = jwt.getClaimAsString("email");
        Long institutionId = firstLong(jwt, "institution_id", "institutionId", "tenant_id", "tenantId");
        Locale locale = Locale.ROOT;
        String localeClaim = jwt.getClaimAsString("locale");
        if (localeClaim != null && !localeClaim.isBlank()) {
            locale = Locale.forLanguageTag(localeClaim);
        }
        return new JwtAppUserInfo(UserId.of(subject), preferredUsername, fullName, email, locale, institutionId);
    }

    private static String firstNonBlank(Jwt jwt, String... claimNames) {
        for (String claimName : claimNames) {
            String v = jwt.getClaimAsString(claimName);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return jwt.getSubject();
    }

    private static Long firstLong(Jwt jwt, String... claimNames) {
        for (String claimName : claimNames) {
            Object v = jwt.getClaims().get(claimName);
            if (v instanceof Number n) {
                return n.longValue();
            }
            if (v instanceof String s && !s.isBlank()) {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException ignored) {
                    // ignore invalid claim value
                }
            }
        }
        return null;
    }

    private Collection<? extends GrantedAuthority> extractAuthorities(Jwt jwt, String rolesClaim) {
        Object raw = jwt.getClaims().get(rolesClaim);
        Stream<String> roles = switch (raw) {
            case null -> Stream.empty();
            case String s -> Stream.of(s);
            case Collection<?> c -> c.stream().filter(String.class::isInstance).map(String.class::cast);
            default -> Stream.empty();
        };
        return roles
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}

