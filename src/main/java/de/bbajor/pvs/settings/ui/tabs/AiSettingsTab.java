package de.bbajor.pvs.settings.ui.tabs;

import java.time.YearMonth;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import de.bbajor.pvs.ai.config.AiProperties;
import de.bbajor.pvs.ai.service.AiUsageService;
import de.bbajor.pvs.ai.service.DockerWhisperService;
import de.bbajor.pvs.ai.service.WhisperInstallationService;
import de.bbajor.pvs.settings.ui.WhisperInstallationDialog;
import lombok.RequiredArgsConstructor;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class AiSettingsTab extends VerticalLayout {

    private final AiProperties aiProperties;
    private final WhisperInstallationService whisperInstallationService;
    private final DockerWhisperService dockerWhisperService;
    private final AiUsageService aiUsageService;

    private Checkbox localWhisperEnabled;
    private TextField whisperHost;
    private IntegerField whisperPort;
    private Button checkWhisperStatusButton;
    private Button installWhisperButton;
    private Span whisperStatusLabel;

    private Checkbox remoteWhisperEnabled;
    private TextField remoteApiUrl;
    private PasswordField remoteApiKey;
    private IntegerField monthlyQuota;

    private Span usageStatsLabel;

    @PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);

        // Whisper Local Configuration
        H3 whisperLocalTitle = new H3("Lokale Whisper-Konfiguration");
        localWhisperEnabled = new Checkbox("Lokaler Whisper aktiviert");
        localWhisperEnabled.setValue(aiProperties.getWhisper().getLocal().isEnabled());
        localWhisperEnabled.addValueChangeListener(e -> {
            aiProperties.getWhisper().getLocal().setEnabled(e.getValue());
            updateStatusLabel();
        });

        whisperHost = new TextField("Host");
        whisperHost.setValue(aiProperties.getWhisper().getLocal().getHost());
        whisperHost.addValueChangeListener(e -> aiProperties.getWhisper().getLocal().setHost(e.getValue()));

        whisperPort = new IntegerField("Port");
        whisperPort.setValue(aiProperties.getWhisper().getLocal().getPort());
        whisperPort.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                aiProperties.getWhisper().getLocal().setPort(e.getValue());
            }
        });

        checkWhisperStatusButton = new Button("Status prüfen", e -> checkWhisperStatus());
        installWhisperButton = new Button("Installation starten", e -> installWhisper());
        installWhisperButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        whisperStatusLabel = new Span();
        whisperStatusLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");

        FormLayout whisperLocalLayout = new FormLayout();
        whisperLocalLayout.add(localWhisperEnabled, 2);
        whisperLocalLayout.add(whisperHost);
        whisperLocalLayout.add(whisperPort);
        whisperLocalLayout.add(checkWhisperStatusButton, installWhisperButton);

        // Remote LLM Configuration
        H3 remoteTitle = new H3("Remote LLM-Konfiguration");
        remoteWhisperEnabled = new Checkbox("Remote LLM aktiviert");
        remoteWhisperEnabled.setValue(aiProperties.getWhisper().getRemote().isEnabled());
        remoteWhisperEnabled.addValueChangeListener(e -> {
            aiProperties.getWhisper().getRemote().setEnabled(e.getValue());
        });

        remoteApiUrl = new TextField("API-URL");
        remoteApiUrl.setValue(aiProperties.getWhisper().getRemote().getApiUrl());
        remoteApiUrl.addValueChangeListener(e -> aiProperties.getWhisper().getRemote().setApiUrl(e.getValue()));

        remoteApiKey = new PasswordField("API-Key");
        String apiKey = aiProperties.getWhisper().getRemote().getApiKey();
        // PasswordField unterstützt keine null-Werte - leeren String verwenden
        remoteApiKey.setValue(apiKey != null ? apiKey : "");
        remoteApiKey.addValueChangeListener(e -> aiProperties.getWhisper().getRemote().setApiKey(e.getValue()));

        monthlyQuota = new IntegerField("Monatliches Quota");
        monthlyQuota.setValue(aiProperties.getWhisper().getRemote().getMonthlyQuota());
        monthlyQuota.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                aiProperties.getWhisper().getRemote().setMonthlyQuota(e.getValue());
            }
        });

        FormLayout remoteLayout = new FormLayout();
        remoteLayout.add(remoteWhisperEnabled, 2);
        remoteLayout.add(remoteApiUrl, 2);
        remoteLayout.add(remoteApiKey, 2);
        remoteLayout.add(monthlyQuota);

        // Usage Statistics
        H3 statsTitle = new H3("Nutzungsstatistiken");
        usageStatsLabel = new Span();
        updateUsageStats();

        add(whisperLocalTitle, whisperLocalLayout, whisperStatusLabel);
        add(remoteTitle, remoteLayout);
        add(statsTitle, usageStatsLabel);

        updateStatusLabel();
    }

    private void checkWhisperStatus() {
        try {
            boolean serverAvailable = whisperInstallationService.checkWhisperServerAvailable();
            boolean dockerAvailable = dockerWhisperService.checkDockerAvailable();
            boolean containerRunning = dockerWhisperService.checkWhisperContainerRunning();
            
            if (serverAvailable) {
                whisperStatusLabel.setText("✓ Whisper-Server ist erreichbar" + 
                    (containerRunning ? " (Container läuft)" : ""));
                whisperStatusLabel.getStyle().set("color", "var(--lumo-success-color)");
            } else if (containerRunning) {
                whisperStatusLabel.setText("⚠ Container läuft, Server antwortet nicht");
                whisperStatusLabel.getStyle().set("color", "var(--lumo-warning-color)");
            } else if (!dockerAvailable) {
                whisperStatusLabel.setText("✗ Docker ist nicht verfügbar");
                whisperStatusLabel.getStyle().set("color", "var(--lumo-error-color)");
            } else {
                whisperStatusLabel.setText("✗ Whisper-Server ist nicht erreichbar");
                whisperStatusLabel.getStyle().set("color", "var(--lumo-error-color)");
            }
        } catch (Exception e) {
            whisperStatusLabel.setText("Fehler beim Prüfen: " + e.getMessage());
            whisperStatusLabel.getStyle().set("color", "var(--lumo-error-color)");
        }
    }

    private void installWhisper() {
        installWhisperButton.setEnabled(false);
        try {
            if (aiProperties.getWhisper().getLocal().isUseDocker()) {
                if (!dockerWhisperService.checkDockerAvailable()) {
                    Notification.show("Docker nicht gefunden. Bitte installieren Sie Docker zuerst.", 5000,
                            Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    installWhisperButton.setEnabled(true);
                    return;
                }
                
                // Open installation dialog for Docker
                WhisperInstallationDialog dialog = new WhisperInstallationDialog(dockerWhisperService, aiProperties);
                dialog.open();
                dialog.addOpenedChangeListener(e -> {
                    if (!e.isOpened()) {
                        installWhisperButton.setEnabled(true);
                        updateStatusLabel();
                    }
                });
                return;
            } else {
                if (!whisperInstallationService.checkPythonAvailable()) {
                    Notification.show("Python nicht gefunden. Bitte installieren Sie Python zuerst oder nutzen Sie Docker.", 5000,
                            Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    installWhisperButton.setEnabled(true);
                    return;
                }
            }

            // Fallback for non-Docker installation
            whisperInstallationService.installWhisper();
            whisperInstallationService.startWhisperServer();
            
            Notification.show("Whisper erfolgreich installiert und gestartet", 3000,
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            updateStatusLabel();
        } catch (Exception e) {
            Notification.show("Fehler bei der Installation: " + e.getMessage(), 5000,
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            installWhisperButton.setEnabled(true);
        }
    }

    private void updateStatusLabel() {
        checkWhisperStatus();
    }

    private void updateUsageStats() {
        YearMonth currentMonth = YearMonth.now();
        
        long localUsage = aiUsageService.getUsageCountForCurrentMonth("local-whisper");
        long remoteUsage = aiUsageService.getUsageCountForCurrentMonth("aleph-alpha");
        
        long quota = aiProperties.getWhisper().getRemote().getMonthlyQuota();
        long remaining = quota - remoteUsage;
        
        String stats = String.format(
                "Aktueller Monat (%s):%nLokal: %d Anfragen%nRemote: %d von %d Anfragen (verbleibend: %d)",
                currentMonth.toString(), localUsage, remoteUsage, quota, remaining);
        
        usageStatsLabel.setText(stats);
        usageStatsLabel.getStyle().set("white-space", "pre-line");
    }

}

