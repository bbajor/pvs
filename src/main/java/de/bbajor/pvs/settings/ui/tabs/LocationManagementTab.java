package de.bbajor.pvs.settings.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tab for managing locations (Standorte) of the current institution.
 * Aligned with the user management tab: grid + dialog, no side form.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class LocationManagementTab extends VerticalLayout {

    private final LocationService locationService;
    private final InstitutionRepository institutionRepository;

    private Grid<Location> locationGrid;
    private Button createButton;

    private List<Location> allLocations;

    @PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);

        // Check if InstitutionContext is set
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            H3 errorTitle = new H3("Standort-Verwaltung");
            add(errorTitle);
            Notification.show("Keine Institution ausgewählt. Bitte melden Sie sich mit einer Institution an.",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        H3 title = new H3("Standort-Verwaltung");

        // Button section above grid (analog Benutzerverwaltung)
        createButton = new Button("Erstellen", e -> openLocationDialog(null));
        createButton.setIcon(VaadinIcon.PLUS.create());
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout buttonSection = new HorizontalLayout(createButton);
        buttonSection.setSpacing(true);
        buttonSection.setPadding(true);

        // Initialize grid
        locationGrid = new Grid<>(Location.class, false);
        locationGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);

        // Combined renderer for name, address, phone, email (+ Hauptstandort-Badge)
        locationGrid.addColumn(new ComponentRenderer<>(location -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(false);
            layout.setPadding(false);

            String name = location.getLocationName() != null ? location.getLocationName() : "-";
            Span nameSpan = new Span(name);
            nameSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
            layout.add(nameSpan);

            if (location.isMainLocation()) {
                Span mainBadge = new Span("Hauptstandort");
                mainBadge.addClassNames(
                        LumoUtility.Background.PRIMARY,
                        LumoUtility.TextColor.PRIMARY_CONTRAST,
                        LumoUtility.Padding.XSMALL,
                        LumoUtility.BorderRadius.SMALL,
                        LumoUtility.FontSize.XSMALL,
                        LumoUtility.FontWeight.SEMIBOLD
                );
                layout.add(mainBadge);
            }

            // Adresse
            String address = location.getFullAddress() != null ? location.getFullAddress() : "-";
            Span addressSpan = new Span(address);
            addressSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            layout.add(addressSpan);

            // Telefon
            String phone = location.getPhone() != null ? location.getPhone() : "-";
            Span phoneSpan = new Span("Tel: " + phone);
            phoneSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            layout.add(phoneSpan);

            // E-Mail
            String email = location.getEmail() != null ? location.getEmail() : "-";
            Span emailSpan = new Span("E-Mail: " + email);
            emailSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            layout.add(emailSpan);

            return layout;
        })).setHeader("Standort").setSortable(true).setAutoWidth(true);

        // Status column
        locationGrid.addColumn(location -> location.isActive() ? "Aktiv" : "Inaktiv")
                .setHeader("Status")
                .setAutoWidth(true);

        // Actions column (Bearbeiten / Hauptstandort setzen / Aktivieren/Deaktivieren / Löschen)
        locationGrid.addComponentColumn(location -> {
            HorizontalLayout buttonLayout = new HorizontalLayout();
            buttonLayout.setSpacing(true);

            Button editButton = new Button("Bearbeiten", e -> openLocationDialog(location));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

            Button mainButton = new Button("Als Hauptstandort", e -> setAsMainLocation(location));
            mainButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            mainButton.setEnabled(!location.isMainLocation());

            Button toggleButton = new Button(
                    location.isActive() ? "Deaktivieren" : "Aktivieren",
                    e -> toggleLocationStatusWithConfirm(location)
            );
            toggleButton.addThemeVariants(
                    location.isActive() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS,
                    ButtonVariant.LUMO_SMALL
            );

            Button deleteButton = new Button("Löschen", e -> confirmAndDelete(location));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            buttonLayout.add(editButton, mainButton, toggleButton, deleteButton);
            return buttonLayout;
        }).setHeader("Aktionen").setAutoWidth(true);

        locationGrid.setSizeFull();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        add(title, buttonSection, locationGrid);
        expand(locationGrid);

        refreshLocations();
    }

    private void setAsMainLocation(Location location) {
        if (location == null || location.getId() == null) {
            return;
        }
        try {
            location.setMainLocation(true);
            locationService.saveLocation(location);
            Notification.show(
                    String.format("Standort '%s' ist jetzt Hauptstandort", location.getLocationName()),
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshLocations();
        } catch (Exception e) {
            log.error("Error setting main location: {}", e.getMessage(), e);
            Notification.show("Fehler beim Setzen des Hauptstandorts: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void openLocationDialog(Location location) {
        LocationDialog dialog = new LocationDialog(locationService, institutionRepository, location);
        dialog.setOnSaveCallback(this::refreshLocations);
        dialog.open();
    }

    private void toggleLocationStatusWithConfirm(Location location) {
        if (location == null || location.getId() == null) {
            return;
        }

        if (location.isActive()) {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader("Standort deaktivieren");
            confirmDialog.setText(
                    "Möchten Sie diesen Standort wirklich deaktivieren? " +
                            "Er steht dann nicht mehr für neue Patienten oder Termine zur Verfügung."
            );
            confirmDialog.setConfirmText("Deaktivieren");
            confirmDialog.setCancelText("Abbrechen");
            confirmDialog.setConfirmButtonTheme("error primary");
            confirmDialog.addConfirmListener(e -> performToggle(location));
            confirmDialog.open();
        } else {
            performToggle(location);
        }
    }

    private void performToggle(Location location) {
        try {
            if (location.isActive()) {
                locationService.deactivateLocation(location.getId());
                Notification.show(
                        String.format("Standort '%s' wurde deaktiviert", location.getLocationName()),
                        3000,
                        Notification.Position.MIDDLE
                ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                locationService.activateLocation(location.getId());
                Notification.show(
                        String.format("Standort '%s' wurde aktiviert", location.getLocationName()),
                        3000,
                        Notification.Position.MIDDLE
                ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }

            refreshLocations();
        } catch (Exception e) {
            log.error("Error toggling location status: {}", e.getMessage(), e);
            Notification.show("Fehler beim Ändern des Status: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshLocations() {
        allLocations = locationService.getAllLocations(false);
        locationGrid.setItems(allLocations);
    }

    private void confirmAndDelete(Location location) {
        if (location == null || location.getId() == null) {
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Standort löschen");
        dialog.setText("Möchten Sie diesen Standort wirklich löschen? " +
                "Dies ist nur möglich, wenn keine Patienten, Terminplaner oder Benutzer diesen Standort verwenden.");
        dialog.setConfirmText("Löschen");
        dialog.setCancelText("Abbrechen");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> performDelete(location));
        dialog.open();
    }

    private void performDelete(Location location) {
        try {
            locationService.deleteLocation(location.getId());
            Notification.show(
                    String.format("Standort '%s' wurde gelöscht", location.getLocationName()),
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshLocations();
        } catch (IllegalStateException ex) {
            Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            log.error("Error deleting location: {}", ex.getMessage(), ex);
            Notification.show("Fehler beim Löschen des Standorts: " + ex.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}

