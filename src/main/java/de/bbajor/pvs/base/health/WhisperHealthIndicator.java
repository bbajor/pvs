package de.bbajor.pvs.base.health;

import de.bbajor.pvs.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Health indicator for Whisper AI service (local or remote).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WhisperHealthIndicator implements HealthIndicator {

    private final AiProperties aiProperties;

    @Override
    public Health health() {
        try {
            // Check if local Whisper is enabled and available
            if (aiProperties.getWhisper().getLocal().isEnabled()) {
                String host = aiProperties.getWhisper().getLocal().getHost();
                int port = aiProperties.getWhisper().getLocal().getPort();
                String healthUrl = String.format("http://%s:%d/health", host, port);
                
                try {
                    URL url = new URL(healthUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(2000);
                    connection.setReadTimeout(2000);
                    
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        return Health.up()
                                .withDetail("type", "local")
                                .withDetail("host", host)
                                .withDetail("port", port)
                                .build();
                    } else {
                        return Health.down()
                                .withDetail("type", "local")
                                .withDetail("host", host)
                                .withDetail("port", port)
                                .withDetail("error", "Health check returned status: " + responseCode)
                                .build();
                    }
                } catch (Exception e) {
                    // Local Whisper not available, check remote
                    if (aiProperties.getWhisper().getRemote().isEnabled()) {
                        return Health.up()
                                .withDetail("type", "remote")
                                .withDetail("provider", aiProperties.getWhisper().getRemote().getProvider())
                                .withDetail("note", "Local Whisper unavailable, using remote provider")
                                .build();
                    } else {
                        return Health.down()
                                .withDetail("type", "local")
                                .withDetail("host", host)
                                .withDetail("port", port)
                                .withDetail("error", "Local Whisper unavailable and remote disabled")
                                .withException(e)
                                .build();
                    }
                }
            } else if (aiProperties.getWhisper().getRemote().isEnabled()) {
                // Only remote enabled
                return Health.up()
                        .withDetail("type", "remote")
                        .withDetail("provider", aiProperties.getWhisper().getRemote().getProvider())
                        .build();
            } else {
                // Both disabled
                return Health.down()
                        .withDetail("error", "Both local and remote Whisper are disabled")
                        .build();
            }
        } catch (Exception e) {
            log.error("Error checking Whisper health", e);
            return Health.down()
                    .withDetail("error", "Health check failed: " + e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}


