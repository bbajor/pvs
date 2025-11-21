package de.bbajor.pvs.institution.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.model.InstitutionFeature;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.institution.service.FeatureFlagService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tab component for managing feature flags per institution.
 * Used in SuperAdminSettingsView.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class FeatureFlagsTab extends VerticalLayout {

    private final FeatureFlagService featureFlagService;
    private final InstitutionRepository institutionRepository;

    private Grid<InstitutionFeature> grid;
    private ComboBox<Institution> institutionComboBox;

    @PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);

        H3 title = new H3("Feature-Flags pro Institution");

        // Institution-Auswahl
        institutionComboBox = new ComboBox<>("Institution auswählen");
        institutionComboBox.setItems(institutionRepository.findAll());
        institutionComboBox.setItemLabelGenerator(Institution::getInstitutionName);
        institutionComboBox.setWidthFull();
        institutionComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                refreshGrid(e.getValue().getId());
            } else {
                grid.setItems(List.of());
            }
        });

        // Grid für Features
        grid = new Grid<>(InstitutionFeature.class, false);
        grid.addColumn(InstitutionFeature::getFeatureName).setHeader("Feature").setSortable(true);
        grid.addColumn(InstitutionFeature::getFeatureKey).setHeader("Schlüssel").setSortable(true);
        grid.addColumn(InstitutionFeature::getDescription).setHeader("Beschreibung");
        
        grid.addColumn(new ComponentRenderer<>(feature -> {
            Checkbox enabledCheckbox = new Checkbox();
            enabledCheckbox.setValue(feature.isEnabled());
            enabledCheckbox.addValueChangeListener(e -> {
                featureFlagService.setFeatureEnabled(
                    feature.getInstitution().getId(),
                    feature.getFeatureKey(),
                    feature.getFeatureName(),
                    feature.getDescription(),
                    e.getValue(),
                    feature.isBeta()
                );
                Notification.show("Feature " + (e.getValue() ? "aktiviert" : "deaktiviert"), 2000,
                    Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(e.getValue() ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_CONTRAST);
                refreshGrid(feature.getInstitution().getId());
            });
            return enabledCheckbox;
        })).setHeader("Aktiviert").setAutoWidth(true);
        
        grid.addColumn(new ComponentRenderer<>(feature -> {
            Span betaSpan = new Span(feature.isBeta() ? "Beta" : "Stable");
            betaSpan.getStyle()
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "500");
            if (feature.isBeta()) {
                betaSpan.getStyle()
                    .set("background-color", "var(--lumo-warning-color-10pct)")
                    .set("color", "var(--lumo-warning-text-color)");
            } else {
                betaSpan.getStyle()
                    .set("background-color", "var(--lumo-success-color-10pct)")
                    .set("color", "var(--lumo-success-text-color)");
            }
            return betaSpan;
        })).setHeader("Status").setAutoWidth(true);

        grid.setSizeFull();

        // Button zum Initialisieren der Standard-Features
        Button initButton = new Button("Standard-Features initialisieren", e -> {
            Institution selected = institutionComboBox.getValue();
            if (selected == null) {
                Notification.show("Bitte wählen Sie zuerst eine Institution aus", 3000,
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            try {
                featureFlagService.initializeDefaultFeatures(selected.getId());
                Notification.show("Standard-Features wurden initialisiert", 3000,
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshGrid(selected.getId());
            } catch (Exception ex) {
                Notification.show("Fehler beim Initialisieren: " + ex.getMessage(), 5000,
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        initButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout headerLayout = new HorizontalLayout(institutionComboBox, initButton);
        headerLayout.setWidthFull();
        headerLayout.setFlexGrow(1, institutionComboBox);

        add(title, headerLayout, grid);
    }

    private void refreshGrid(Long institutionId) {
        List<InstitutionFeature> features = featureFlagService.getFeaturesForInstitution(institutionId);
        grid.setItems(features);
    }
}

