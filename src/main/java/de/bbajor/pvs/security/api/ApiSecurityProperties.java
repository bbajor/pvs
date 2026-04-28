package de.bbajor.pvs.security.api;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record ApiSecurityProperties(
        List<String> corsAllowedOrigins,
        String rolesClaim) {

    public ApiSecurityProperties {
        corsAllowedOrigins = corsAllowedOrigins == null ? List.of() : List.copyOf(corsAllowedOrigins);
        rolesClaim = rolesClaim == null || rolesClaim.isBlank() ? "roles" : rolesClaim;
    }
}

