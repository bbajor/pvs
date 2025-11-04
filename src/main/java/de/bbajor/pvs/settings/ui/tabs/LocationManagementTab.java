package de.bbajor.pvs.settings.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Tab for managing locations (Standorte) of the current institution.
 * Allows adding, editing, activating, and deactivating locations.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class LocationManagementTab extends VerticalLayout {

    private final LocationService locationService;
    private final InstitutionRepository institutionRepository;

    private Grid<Location> grid;
    private TextField locationNameField;
    private TextField streetField;
    private TextField houseNumberField;
    private TextField postalCodeField;
    private TextField cityField;
    private TextField countryField;
    private TextField phoneField;
    private TextField emailField;
    private TextArea additionalInfoField;
    private Button saveButton;
    private Location selectedLocation;

    @PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);

        // Check if InstitutionContext is set
        if (InstitutionContext.getInstitutionId() == null) {
            H3 errorTitle = new H3("Standort-Verwaltung");
            add(errorTitle);
            Notification.show("Keine Institution ausgewählt. Bitte melden Sie sich mit einer Institution an.",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        H3 title = new H3("Standort-Verwaltung");

        // Initialize all fields
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

        saveButton = new Button("Änderungen speichern", e -> saveLocation());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setEnabled(false);

        Button cancelButton = new Button("Abbrechen", e -> clearForm());

        FormLayout formLayout = new FormLayout();
        formLayout.add(locationNameField, 2);
        formLayout.add(streetField, houseNumberField);
        formLayout.add(postalCodeField, cityField, countryField);
        formLayout.add(phoneField, emailField);
        formLayout.add(additionalInfoField, 2);
        formLayout.add(createButton, 2);
        formLayout.add(saveButton, cancelButton);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        // Configure grid
        grid.addColumn(Location::getLocationName).setHeader("Name").setSortable(true);
        grid.addColumn(Location::getFullAddress).setHeader("Adresse");
        grid.addColumn(Location::getPhone).setHeader("Telefon");
        grid.addColumn(Location::getEmail).setHeader("E-Mail");
        grid.addColumn(location -> location.isActive() ? "Aktiv" : "Inaktiv")
                .setHeader("Status").setSortable(true);

        grid.addComponentColumn(location -> {
            Button editButton = new Button("Bearbeiten", e -> editLocation(location));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            
            Button toggleButton = new Button(
                    location.isActive() ? "Deaktivieren" : "Aktivieren",
                    e -> toggleLocationStatus(location)
            );
            toggleButton.addThemeVariants(
                    location.isActive() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS,
                    ButtonVariant.LUMO_SMALL
            );
            return new HorizontalLayout(editButton, toggleButton);
        }).setHeader("Aktionen");

        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                editLocation(e.getValue());
            } else {
                clearForm();
            }
        });

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
            location.setActive(true);

            // Set institution from current context
            Long institutionId = InstitutionContext.getInstitutionId();
            Institution institution = institutionRepository.findById(institutionId)
                    .orElseThrow(() -> new IllegalStateException("Institution not found: " + institutionId));
            location.setInstitution(institution);

            locationService.saveLocation(location);

            Notification.show(
                    String.format("Standort '%s' wurde erfolgreich angelegt!", location.getLocationName()),
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            clearForm();
            refreshGrid();
        } catch (Exception e) {
            log.error("Error creating location: {}", e.getMessage(), e);
            Notification.show("Fehler beim Anlegen des Standorts: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void editLocation(Location location) {
        selectedLocation = location;
        locationNameField.setValue(location.getLocationName() != null ? location.getLocationName() : "");
        streetField.setValue(location.getStreet() != null ? location.getStreet() : "");
        houseNumberField.setValue(location.getHouseNumber() != null ? location.getHouseNumber() : "");
        postalCodeField.setValue(location.getPostalCode() != null ? location.getPostalCode() : "");
        cityField.setValue(location.getCity() != null ? location.getCity() : "");
        countryField.setValue(location.getCountry() != null ? location.getCountry() : "");
        phoneField.setValue(location.getPhone() != null ? location.getPhone() : "");
        emailField.setValue(location.getEmail() != null ? location.getEmail() : "");
        additionalInfoField.setValue(location.getAdditionalInfo() != null ? location.getAdditionalInfo() : "");
        saveButton.setEnabled(true);
    }

    private void saveLocation() {
        if (selectedLocation == null) {
            return;
        }

        String name = locationNameField.getValue();
        if (name == null || name.trim().isEmpty()) {
            Notification.show("Bitte geben Sie einen Standort-Namen ein", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            selectedLocation.setLocationName(name);
            selectedLocation.setStreet(streetField.getValue());
            selectedLocation.setHouseNumber(houseNumberField.getValue());
            selectedLocation.setPostalCode(postalCodeField.getValue());
            selectedLocation.setCity(cityField.getValue());
            selectedLocation.setCountry(countryField.getValue());
            selectedLocation.setPhone(phoneField.getValue());
            selectedLocation.setEmail(emailField.getValue());
            selectedLocation.setAdditionalInfo(additionalInfoField.getValue());

            locationService.saveLocation(selectedLocation);

            Notification.show(
                    String.format("Standort '%s' wurde erfolgreich aktualisiert!", selectedLocation.getLocationName()),
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            clearForm();
            refreshGrid();
        } catch (Exception e) {
            log.error("Error saving location: {}", e.getMessage(), e);
            Notification.show("Fehler beim Speichern: " + e.getMessage(),
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
            log.error("Error toggling location status: {}", e.getMessage(), e);
            Notification.show("Fehler beim Ändern des Status: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void clearForm() {
        selectedLocation = null;
        locationNameField.clear();
        streetField.clear();
        houseNumberField.clear();
        postalCodeField.clear();
        cityField.clear();
        countryField.clear();
        phoneField.clear();
        emailField.clear();
        additionalInfoField.clear();
        saveButton.setEnabled(false);
        grid.deselectAll();
    }

    private void refreshGrid() {
        // Show all locations (active and inactive) for management
        grid.setItems(locationService.getAllLocations(false));
    }
}

