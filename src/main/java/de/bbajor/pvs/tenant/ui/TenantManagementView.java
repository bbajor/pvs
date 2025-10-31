package de.bbajor.pvs.tenant.ui;

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
import de.bbajor.pvs.tenant.model.Tenant;
import de.bbajor.pvs.tenant.service.TenantService;
import jakarta.annotation.security.RolesAllowed;

/**
 * View for managing tenants.
 * Only accessible by super admins.
 */
@Route(value = "admin/tenants", layout = MainLayout.class)
@PageTitle("Tenant-Verwaltung")
@RolesAllowed("SUPER_ADMIN")
public class TenantManagementView extends VerticalLayout {

    private final TenantService tenantService;
    private final Grid<Tenant> grid;
    private final TextField tenantNameField;
    private final TextField tenantCodeField;
    private final TextField descriptionField;

    public TenantManagementView(TenantService tenantService) {
        this.tenantService = tenantService;

        setSizeFull();
        setPadding(true);

        H2 title = new H2("Tenant-Verwaltung");

        // Create form
        tenantNameField = new TextField("Praxis-/Einrichtungsname");
        tenantNameField.setRequired(true);
        tenantNameField.setPlaceholder("z.B. Augenarztpraxis Dr. Müller");
        tenantNameField.setWidthFull();

        tenantCodeField = new TextField("Tenant-Code");
        tenantCodeField.setPlaceholder("Wird automatisch generiert");
        tenantCodeField.setReadOnly(true);
        tenantCodeField.setWidthFull();

        descriptionField = new TextField("Beschreibung");
        descriptionField.setPlaceholder("Optional");
        descriptionField.setWidthFull();

        Button createButton = new Button("Neuen Tenant anlegen", e -> createTenant());
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        FormLayout formLayout = new FormLayout();
        formLayout.add(tenantNameField, descriptionField, createButton);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        // Create grid
        grid = new Grid<>(Tenant.class, false);
        grid.addColumn(Tenant::getTenantCode).setHeader("Tenant-Code").setSortable(true);
        grid.addColumn(Tenant::getTenantName).setHeader("Name").setSortable(true);
        grid.addColumn(tenant -> tenant.isActive() ? "Aktiv" : "Inaktiv")
                .setHeader("Status").setSortable(true);
        grid.addColumn(Tenant::getDescription).setHeader("Beschreibung");

        grid.addComponentColumn(tenant -> {
            Button deactivateButton = new Button(
                    tenant.isActive() ? "Deaktivieren" : "Aktivieren",
                    e -> toggleTenantStatus(tenant)
            );
            deactivateButton.addThemeVariants(
                    tenant.isActive() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS,
                    ButtonVariant.LUMO_SMALL
            );
            return new HorizontalLayout(deactivateButton);
        }).setHeader("Aktionen");

        grid.setSizeFull();

        add(title, formLayout, grid);
        refreshGrid();
    }

    private void createTenant() {
        String name = tenantNameField.getValue();

        if (name == null || name.trim().isEmpty()) {
            Notification.show("Bitte geben Sie einen Namen ein", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            Tenant tenant = tenantService.createTenant(name);
            tenant.setDescription(descriptionField.getValue());
            tenantService.save(tenant);

            Notification notification = Notification.show(
                    String.format("Tenant '%s' wurde erfolgreich angelegt!\nTenant-Code: %s",
                            tenant.getTenantName(), tenant.getTenantCode()),
                    5000,
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            tenantNameField.clear();
            descriptionField.clear();
            refreshGrid();
        } catch (Exception e) {
            Notification.show("Fehler beim Anlegen des Tenants: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void toggleTenantStatus(Tenant tenant) {
        try {
            tenant.setActive(!tenant.isActive());
            tenantService.save(tenant);

            Notification.show(
                    String.format("Tenant '%s' wurde %s",
                            tenant.getTenantName(),
                            tenant.isActive() ? "aktiviert" : "deaktiviert"),
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
        grid.setItems(tenantService.findAll());
    }
}
