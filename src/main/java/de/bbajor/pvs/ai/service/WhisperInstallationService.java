package de.bbajor.pvs.ai.service;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.whisper.local.enabled", havingValue = "true", matchIfMissing = false)
public class WhisperInstallationService {

    private static final Logger LOG = LogManager.getLogger(WhisperInstallationService.class);
    private final AiProperties aiProperties;
    private final PodmanWhisperService podmanWhisperService;

    public boolean checkPythonAvailable() {
        try {
            Process process = new ProcessBuilder(
                    aiProperties.getWhisper().getLocal().getPythonPath(), "--version")
                    .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            LOG.warn("Python not found: {}", e.getMessage());
            return false;
        }
    }

    public boolean checkWhisperServerAvailable() {
        try {
            java.net.URI uri = new java.net.URI(
                    "http://" + aiProperties.getWhisper().getLocal().getHost() + ":"
                            + aiProperties.getWhisper().getLocal().getPort() + "/health");
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(2))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(2))
                    .build();
            
            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public void installWhisper() throws IOException, InterruptedException {
        if (aiProperties.getWhisper().getLocal().isUsePodman()) {
            // Use Podman installation
            podmanWhisperService.startWhisperContainer();
        } else {
            // Fallback to Python installation
            if (!checkPythonAvailable()) {
                throw new IllegalStateException("Python not found. Please install Python first or use Podman.");
            }

            String pythonPath = aiProperties.getWhisper().getLocal().getPythonPath();

            // Install faster-whisper and flask
            LOG.info("Installing faster-whisper and flask...");
            Process installProcess = new ProcessBuilder(
                    pythonPath, "-m", "pip", "install", "faster-whisper", "flask", "flask-cors")
                    .inheritIO()
                    .start();
            int exitCode = installProcess.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to install Python dependencies. Exit code: " + exitCode);
            }

            LOG.info("Whisper installation completed successfully");
        }
    }

    public void startWhisperServer() throws IOException, InterruptedException {
        if (aiProperties.getWhisper().getLocal().isUsePodman()) {
            // Check if container is running, start if not
            if (!podmanWhisperService.checkWhisperContainerRunning()) {
                podmanWhisperService.startWhisperContainer();
            }
        } else {
            // Fallback to Python process
            // This would require the old implementation
            LOG.warn("Python-based Whisper server start not implemented. Please use Podman.");
            throw new IllegalStateException("Please use Podman for Whisper installation (set ai.whisper.local.use-podman=true)");
        }
    }

}

