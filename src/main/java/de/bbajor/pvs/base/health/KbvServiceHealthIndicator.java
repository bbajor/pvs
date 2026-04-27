package de.bbajor.pvs.base.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Health indicator for KBV Master Data Service.
 * Checks if the KBV service is available at the configured URL.
 */
@Component
@ConditionalOnProperty(name = "kbv.service.health.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class KbvServiceHealthIndicator implements HealthIndicator {

    @Value("${kbv.service.url:http://kbv-service:8081/actuator/health}")
    private String kbvServiceUrl;

    @Override
    public Health health() {
        try {
            URL url = new URL(kbvServiceUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                return Health.up()
                        .withDetail("url", kbvServiceUrl)
                        .build();
            } else {
                return Health.down()
                        .withDetail("url", kbvServiceUrl)
                        .withDetail("error", "Health check returned status: " + responseCode)
                        .build();
            }
        } catch (Exception e) {
            log.debug("KBV service health check failed", e);
            return Health.down()
                    .withDetail("url", kbvServiceUrl)
                    .withDetail("error", "Connection failed: " + e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}


