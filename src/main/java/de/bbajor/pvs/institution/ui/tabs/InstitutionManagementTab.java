package de.bbajor.pvs.institution.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.service.InstitutionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Tab component for managing institutions and administrators.
 * Used in SuperAdminSettingsView.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class InstitutionManagementTab extends VerticalLayout {

    private final InstitutionService institutionService;

    private Grid<Institution> grid;
    private TextField institutionNameField;
    private TextField institutionCodeField;
    private TextField descriptionField;

    @PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);

        H3 title = new H3("Institution-Verwaltung");

        // Create form
        institutionNameField = new TextField("Praxis-/Einrichtungsname");
        institutionNameField.setRequired(true);
        institutionNameField.setPlaceholder("z.B. Augenarztpraxis Dr. Müller");
        institutionNameField.setWidthFull();

        institutionCodeField = new TextField("Institution-Code");
        institutionCodeField.setPlaceholder("Wird automatisch generiert");
        institutionCodeField.setReadOnly(true);
        institutionCodeField.setWidthFull();

        descriptionField = new TextField("Beschreibung");
        descriptionField.setPlaceholder("Optional");
        descriptionField.setWidthFull();

        Button createButton = new Button("Neue Institution anlegen", e -> createInstitution());
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        FormLayout formLayout = new FormLayout();
        formLayout.add(institutionNameField, institutionCodeField, descriptionField, createButton);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        // Create grid
        grid = new Grid<>(Institution.class, false);
        grid.addColumn(Institution::getInstitutionCode).setHeader("Institution-Code").setSortable(true);
        grid.addColumn(Institution::getInstitutionName).setHeader("Name").setSortable(true);
        grid.addColumn(institution -> institution.isActive() ? "Aktiv" : "Inaktiv")
                .setHeader("Status").setSortable(true);
        grid.addColumn(Institution::getDescription).setHeader("Beschreibung");

        grid.addComponentColumn(institution -> {
            Button toggleButton = new Button(
                    institution.isActive() ? "Deaktivieren" : "Aktivieren",
                    e -> toggleInstitutionStatus(institution)
            );
            toggleButton.addThemeVariants(
                    institution.isActive() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS,
                    ButtonVariant.LUMO_SMALL
            );
            return new HorizontalLayout(toggleButton);
        }).setHeader("Aktionen");

        grid.setSizeFull();

        add(title, formLayout, grid);
        refreshGrid();
    }

    private void createInstitution() {
        String name = institutionNameField.getValue();

        if (name == null || name.trim().isEmpty()) {
            Notification.show("Bitte geben Sie einen Namen ein", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            Institution institution = institutionService.createInstitution(name);
            institution.setDescription(descriptionField.getValue());
            institutionService.save(institution);

            Notification notification = Notification.show(
                    String.format("Institution '%s' wurde erfolgreich angelegt!\nInstitution-Code: %s",
                            institution.getInstitutionName(), institution.getInstitutionCode()),
                    5000,
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            institutionNameField.clear();
            descriptionField.clear();
            refreshGrid();
        } catch (Exception e) {
            Notification.show("Fehler beim Anlegen der Institution: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void toggleInstitutionStatus(Institution institution) {
        try {
            institution.setActive(!institution.isActive());
            institutionService.save(institution);

            Notification.show(
                    String.format("Institution '%s' wurde %s",
                            institution.getInstitutionName(),
                            institution.isActive() ? "aktiviert" : "deaktiviert"),
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            refreshGrid();
        } catch (Exception e) {
            Notification.show("Fehler beim Ändern des Status: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshGrid() {
        grid.setItems(institutionService.findAll());
    }
}

