package de.bbajor.pvs.system.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApplicationUpdateService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationUpdateService.class);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final String currentVersionFile;
    private final String latestVersionUrl;
    private final String updateCommand;
    private final boolean updateEnabled;

    public ApplicationUpdateService(
            @Value("${app.update.current-version-file:/opt/ivomplaner/current/VERSION}") String currentVersionFile,
            @Value("${app.update.latest-version-url:}") String latestVersionUrl,
            @Value("${app.update.command:sudo -n /usr/local/bin/ivomplaner-update-wrapper latest}") String updateCommand,
            @Value("${app.update.enabled:false}") boolean updateEnabled) {
        this.currentVersionFile = currentVersionFile;
        this.latestVersionUrl = latestVersionUrl;
        this.updateCommand = updateCommand;
        this.updateEnabled = updateEnabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .build();
    }

    public ApplicationUpdateStatus getStatus() {
        String installedVersion = resolveInstalledVersion();
        if (!updateEnabled) {
            return ApplicationUpdateStatus.disabled(installedVersion);
        }
        if (latestVersionUrl == null || latestVersionUrl.isBlank()) {
            return ApplicationUpdateStatus.notConfigured(installedVersion);
        }

        try {
            String latestVersion = fetchLatestVersion();
            boolean updateAvailable = !latestVersion.isBlank()
                    && !installedVersion.isBlank()
                    && !latestVersion.equals(installedVersion);
            return ApplicationUpdateStatus.available(installedVersion, latestVersion, updateAvailable);
        } catch (IOException e) {
            log.warn("Failed to check application update status", e);
            return ApplicationUpdateStatus.error(installedVersion, "Update-Status konnte nicht abgerufen werden.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ApplicationUpdateStatus.error(installedVersion, "Update-Status wurde unterbrochen.");
        }
    }

    public void startUpdate() {
        if (!updateEnabled) {
            throw new IllegalStateException("App-Updates sind nicht aktiviert.");
        }
        if (updateCommand == null || updateCommand.isBlank()) {
            throw new IllegalStateException("Update-Kommando ist nicht konfiguriert.");
        }

        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-lc", updateCommand);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            String output = readOutput(process);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Update-Start hat zu lange gedauert.");
            }
            if (process.exitValue() != 0) {
                log.warn("Update command failed with exit code {}: {}", process.exitValue(), output);
                throw new IllegalStateException("Update konnte nicht gestartet werden.");
            }
            log.info("Application update command started successfully: {}", output);
        } catch (IOException e) {
            log.warn("Failed to start application update command", e);
            throw new IllegalStateException("Update konnte nicht gestartet werden.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Update-Start wurde unterbrochen.", e);
        }
    }

    private String resolveInstalledVersion() {
        if (currentVersionFile != null && !currentVersionFile.isBlank()) {
            try {
                String version = Files.readString(Path.of(currentVersionFile), StandardCharsets.UTF_8).trim();
                if (!version.isBlank()) {
                    return version;
                }
            } catch (IOException e) {
                log.debug("Could not read installed version from {}", currentVersionFile, e);
            }
        }
        return readVersionFromEnvironment();
    }

    private String readVersionFromEnvironment() {
        String version = System.getenv("IVOMPLANER_VERSION");
        return version == null || version.isBlank() ? "unknown" : version;
    }

    private String fetchLatestVersion() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(latestVersionUrl))
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Unexpected status code: " + response.statusCode());
        }
        return response.body().trim();
    }

    private String readOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append('\n');
                }
                output.append(line);
            }
            return output.toString();
        }
    }
}
