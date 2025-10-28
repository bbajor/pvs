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
public class DockerWhisperService {

    private static final Logger LOG = LogManager.getLogger(DockerWhisperService.class);
    private final AiProperties aiProperties;

    public boolean checkDockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "--version").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            LOG.warn("Docker not found: {}", e.getMessage());
            return false;
        }
    }

    public boolean checkWhisperContainerRunning() {
        if (!checkDockerAvailable()) {
            return false;
        }

        try {
            Process process = new ProcessBuilder("docker", "ps", "--filter", "name=pvs-whisper", "--format", "{{.Names}}")
                    .start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            boolean isRunning = output.contains("pvs-whisper");
            
            if (!isRunning) {
                LOG.debug("Whisper container not running");
            } else {
                // Also check container health status
                Process healthProcess = new ProcessBuilder("docker", "inspect", "--format", "{{.State.Health.Status}}", "pvs-whisper")
                        .start();
                String healthOutput = new String(healthProcess.getInputStream().readAllBytes()).trim();
                healthProcess.waitFor();
                LOG.debug("Container health status: {}", healthOutput);
            }
            process.waitFor();
            return isRunning;
        } catch (IOException e) {
            LOG.warn("Error checking docker container status: {}", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while checking docker container status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if Whisper server is available (can be called from dialog)
     */
    public boolean checkWhisperServerAvailable() {
        return checkWhisperServerAvailableInternal();
    }

    /**
     * Checks if Whisper container is running (can be called from dialog)
     */
    public boolean isContainerRunning() {
        return checkWhisperContainerRunning();
    }

    public void startWhisperContainer() throws IOException, InterruptedException {
        if (!checkDockerAvailable()) {
            throw new IllegalStateException("Docker not found. Please install Docker first.");
        }

        String composePath = aiProperties.getWhisper().getLocal().getDockerComposePath();
        LOG.info("Starting Whisper container using docker-compose from: {}", composePath);

        // First, ensure Docker engine is running by checking if we can communicate with it
        try {
            Process checkProcess = new ProcessBuilder("docker", "info").start();
            int checkExitCode = checkProcess.waitFor();
            if (checkExitCode != 0) {
                throw new IllegalStateException("Docker engine is not running. Please start Docker Desktop first.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Docker engine is not accessible. Please start Docker Desktop first.", e);
        }

        // Build and start container
        ProcessBuilder pb = new ProcessBuilder("docker", "compose", "-f", composePath, "up", "-d", "--build");
        pb.inheritIO();
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new IOException("Failed to start Whisper container. Exit code: " + exitCode);
        }

        LOG.info("Whisper container started successfully");
        
        // Wait for container to be healthy - increased timeout for first startup
        int maxAttempts = 60; // Increased to 2 minutes for model download
        int attempt = 0;
        while (attempt < maxAttempts) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for container to be ready", e);
            }
            
            // Check if container is running
            if (!checkWhisperContainerRunning()) {
                LOG.warn("Container is not running, attempt {}/{}", attempt + 1, maxAttempts);
                attempt++;
                continue;
            }
            
            // Check if server is responding
            if (checkWhisperServerAvailableInternal()) {
                LOG.info("Whisper container is ready and responding");
                return;
            } else {
                LOG.debug("Container is running but server not yet responding, attempt {}/{}", attempt + 1, maxAttempts);
            }
            attempt++;
        }
        
        // Log container status for debugging
        String logs = getContainerLogs(20);
        LOG.error("Whisper container logs (last 20 lines):\n{}", logs);
        
        throw new IOException("Whisper container started but not responding after " + (maxAttempts * 2) + " seconds. Check container logs above.");
    }

    public void stopWhisperContainer() throws IOException, InterruptedException {
        if (!checkDockerAvailable()) {
            return;
        }

        String composePath = aiProperties.getWhisper().getLocal().getDockerComposePath();
        LOG.info("Stopping Whisper container");

        ProcessBuilder pb = new ProcessBuilder("docker", "compose", "-f", composePath, "down");
        pb.inheritIO();
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            LOG.warn("Failed to stop Whisper container. Exit code: {}", exitCode);
        } else {
            LOG.info("Whisper container stopped successfully");
        }
    }

    private boolean checkWhisperServerAvailableInternal() {
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
        } catch (java.net.http.HttpTimeoutException | java.net.ConnectException e) {
            // Expected exceptions for unavailable server
            return false;
        } catch (Exception e) {
            LOG.debug("Error checking Whisper server availability: {}", e.getMessage());
            return false;
        }
    }

    public String getContainerLogs(int lines) {
        if (!checkDockerAvailable()) {
            return "Docker not available";
        }

        try {
            Process process = new ProcessBuilder("docker", "logs", "--tail", String.valueOf(lines), "pvs-whisper")
                    .start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            return output;
        } catch (IOException e) {
            return "Error retrieving logs: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error retrieving logs: interrupted";
        }
    }

}

