package de.bbajor.pvs.ai.service;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Stub implementation when Whisper is disabled.
 * Provides no-op implementations to prevent dependency injection errors.
 */
@Slf4j
@Service
@ConditionalOnMissingBean(PodmanWhisperService.class)
@RequiredArgsConstructor
public class PodmanWhisperServiceStub {

    private final AiProperties aiProperties;

    public boolean checkPodmanAvailable() {
        log.debug("Whisper disabled - checkPodmanAvailable() returns false");
        return false;
    }

    public boolean checkWhisperContainerRunning() {
        log.debug("Whisper disabled - checkWhisperContainerRunning() returns false");
        return false;
    }

    public boolean checkWhisperServerAvailable() {
        log.debug("Whisper disabled - checkWhisperServerAvailable() returns false");
        return false;
    }

    public boolean isContainerRunning() {
        return false;
    }

    public void startWhisperContainer() throws IOException, InterruptedException {
        log.warn("Whisper is disabled - container start not available");
        throw new IllegalStateException("Whisper is disabled. Enable it via ai.whisper.local.enabled=true");
    }

    public void stopWhisperContainer() throws IOException, InterruptedException {
        log.debug("Whisper disabled - stopWhisperContainer() no-op");
    }

    public String getContainerLogs(int lines) {
        return "Whisper is disabled";
    }

}

