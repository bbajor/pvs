package de.bbajor.pvs.settings.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import lombok.Setter;

/**
 * Dialog for creating and editing locations (Standorte).
 * Aligned with the behaviour of {@link UserDialog}.
 */
public class LocationDialog extends Dialog {

    private final LocationService locationService;
    private final InstitutionRepository institutionRepository;
    private final Location location;

    @Setter
    private Runnable onSaveCallback;

    private TextField nameField;
    private TextField streetField;
    private TextField houseNumberField;
    private TextField postalCodeField;
    private TextField cityField;
    private TextField countryField;
    private TextField phoneField;
    private TextField emailField;
    private TextArea additionalInfoField;

    private Button saveButton;

    public LocationDialog(LocationService locationService, Location location) {
        this.locationService = locationService;
        this.institutionRepository = null;
        this.location = location != null ? location : new Location();

        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("600px");
        setCloseOnOutsideClick(false);

        Button closeIconButton = new Button("✕", e -> close());
        closeIconButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        closeIconButton.getStyle().set("margin-left", "auto");
        getHeader().add(closeIconButton);

        initializeDialog();
    }

    public LocationDialog(LocationService locationService,
                          InstitutionRepository institutionRepository,
                          Location location) {
        this.locationService = locationService;
        this.institutionRepository = institutionRepository;
        this.location = location != null ? location : new Location();

        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("600px");
        setCloseOnOutsideClick(false);

        Button closeIconButton = new Button("✕", e -> close());
        closeIconButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        closeIconButton.getStyle().set("margin-left", "auto");
        getHeader().add(closeIconButton);

        initializeDialog();
    }

    private void initializeDialog() {
        String titleText = location.getId() != null ? "Standort bearbeiten" : "Neuer Standort";
        H3 title = new H3(titleText);

        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        add(title, formLayout, buttonLayout);

        if (location.getId() != null) {
            loadLocationData();
        }
    }

    private FormLayout createFormLayout() {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        nameField = new TextField("Standort-Name");
        nameField.setRequired(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        streetField = new TextField("Straße");
        streetField.setWidthFull();

        houseNumberField = new TextField("Hausnummer");
        houseNumberField.setWidthFull();

        postalCodeField = new TextField("Postleitzahl");
        postalCodeField.setWidthFull();

        cityField = new TextField("Stadt");
        cityField.setWidthFull();

        countryField = new TextField("Land");
        countryField.setWidthFull();

        phoneField = new TextField("Telefon");
        phoneField.setWidthFull();

        emailField = new TextField("E-Mail");
        emailField.setWidthFull();

        additionalInfoField = new TextArea("Zusätzliche Informationen");
        additionalInfoField.setWidthFull();
        additionalInfoField.setHeight("120px");

        layout.add(nameField, 2);
        layout.add(streetField, houseNumberField);
        layout.add(postalCodeField, cityField);
        layout.add(countryField, 2);
        layout.add(phoneField, emailField);
        layout.add(additionalInfoField, 2);

        return layout;
    }

    private HorizontalLayout createButtonLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        saveButton = new Button("Speichern", event -> saveLocation());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(saveButton);
        return layout;
    }

    private void loadLocationData() {
        nameField.setValue(location.getLocationName() != null ? location.getLocationName() : "");
        streetField.setValue(location.getStreet() != null ? location.getStreet() : "");
        houseNumberField.setValue(location.getHouseNumber() != null ? location.getHouseNumber() : "");
        postalCodeField.setValue(location.getPostalCode() != null ? location.getPostalCode() : "");
        cityField.setValue(location.getCity() != null ? location.getCity() : "");
        countryField.setValue(location.getCountry() != null ? location.getCountry() : "");
        phoneField.setValue(location.getPhone() != null ? location.getPhone() : "");
        emailField.setValue(location.getEmail() != null ? location.getEmail() : "");
        additionalInfoField.setValue(location.getAdditionalInfo() != null ? location.getAdditionalInfo() : "");
    }

    private void saveLocation() {
        String name = nameField.getValue();
        if (name == null || name.trim().isEmpty()) {
            showError("Bitte geben Sie einen Standort-Namen ein");
            return;
        }

        try {
            location.setLocationName(name);
            location.setStreet(streetField.getValue());
            location.setHouseNumber(houseNumberField.getValue());
            location.setPostalCode(postalCodeField.getValue());
            location.setCity(cityField.getValue());
            location.setCountry(countryField.getValue());
            location.setPhone(phoneField.getValue());
            location.setEmail(emailField.getValue());
            location.setAdditionalInfo(additionalInfoField.getValue());

            // Für neue Standorte sicherstellen, dass Institution gesetzt ist
            if (location.getInstitution() == null && institutionRepository != null) {
                Long institutionId = InstitutionContext.getInstitutionId();
                if (institutionId != null) {
                    Institution institution = institutionRepository.findById(institutionId)
                            .orElse(null);
                    location.setInstitution(institution);
                }
            }

            if (location.getId() == null) {
                location.setActive(true);
            }

            locationService.saveLocation(location);

            showSuccess("Standort wurde erfolgreich gespeichert!");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            close();
        } catch (Exception e) {
            showError("Fehler beim Speichern: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}

