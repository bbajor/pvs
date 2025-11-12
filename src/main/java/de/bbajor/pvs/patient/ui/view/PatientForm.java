package de.bbajor.pvs.patient.ui.view;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.TextRenderer;

import de.bbajor.pvs.base.ui.component.AddressField;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.patient.dto.Salutation;
import de.bbajor.pvs.patient.dto.Title;
import de.bbajor.pvs.patient.model.Address;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.security.AppRoles;

public class PatientForm extends AbstractCompositeField<FormLayout, PatientForm, Patient> {

        private final Binder<Patient> binder = new Binder<>(Patient.class);

        private final ComboBox<Salutation> salutationComboBox = new ComboBox<>("Anrede");
        private final ComboBox<Title> titleComboBox = new ComboBox<Title>("Titel");

        private final TextField firstNameField = new TextField("Vorname");
        private final TextField lastNameField = new TextField("Nachname");
        private final DatePicker birthDateField = new DatePicker("Geburtsdatum");
        private final ComboBox<HealthInsurance> healthInsuranceField = new ComboBox<>("Krankenversicherung");
        private final TextField healthInsuranceNumberField = new TextField("Versichertennummer");

        private final TextArea descriptionField = new TextArea("Beschreibung");
        private final AddressField<Address> addressField = new AddressField<>(new Address());
        private final TextField phoneField = new TextField("Telefonnummer");
        private final TextField emailField = new TextField("E-Mail");
        private final ComboBox<Location> locationField = new ComboBox<>("Standort");

        public PatientForm(List<HealthInsurance> healthInsurances, Patient patient,
                        ValueChangeListener<? super ValueChangeEvent<?>> listener) {
                this(healthInsurances, patient, null, listener);
        }

        public PatientForm(List<HealthInsurance> healthInsurances, Patient patient,
                        List<Location> locations, ValueChangeListener<? super ValueChangeEvent<?>> listener) {
                super(patient);

                titleComboBox.setItems(Title.values());
                salutationComboBox.setItems(Salutation.values());
                healthInsuranceField.setItems(healthInsurances);

                // Location field: Only visible/editable for TECH_USER, ADMIN, OWNER
                boolean canEditLocation = canUserEditLocation();
                locationField.setVisible(canEditLocation);
                locationField.setEnabled(canEditLocation);
                if (locations != null && canEditLocation) {
                    locationField.setItems(locations);
                    locationField.setRenderer(new TextRenderer<>(Location::getLocationName));
                    locationField.setItemLabelGenerator(loc -> loc.getLocationName() != null ? loc.getLocationName() : "");
                }

                descriptionField.setWidthFull();
                descriptionField.setHeight("150px");
                descriptionField.setMinHeight("150px");
                descriptionField.setMaxHeight("300px");

                // Hauptformular konfigurieren - Sections in zwei Spalten
                var formLayout = getContent();
                formLayout.setWidthFull();
                formLayout.setMinColumns(2);
                
                // Section für Persönliche Daten
                com.vaadin.flow.component.html.Div personalSection = createSection("Persönliche Daten");
                FormLayout personalDataLayout = new FormLayout();
                personalDataLayout.setWidthFull();
                personalDataLayout.setMinColumns(1);
                personalDataLayout.add(salutationComboBox);
                personalDataLayout.add(titleComboBox);
                personalDataLayout.add(firstNameField);
                personalDataLayout.add(lastNameField);
                personalDataLayout.add(birthDateField);
                // Location field only visible for authorized users
                if (canEditLocation) {
                    personalDataLayout.add(locationField);
                }
                personalSection.add(personalDataLayout);
                formLayout.add(personalSection);
                
                // Section für Kontaktdaten
                com.vaadin.flow.component.html.Div contactSection = createSection("Kontaktdaten");
                FormLayout contactDataLayout = new FormLayout();
                contactDataLayout.setWidthFull();
                contactDataLayout.setMinColumns(1);
                contactDataLayout.add(addressField);
                contactDataLayout.add(phoneField);
                contactDataLayout.add(emailField);
                contactSection.add(contactDataLayout);
                formLayout.add(contactSection);
                
                // Section für Versicherungsdaten
                com.vaadin.flow.component.html.Div insuranceSection = createSection("Versicherungsdaten");
                FormLayout insuranceDataLayout = new FormLayout();
                insuranceDataLayout.setWidthFull();
                insuranceDataLayout.setMinColumns(1);
                insuranceDataLayout.add(healthInsuranceField);
                insuranceDataLayout.add(healthInsuranceNumberField);
                insuranceSection.add(insuranceDataLayout);
                formLayout.add(insuranceSection);
                
                // Section für Zusätzliche Informationen
                com.vaadin.flow.component.html.Div additionalSection = createSection("Zusätzliche Informationen");
                FormLayout additionalInfoLayout = new FormLayout();
                additionalInfoLayout.setWidthFull();
                additionalInfoLayout.setMinColumns(1);
                additionalInfoLayout.add(descriptionField);
                additionalSection.add(additionalInfoLayout);
                formLayout.add(additionalSection);

                // Pflichtfelder markieren
                firstNameField.setRequired(true);
                firstNameField.setRequiredIndicatorVisible(true);
                binder.forField(firstNameField).asRequired("Bitte geben Sie einen gültigen Vornamen ein")
                                .withValidator(item -> !item.trim().isEmpty() && item.trim().length() < 100,
                                                "Der Vorname muss zwischen 1 und 100 Zeichen enthalten")
                                .bind(Patient::getFirstName, Patient::setFirstName);
                
                lastNameField.setRequired(true);
                lastNameField.setRequiredIndicatorVisible(true);
                binder.forField(lastNameField).asRequired("Bitte geben Sie einen gültigen Nachnamen ein")
                                .withValidator(item -> !item.trim().isEmpty() && item.trim().length() < 100,
                                                "Der Nachname muss zwischen 1 und 100 Zeichen enthalten")
                                .bind(Patient::getLastName, Patient::setLastName);
                
                birthDateField.setRequired(true);
                birthDateField.setRequiredIndicatorVisible(true);
                binder.forField(birthDateField).asRequired("Bitte geben Sie ein gültiges Geburtsdatum ein")
                                .withValidator(item -> item != null && item.isBefore(java.time.LocalDate.now()),
                                                "Das Geburtsdatum muss in der Vergangenheit liegen")
                                .bind(Patient::getBirth, Patient::setBirth);
                
                healthInsuranceField.setRequired(true);
                healthInsuranceField.setRequiredIndicatorVisible(true);
                binder.forField(phoneField).withValidator(item -> item.isEmpty() || item.trim().length() < 30,
                                "Die Telefonnummer darf maximal 30 Zeichen enthalten")
                                .bind(Patient::getPhone, Patient::setPhone);
                binder.forField(emailField).withValidator(item -> item.isEmpty() || item.contains("@"),
                                "Bitte eine gültige E-Mail-Adresse eingeben")
                                .bind(Patient::getEmail, Patient::setEmail);
                binder.bind(salutationComboBox, Patient::getSalutation, Patient::setSalutation);
                binder.forField(healthInsuranceNumberField)
                                .withValidator(item -> item != null, "Die Versichertennummer darf nicht leer sein")
                                .withValidator(item -> item.isEmpty() || item.trim().length() < 30,
                                                "Die Versichertennummer darf maximal 30 Zeichen enthalten")
                                .bind(Patient::getInsuranceNumber,
                                                Patient::setInsuranceNumber);
                binder.forField(healthInsuranceField)
                                .asRequired("Bitte wählen Sie eine Krankenversicherung aus")
                                .bind(Patient::getHealthInsurance, Patient::setHealthInsurance);
                binder.bind(titleComboBox, Patient::getTitle, Patient::setTitle);
                binder.forField(descriptionField).withValidator(item -> item.isEmpty() || item.trim().length() < 2000,
                                "Die Beschreibung darf maximal 2000 Zeichen enthalten")
                                .bind(Patient::getDescription, Patient::setDescription);
                binder.forField(addressField).bind(Patient::getAddress, Patient::setAddress);
                // Location binding only if user can edit
                if (canEditLocation) {
                    binder.bind(locationField, Patient::getLocation, Patient::setLocation);
                }
                binder.addValueChangeListener(listener);
                setValue(patient);
        }
        
        /**
         * Erstellt eine optisch getrennte Section mit Titel.
         */
        private com.vaadin.flow.component.html.Div createSection(String title) {
            com.vaadin.flow.component.html.Div section = new com.vaadin.flow.component.html.Div();
            section.addClassName("dialog-section");
            section.setWidthFull();
            section.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
            section.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            section.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
            section.getStyle().set("padding", "var(--lumo-space-m)");
            section.getStyle().set("box-sizing", "border-box");
            section.getStyle().set("margin-bottom", "var(--lumo-space-m)");
            
            com.vaadin.flow.component.html.H4 sectionTitle = new com.vaadin.flow.component.html.H4(title);
            sectionTitle.getStyle().set("margin-top", "0");
            sectionTitle.getStyle().set("margin-bottom", "var(--lumo-space-s)");
            sectionTitle.getStyle().set("color", "var(--lumo-primary-text-color)");
            sectionTitle.getStyle().set("font-size", "var(--lumo-font-size-m)");
            sectionTitle.getStyle().set("font-weight", "600");
            section.add(sectionTitle);
            
            return section;
        }

        /**
         * Checks if the current user has permission to edit the location field.
         * Only TECH_USER, ADMIN, and OWNER can edit the location.
         */
        private boolean canUserEditLocation() {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getAuthorities() != null) {
                    return auth.getAuthorities().stream()
                            .anyMatch(a -> {
                                String authority = a.getAuthority();
                                return authority.equals("ROLE_" + AppRoles.TECH_USER) ||
                                       authority.equals("ROLE_" + AppRoles.ADMIN) ||
                                       authority.equals("ROLE_" + AppRoles.OWNER);
                            });
                }
            } catch (Exception e) {
                // If we can't determine the user's role, don't show the field
            }
            return false;
        }

        @Override
        public void setValue(Patient value) {
                binder.setBean(value);
                super.setValue(value);
        }

        @Override
        public Patient getValue() {
                return binder.getBean();
        }

        @Override
        protected void setPresentationValue(Patient newPresentationValue) {
                binder.setBean(newPresentationValue);
        }

        public boolean isValidateOk() {
                return binder.validate().isOk();
        }

        public void writeIfValid() throws ValidationException {
                binder.writeBean(binder.getBean());
        }
}
