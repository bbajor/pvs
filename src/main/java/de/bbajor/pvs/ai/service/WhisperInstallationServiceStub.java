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
@ConditionalOnMissingBean(WhisperInstallationService.class)
@RequiredArgsConstructor
public class WhisperInstallationServiceStub {

    private final AiProperties aiProperties;

    public boolean checkPythonAvailable() {
        log.debug("Whisper disabled - checkPythonAvailable() returns false");
        return false;
    }

    public boolean checkWhisperServerAvailable() {
        log.debug("Whisper disabled - checkWhisperServerAvailable() returns false");
        return false;
    }

    public void installWhisper() throws IOException, InterruptedException {
        log.warn("Whisper is disabled - installation not available");
        throw new IllegalStateException("Whisper is disabled. Enable it via ai.whisper.local.enabled=true");
    }

    public void startWhisperServer() throws IOException, InterruptedException {
        log.warn("Whisper is disabled - server start not available");
        throw new IllegalStateException("Whisper is disabled. Enable it via ai.whisper.local.enabled=true");
    }

}

