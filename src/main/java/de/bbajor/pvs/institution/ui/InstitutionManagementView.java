package de.bbajor.pvs.institution.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.bbajor.pvs.base.ui.view.MainLayout;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.service.InstitutionService;
import jakarta.annotation.security.RolesAllowed;

import de.bbajor.pvs.security.AppRoles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * View for managing institutions.
 * Accessible by SUPER_ADMIN (full access) and INSTITUTION_ADMIN (institution management only, no data access).
 * Note: This view is not shown in the menu - it's only accessible via SuperAdminSettingsView.
 */
@Route(value = "admin/institutions", layout = MainLayout.class)
@PageTitle("Institution-Verwaltung")
@RolesAllowed({AppRoles.SUPER_ADMIN, AppRoles.INSTITUTION_ADMIN})
public class InstitutionManagementView extends VerticalLayout {

    private final InstitutionService institutionService;
    private final Grid<Institution> grid;
    private final TextField institutionNameField;
    private final TextField institutionCodeField;
    private final TextField descriptionField;

    public InstitutionManagementView(InstitutionService institutionService) {
        this.institutionService = institutionService;

        setSizeFull();
        setPadding(true);

        // Check if user is SUPER_ADMIN or INSTITUTION_ADMIN
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + AppRoles.SUPER_ADMIN));
        boolean isInstitutionAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + AppRoles.INSTITUTION_ADMIN));

        H2 title = new H2("Institution-Verwaltung");
        
        // INSTITUTION_ADMIN can only create institutions, not view/edit them
        // SUPER_ADMIN has full access
        if (isInstitutionAdmin && !isSuperAdmin) {
            // For INSTITUTION_ADMIN: Only show creation form, hide grid
            title.setText("Institution anlegen");
        }

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
            Button deactivateButton = new Button(
                    institution.isActive() ? "Deaktivieren" : "Aktivieren",
                    e -> toggleInstitutionStatus(institution)
            );
            deactivateButton.addThemeVariants(
                    institution.isActive() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS,
                    ButtonVariant.LUMO_SMALL
            );
            return new HorizontalLayout(deactivateButton);
        }).setHeader("Aktionen");

        grid.setSizeFull();

        // Only show grid for SUPER_ADMIN (INSTITUTION_ADMIN cannot see institutions)
        if (isSuperAdmin) {
            add(title, formLayout, grid);
            refreshGrid();
        } else {
            // INSTITUTION_ADMIN: Only show creation form
            add(title, formLayout);
        }
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

