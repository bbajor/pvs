package de.bbajor.pvs.location.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.bbajor.pvs.base.ui.view.MainLayout;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import jakarta.annotation.security.RolesAllowed;
import de.bbajor.pvs.security.AppRoles;

/**
 * View for managing locations (Standorte) of the current institution.
 * Accessible by INSTITUTION_ADMIN, TECH_USER, and OWNER.
 */
@Route(value = "admin/locations", layout = MainLayout.class)
@PageTitle("Standort-Verwaltung")
@RolesAllowed({AppRoles.INSTITUTION_ADMIN, AppRoles.TECH_USER, AppRoles.OWNER})
public class LocationManagementView extends VerticalLayout {

    private final LocationService locationService;
    private final Grid<Location> grid;
    private final TextField locationNameField;
    private final TextField streetField;
    private final TextField houseNumberField;
    private final TextField postalCodeField;
    private final TextField cityField;
    private final TextField countryField;
    private final TextField phoneField;
    private final TextField emailField;
    private final TextArea additionalInfoField;

    public LocationManagementView(LocationService locationService) {
        this.locationService = locationService;

        setSizeFull();
        setPadding(true);

        // Initialize all fields first to avoid initialization errors
        grid = new Grid<>(Location.class, false);
        locationNameField = new TextField("Standort-Name");
        streetField = new TextField("Straße");
        houseNumberField = new TextField("Hausnummer");
        postalCodeField = new TextField("Postleitzahl");
        cityField = new TextField("Stadt");
        countryField = new TextField("Land");
        phoneField = new TextField("Telefon");
        emailField = new TextField("E-Mail");
        additionalInfoField = new TextArea("Zusätzliche Informationen");

        // Check if InstitutionContext is set
        if (InstitutionContext.getInstitutionId() == null) {
            H2 errorTitle = new H2("Standort-Verwaltung");
            add(errorTitle);
            Notification.show("Keine Institution ausgewählt. Bitte melden Sie sich mit einer Institution an.",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        H2 title = new H2("Standort-Verwaltung");

        // Configure form fields
        locationNameField.setRequired(true);
        locationNameField.setPlaceholder("z.B. Hauptsitz, Zweigstelle Nord");
        locationNameField.setWidthFull();

        streetField.setWidthFull();
        houseNumberField.setWidthFull();
        postalCodeField.setWidthFull();
        cityField.setWidthFull();
        countryField.setWidthFull();
        phoneField.setWidthFull();
        emailField.setWidthFull();
        additionalInfoField.setWidthFull();
        additionalInfoField.setHeight("100px");

        Button createButton = new Button("Neuen Standort anlegen", e -> createLocation());
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        FormLayout formLayout = new FormLayout();
        formLayout.add(locationNameField, 2);
        formLayout.add(streetField, houseNumberField);
        formLayout.add(postalCodeField, cityField, countryField);
        formLayout.add(phoneField, emailField);
        formLayout.add(additionalInfoField, 2);
        formLayout.add(createButton, 2);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        // Configure grid
        grid.addColumn(Location::getLocationName).setHeader("Name").setSortable(true);
        grid.addColumn(location -> location.getFullAddress()).setHeader("Adresse");
        grid.addColumn(Location::getPhone).setHeader("Telefon");
        grid.addColumn(Location::getEmail).setHeader("E-Mail");
        grid.addColumn(location -> location.isActive() ? "Aktiv" : "Inaktiv")
                .setHeader("Status").setSortable(true);

        grid.addComponentColumn(location -> {
            Button toggleButton = new Button(
                    location.isActive() ? "Deaktivieren" : "Aktivieren",
                    e -> toggleLocationStatus(location)
            );
            toggleButton.addThemeVariants(
                    location.isActive() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS,
                    ButtonVariant.LUMO_SMALL
            );
            return new HorizontalLayout(toggleButton);
        }).setHeader("Aktionen");

        grid.setSizeFull();

        add(title, formLayout, grid);
        refreshGrid();
    }

    private void createLocation() {
        String name = locationNameField.getValue();

        if (name == null || name.trim().isEmpty()) {
            Notification.show("Bitte geben Sie einen Standort-Namen ein", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            Location location = new Location();
            location.setLocationName(name);
            location.setStreet(streetField.getValue());
            location.setHouseNumber(houseNumberField.getValue());
            location.setPostalCode(postalCodeField.getValue());
            location.setCity(cityField.getValue());
            location.setCountry(countryField.getValue());
            location.setPhone(phoneField.getValue());
            location.setEmail(emailField.getValue());
            location.setAdditionalInfo(additionalInfoField.getValue());
            location.setActive(true); // New locations are active by default

            Location saved = locationService.saveLocation(location);

            Notification notification = Notification.show(
                    String.format("Standort '%s' wurde erfolgreich angelegt!", saved.getLocationName()),
                    3000,
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            clearForm();
            refreshGrid();
        } catch (Exception e) {
            Notification.show("Fehler beim Anlegen des Standorts: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void toggleLocationStatus(Location location) {
        try {
            if (location.isActive()) {
                locationService.deactivateLocation(location.getId());
            } else {
                locationService.activateLocation(location.getId());
            }

            Notification.show(
                    String.format("Standort '%s' wurde %s",
                            location.getLocationName(),
                            location.isActive() ? "deaktiviert" : "aktiviert"),
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

    private void clearForm() {
        locationNameField.clear();
        streetField.clear();
        houseNumberField.clear();
        postalCodeField.clear();
        cityField.clear();
        countryField.clear();
        phoneField.clear();
        emailField.clear();
        additionalInfoField.clear();
    }

    private void refreshGrid() {
        // Show all locations (active and inactive) for management
        grid.setItems(locationService.getAllLocations(false));
    }
}

