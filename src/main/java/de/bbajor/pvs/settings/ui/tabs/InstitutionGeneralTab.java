package de.bbajor.pvs.settings.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Tab for editing general institution data (name, address, contact information).
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class InstitutionGeneralTab extends VerticalLayout {

    private final InstitutionRepository institutionRepository;

    private TextField institutionNameField;
    private TextField companyNameField;
    private TextField taxIdField;
    private TextField streetField;
    private TextField houseNumberField;
    private TextField postalCodeField;
    private TextField cityField;
    private TextField countryField;
    private TextField phoneField;
    private TextField faxField;
    private EmailField emailField;
    private TextArea descriptionField;
    private Button saveButton;

    @PostConstruct
    private void init() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        // Check if InstitutionContext is set
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            H3 errorTitle = new H3("Institutions-Daten");
            add(errorTitle);
            Notification.show("Keine Institution ausgewählt. Bitte melden Sie sich mit einer Institution an.",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalStateException("Institution not found: " + institutionId));

        H3 title = new H3("Institutions-Daten");

        // Institution name and company information
        institutionNameField = new TextField("Institutions-Name");
        institutionNameField.setRequired(true);
        institutionNameField.setValue(institution.getInstitutionName() != null ? institution.getInstitutionName() : "");
        institutionNameField.setWidthFull();

        companyNameField = new TextField("Firmenname");
        companyNameField.setValue(institution.getCompanyName() != null ? institution.getCompanyName() : "");
        companyNameField.setWidthFull();

        taxIdField = new TextField("Steuernummer / Handelsregisternummer");
        taxIdField.setValue(institution.getTaxId() != null ? institution.getTaxId() : "");
        taxIdField.setWidthFull();

        // Address fields
        streetField = new TextField("Straße");
        streetField.setValue(institution.getStreet() != null ? institution.getStreet() : "");
        streetField.setWidthFull();

        houseNumberField = new TextField("Hausnummer");
        houseNumberField.setValue(institution.getHouseNumber() != null ? institution.getHouseNumber() : "");
        houseNumberField.setWidthFull();

        postalCodeField = new TextField("Postleitzahl");
        postalCodeField.setValue(institution.getPostalCode() != null ? institution.getPostalCode() : "");
        postalCodeField.setWidthFull();

        cityField = new TextField("Stadt");
        cityField.setValue(institution.getCity() != null ? institution.getCity() : "");
        cityField.setWidthFull();

        countryField = new TextField("Land");
        countryField.setValue(institution.getCountry() != null ? institution.getCountry() : "");
        countryField.setWidthFull();

        // Contact fields
        phoneField = new TextField("Telefon");
        phoneField.setValue(institution.getPhone() != null ? institution.getPhone() : "");
        phoneField.setWidthFull();

        faxField = new TextField("Fax");
        faxField.setValue(institution.getFax() != null ? institution.getFax() : "");
        faxField.setWidthFull();

        emailField = new EmailField("E-Mail");
        emailField.setValue(institution.getEmail() != null ? institution.getEmail() : "");
        emailField.setWidthFull();

        descriptionField = new TextArea("Beschreibung");
        descriptionField.setValue(institution.getDescription() != null ? institution.getDescription() : "");
        descriptionField.setWidthFull();
        descriptionField.setHeight("100px");

        saveButton = new Button("Speichern", e -> saveInstitution(institution));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        FormLayout formLayout = new FormLayout();
        formLayout.add(institutionNameField, companyNameField, taxIdField,
                streetField, houseNumberField, postalCodeField, cityField, countryField,
                phoneField, faxField, emailField, descriptionField, saveButton);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        add(title, formLayout);
    }

    private void saveInstitution(Institution institution) {
        if (institutionNameField.getValue() == null || institutionNameField.getValue().trim().isEmpty()) {
            Notification.show("Bitte geben Sie einen Institutions-Namen ein", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            institution.setInstitutionName(institutionNameField.getValue());
            institution.setCompanyName(companyNameField.getValue());
            institution.setTaxId(taxIdField.getValue());
            institution.setStreet(streetField.getValue());
            institution.setHouseNumber(houseNumberField.getValue());
            institution.setPostalCode(postalCodeField.getValue());
            institution.setCity(cityField.getValue());
            institution.setCountry(countryField.getValue());
            institution.setPhone(phoneField.getValue());
            institution.setFax(faxField.getValue());
            institution.setEmail(emailField.getValue());
            institution.setDescription(descriptionField.getValue());

            institutionRepository.save(institution);

            Notification.show("Institutions-Daten wurden erfolgreich gespeichert!", 3000,
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            log.error("Error saving institution: {}", e.getMessage(), e);
            Notification.show("Fehler beim Speichern: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}

