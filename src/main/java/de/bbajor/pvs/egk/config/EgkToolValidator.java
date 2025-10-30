package de.bbajor.pvs.egk.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class EgkToolValidator {

    private final EgkToolProperties properties;
    private static final Logger log = LoggerFactory.getLogger(EgkToolValidator.class);

    public EgkToolValidator(EgkToolProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void validateEgkTool() {
        if (!properties.isEnabled()) {
            log.warn("EGK validation is disabled (egk.enabled=false). Skipping eGK tool checks.");
            return;
        }
        String toolPath = properties.getToolPath();
        if (toolPath == null || toolPath.isEmpty()) {
            throw new RuntimeException("EGK tool path is not configured!");
        }

        Path path = Paths.get(toolPath);
        if (!Files.exists(path)) {
            throw new RuntimeException("EGK tool not found at: " + toolPath);
        }

        File tool = path.toFile();
        if (!tool.canExecute()) {
            throw new RuntimeException("EGK tool is not executable: " + toolPath);
        }

        String toolDir = path.getParent().toString();
        System.setProperty("java.library.path", toolDir + File.pathSeparator + System.getProperty("java.library.path"));
    }
}