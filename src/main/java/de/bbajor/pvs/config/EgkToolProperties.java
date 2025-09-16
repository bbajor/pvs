package de.bbajor.pvs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "egk")
public class EgkToolProperties {
    private String toolPath;
}