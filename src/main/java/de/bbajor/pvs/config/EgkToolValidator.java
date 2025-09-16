package de.bbajor.pvs.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EgkToolValidator {

    private final EgkToolProperties properties;

    public EgkToolValidator(EgkToolProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void validateEgkTool() {
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

        // Add tool directory to system PATH
        String systemPath = System.getenv("PATH");
        String toolDir = path.getParent().toString();
        System.setProperty("java.library.path", toolDir + File.pathSeparator + System.getProperty("java.library.path"));
    }
}