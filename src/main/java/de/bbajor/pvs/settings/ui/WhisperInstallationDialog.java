package de.bbajor.pvs.settings.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;

import de.bbajor.pvs.ai.config.AiProperties;
import de.bbajor.pvs.ai.service.DockerWhisperService;

public class WhisperInstallationDialog extends Dialog {

    private final DockerWhisperService dockerWhisperService;
    private final AiProperties aiProperties;
    private final TextArea logOutput;
    private final ProgressBar progressBar;
    private final Button closeButton;
    private final Button cancelButton;
    private final H3 description;
    private Process dockerProcess;
    private boolean cancelled = false;

    public WhisperInstallationDialog(DockerWhisperService dockerWhisperService, AiProperties aiProperties) {
        this.dockerWhisperService = dockerWhisperService;
        this.aiProperties = aiProperties;

        setHeaderTitle("Whisper Installation");
        setWidth("800px");
        setHeight("600px");
        setModal(true);
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(true);

        // Description
        description = new H3("Docker-Container wird erstellt und gestartet...");
        description.getStyle().set("margin-top", "0");
        content.add(description);

        // Progress bar (indeterminate)
        progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidthFull();
        content.add(progressBar);

        // Log output area
        logOutput = new TextArea();
        logOutput.setLabel("Installations-Fortschritt");
        logOutput.setReadOnly(true);
        logOutput.setWidthFull();
        logOutput.setHeight("400px");
        logOutput.setValue("Starte Docker-Installation...\n");
        content.add(logOutput);
        content.setFlexGrow(1, logOutput);

        // Buttons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);

        cancelButton = new Button("Abbrechen", VaadinIcon.CLOSE.create());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancelButton.addClickListener(e -> cancelInstallation());

        closeButton = new Button("Schließen", VaadinIcon.CHECK.create());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.setEnabled(false);
        closeButton.setVisible(false);
        closeButton.addClickListener(e -> close());

        buttonLayout.add(cancelButton, closeButton);
        content.add(buttonLayout);

        add(content);

        // Start installation
        startInstallation();
    }

    private void startInstallation() {
        appendLog("Prüfe Docker Engine...");
        
        if (!dockerWhisperService.checkDockerAvailable()) {
            appendLog("FEHLER: Docker ist nicht verfügbar. Bitte installieren Sie Docker Desktop.");
            showError();
            return;
        }

        appendLog("Docker Engine gefunden.");
        appendLog("Starte Whisper-Container...");
        appendLog("Dies kann einige Minuten dauern, besonders beim ersten Start.");

        // Run installation asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                return executeDockerBuild();
            } catch (Exception e) {
                appendLog("FEHLER: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }).thenAccept(success -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                if (cancelled) {
                    return;
                }
                
                if (success) {
                    appendLog("\n✓ Installation erfolgreich abgeschlossen!");
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(1.0);
                    description.setText("Installation erfolgreich!");
                    cancelButton.setVisible(false);
                    closeButton.setVisible(true);
                    closeButton.setEnabled(true);
                } else {
                    showError();
                }
            }));
        });
    }

    private boolean executeDockerBuild() {
        try {
            appendLog("Führe 'docker compose up --build' aus...");
            
            // Get compose path from properties
            String composePath = aiProperties.getWhisper().getLocal().getDockerComposePath();
            
            // Resolve relative path to absolute path if needed
            if (!composePath.startsWith("/") && !composePath.matches("^[A-Za-z]:.*")) {
                // Relative path - resolve from project root
                String projectRoot = System.getProperty("user.dir");
                composePath = projectRoot + "/" + composePath;
            }
            
            appendLog("Verwende Compose-Datei: " + composePath);
            
            // Use docker compose command
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "compose", "-f", composePath, "up", "-d", "--build"
            );
            pb.redirectErrorStream(true);
            dockerProcess = pb.start();

            // Read output line by line
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(dockerProcess.getInputStream()))) {
                
                String line;
                while ((line = reader.readLine()) != null && !cancelled) {
                    final String logLine = line;
                    appendLog(logLine);
                    
                    // Check if container is starting
                    if (line.contains("Creating") || line.contains("Starting")) {
                        appendLog("Container wird erstellt...");
                    } else if (line.contains("Pulling")) {
                        appendLog("Docker-Image wird heruntergeladen...");
                    } else if (line.contains("Building")) {
                        appendLog("Container wird gebaut...");
                    }
                }
            }

            if (cancelled) {
                dockerProcess.destroyForcibly();
                appendLog("\nInstallation wurde abgebrochen.");
                return false;
            }

            int exitCode = dockerProcess.waitFor();
            
            if (exitCode == 0) {
                appendLog("\nWarte auf Container-Bereitschaft...");
                
                // Wait for container to be ready
                int attempts = 0;
                while (attempts < 60 && !cancelled) {
                    Thread.sleep(2000);
                    attempts++;
                    
                    if (dockerWhisperService.isContainerRunning()) {
                        if (dockerWhisperService.checkWhisperServerAvailable()) {
                            appendLog("Container ist bereit und antwortet!");
                            return true;
                        }
                    }
                    
                    if (attempts % 5 == 0) {
                        appendLog("Warte auf Container-Bereitschaft... (" + attempts + "/60)");
                    }
                }
                
                if (cancelled) {
                    return false;
                }
                
                appendLog("WARNUNG: Container läuft, aber Server antwortet noch nicht.");
                return true; // Container is running, even if not fully ready
            } else {
                appendLog("FEHLER: Docker-Kommando fehlgeschlagen mit Exit-Code: " + exitCode);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            appendLog("Installation wurde unterbrochen.");
            return false;
        } catch (Exception e) {
            appendLog("FEHLER: " + e.getMessage());
            return false;
        }
    }

    private void appendLog(String message) {
        getUI().ifPresent(ui -> ui.access(() -> {
            String current = logOutput.getValue();
            logOutput.setValue(current + message + "\n");
            // Auto-scroll to bottom
            logOutput.getElement().executeJs(
                "this.scrollTop = this.scrollHeight;"
            );
        }));
    }

    private void cancelInstallation() {
        cancelled = true;
        if (dockerProcess != null && dockerProcess.isAlive()) {
            appendLog("\nInstallation wird abgebrochen...");
            dockerProcess.destroyForcibly();
        }
        description.setText("Installation abgebrochen");
        cancelButton.setEnabled(false);
        closeButton.setVisible(true);
        closeButton.setEnabled(true);
        closeButton.setText("Schließen");
        progressBar.setIndeterminate(false);
    }

    private void showError() {
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        description.setText("Installation fehlgeschlagen");
        cancelButton.setVisible(false);
        closeButton.setVisible(true);
        closeButton.setEnabled(true);
        closeButton.setText("Schließen");
    }
}

