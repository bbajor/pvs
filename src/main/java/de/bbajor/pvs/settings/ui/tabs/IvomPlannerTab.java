package de.bbajor.pvs.settings.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab for IVOM-Planer settings (Zeitsperre zwischen der Behandlung beider Augen).
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class IvomPlannerTab extends VerticalLayout {

    private final InstitutionRepository institutionRepository;

    private ComboBox<Integer> lockoutDaysComboBox;
    private Button saveButton;

    @PostConstruct
    private void init() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        // Check if InstitutionContext is set
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            H3 errorTitle = new H3("IVOM-Planer");
            add(errorTitle);
            Notification.show("Keine Institution ausgewählt. Bitte melden Sie sich mit einer Institution an.",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalStateException("Institution not found: " + institutionId));

        H3 title = new H3("IVOM-Planer");

        // Zeitsperre zwischen der Behandlung beider Augen
        List<Integer> dayOptions = new ArrayList<>();
        for (int i = 0; i <= 14; i++) {
            dayOptions.add(i);
        }
        
        lockoutDaysComboBox = new ComboBox<>("Zeitsperre zwischen der Behandlung beider Augen");
        lockoutDaysComboBox.setItems(dayOptions);
        lockoutDaysComboBox.setItemLabelGenerator(days -> {
            if (days == 0) {
                return "Keine Sperre";
            } else if (days == 1) {
                return "1 Tag";
            } else {
                return days + " Tage";
            }
        });
        lockoutDaysComboBox.setValue(institution.getIvomEyeTreatmentLockoutDays() != null 
                ? institution.getIvomEyeTreatmentLockoutDays() : 0);
        lockoutDaysComboBox.setWidthFull();
        lockoutDaysComboBox.setHelperText("Verhindert, dass beide Augen innerhalb dieses Zeitraums behandelt werden. " +
                "Beim Buchen eines Termins für ein Auge wird geprüft, ob das andere Auge bereits in diesem Zeitraum behandelt wird.");

        saveButton = new Button("Speichern", e -> saveSettings(institution));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        FormLayout formLayout = new FormLayout();
        formLayout.add(lockoutDaysComboBox, saveButton);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        add(title, formLayout);
    }

    private void saveSettings(Institution institution) {
        try {
            Integer lockoutDays = lockoutDaysComboBox.getValue();
            if (lockoutDays == null) {
                lockoutDays = 0;
            }
            
            institution.setIvomEyeTreatmentLockoutDays(lockoutDays);
            institutionRepository.save(institution);

            Notification.show("IVOM-Planer-Einstellungen wurden erfolgreich gespeichert!", 3000,
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            log.error("Error saving IVOM-Planer settings: {}", e.getMessage(), e);
            Notification.show("Fehler beim Speichern: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}

