package de.bbajor.pvs.security.api;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserId;

@Configuration
@EnableMethodSecurity(jsr250Enabled = true)
@EnableConfigurationProperties(ApiSecurityProperties.class)
public class ApiSecurityConfig {

    @Bean
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            ApiSecurityProperties props,
            CurrentInstitutionService currentInstitutionService,
            UserAccountRepository userAccountRepository) throws Exception {
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
                        // Existing public AI endpoints (currently used without auth)
                        .requestMatchers("/api/ai/**").permitAll()
                        // New API surface
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/me").authenticated()
                        .requestMatchers("/api/v1/**").access(institutionRequired)
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter(userAccountRepository))))
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

    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter(
            UserAccountRepository userAccountRepository) {
        return jwt -> {
            JwtAppUserInfo tokenUser = toAppUser(jwt);
            Optional<UserAccount> userAccount = findUserAccount(tokenUser, userAccountRepository);
            userAccount.filter(account -> !account.isEnabled())
                    .ifPresent(account -> {
                        throw new DisabledException("User is disabled");
                    });

            var authorities = userAccount
                    .<Collection<? extends GrantedAuthority>>map(account -> authoritiesFromRoles(account.getRoles()))
                    .orElseGet(List::of);
            var principal = new JwtAppUserPrincipal(toAppUser(tokenUser, userAccount.orElse(null)), authorities);
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

    private static JwtAppUserInfo toAppUser(JwtAppUserInfo tokenUser, UserAccount userAccount) {
        if (userAccount == null) {
            return new JwtAppUserInfo(
                    tokenUser.userId(),
                    tokenUser.preferredUsername(),
                    tokenUser.fullName(),
                    tokenUser.email(),
                    tokenUser.locale(),
                    null);
        }

        String userId = userAccount.getUserId();
        if (userId == null || userId.isBlank()) {
            userId = userAccount.getUsername();
        }
        String fullName = userAccount.getFullName();
        if (fullName == null || fullName.isBlank()) {
            fullName = userAccount.getUsername();
        }
        Long institutionId = userAccount.getInstitution() == null ? null : userAccount.getInstitution().getId();

        return new JwtAppUserInfo(
                UserId.of(userId),
                userAccount.getUsername(),
                fullName,
                userAccount.getEmail(),
                tokenUser.locale(),
                institutionId);
    }

    private static Optional<UserAccount> findUserAccount(
            JwtAppUserInfo tokenUser,
            UserAccountRepository userAccountRepository) {
        Optional<UserAccount> byUserId = userAccountRepository.findByUserId(tokenUser.userId().toString());
        if (byUserId.isPresent()) {
            return byUserId;
        }

        return Stream.of(tokenUser.preferredUsername(), tokenUser.email())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .map(userAccountRepository::findByUsernameOrEmail)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
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

    private static Collection<? extends GrantedAuthority> authoritiesFromRoles(Set<String> roles) {
        Set<String> normalizedRoles = new LinkedHashSet<>(roles == null ? Set.of() : roles);
        if (normalizedRoles.contains("INSTITUTION_ADMIN")) {
            normalizedRoles.add(AppRoles.ADMIN);
        }

        return normalizedRoles.stream()
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}

