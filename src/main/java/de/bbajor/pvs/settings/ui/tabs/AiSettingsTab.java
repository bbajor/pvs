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
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class AiSettingsTab extends VerticalLayout {

    private final AiProperties aiProperties;
    private final AiUsageService aiUsageService;
    private final InstitutionRepository institutionRepository;

    private Checkbox remoteLlmEnabled;
    private TextField remoteApiUrl;
    private PasswordField remoteApiKey;
    private IntegerField monthlyQuota;

    private Span usageStatsLabel;

    @PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);

        // Get current institution
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            add(new H3("Keine Institution ausgewählt. Bitte melden Sie sich mit einer Institution an."));
            return;
        }

        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalStateException("Institution not found: " + institutionId));

        // Remote LLM Configuration (per Institution)
        H3 remoteTitle = new H3("Remote LLM-Konfiguration");
        remoteLlmEnabled = new Checkbox("Remote LLM aktiviert");
        remoteLlmEnabled.setValue(institution.getRemoteLlmEnabled() != null ? institution.getRemoteLlmEnabled() : false);
        remoteLlmEnabled.addValueChangeListener(e -> {
            institution.setRemoteLlmEnabled(e.getValue());
            institutionRepository.save(institution);
        });

        remoteApiUrl = new TextField("API-URL");
        remoteApiUrl.setValue(institution.getRemoteLlmApiUrl() != null ? institution.getRemoteLlmApiUrl() : "");
        remoteApiUrl.addValueChangeListener(e -> {
            institution.setRemoteLlmApiUrl(e.getValue());
            institutionRepository.save(institution);
        });

        remoteApiKey = new PasswordField("API-Key");
        remoteApiKey.setValue(institution.getRemoteLlmApiKey() != null ? institution.getRemoteLlmApiKey() : "");
        remoteApiKey.addValueChangeListener(e -> {
            institution.setRemoteLlmApiKey(e.getValue());
            institutionRepository.save(institution);
        });

        monthlyQuota = new IntegerField("Monatliches Quota");
        monthlyQuota.setValue(institution.getRemoteLlmMonthlyQuota() != null ? institution.getRemoteLlmMonthlyQuota() : 1000);
        monthlyQuota.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                institution.setRemoteLlmMonthlyQuota(e.getValue());
                institutionRepository.save(institution);
            }
        });

        FormLayout remoteLayout = new FormLayout();
        remoteLayout.add(remoteLlmEnabled, 2);
        remoteLayout.add(remoteApiUrl, 2);
        remoteLayout.add(remoteApiKey, 2);
        remoteLayout.add(monthlyQuota);

        // Usage Statistics
        H3 statsTitle = new H3("Nutzungsstatistiken");
        usageStatsLabel = new Span();
        updateUsageStats();

        add(remoteTitle, remoteLayout);
        add(statsTitle, usageStatsLabel);
    }

    private void updateUsageStats() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            usageStatsLabel.setText("Keine Institution ausgewählt");
            return;
        }

        Institution institution = institutionRepository.findById(institutionId).orElse(null);
        if (institution == null) {
            usageStatsLabel.setText("Institution nicht gefunden");
            return;
        }

        YearMonth currentMonth = YearMonth.now();
        
        long remoteUsage = aiUsageService.getUsageCountForCurrentMonth("remote-llm-" + institutionId);
        
        long quota = institution.getRemoteLlmMonthlyQuota() != null ? institution.getRemoteLlmMonthlyQuota() : 1000;
        long remaining = quota - remoteUsage;
        
        String stats = String.format(
                "Aktueller Monat (%s):%nRemote LLM: %d von %d Anfragen (verbleibend: %d)",
                currentMonth.toString(), remoteUsage, quota, remaining);
        
        usageStatsLabel.setText(stats);
        usageStatsLabel.getStyle().set("white-space", "pre-line");
    }

}

