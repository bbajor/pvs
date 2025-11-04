package de.bbajor.pvs.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private WhisperProperties whisper = new WhisperProperties();
    private ExtractionProperties extraction = new ExtractionProperties();

    @Getter
    @Setter
    public static class WhisperProperties {
        private LocalWhisperProperties local = new LocalWhisperProperties();
        private RemoteWhisperProperties remote = new RemoteWhisperProperties();
    }

    @Getter
    @Setter
    public static class LocalWhisperProperties {
        private boolean enabled = true;
        private String host = "localhost";
        private int port = 9000;
        private boolean autoInstall = true;
        private String pythonPath = "python";
        private boolean usePodman = true;
        private String podmanImage = "pvs-whisper:latest";
        private String podmanComposePath = "docker/whisper/podman-compose.yml";
    }

    @Getter
    @Setter
    public static class RemoteWhisperProperties {
        private boolean enabled = true;
        private String provider = "aleph-alpha";
        private String apiUrl = "https://api.aleph-alpha.com/complete";
        private String apiKey;
        private int monthlyQuota = 1000;
    }

    @Getter
    @Setter
    public static class ExtractionProperties {
        private double confidenceThreshold = 0.7;
    }

}

