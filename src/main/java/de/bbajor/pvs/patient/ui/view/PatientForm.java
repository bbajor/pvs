package de.bbajor.pvs.patient.ui.view;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

        private final TextArea descriptionField = new TextArea();
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

                // Persönliche Daten
                FormLayout personalDataLayout = new FormLayout();
                personalDataLayout.setWidthFull();
                personalDataLayout.setMinColumns(3);
                personalDataLayout.add(salutationComboBox);
                personalDataLayout.add(titleComboBox);
                personalDataLayout.add(firstNameField);
                personalDataLayout.add(lastNameField);
                personalDataLayout.add(birthDateField);
                // Location field only visible for authorized users
                if (canEditLocation) {
                    personalDataLayout.add(locationField);
                }
                AccordionPanel personalPanel = new AccordionPanel("Persönliche Daten", personalDataLayout);
                personalPanel.setOpened(true);
                personalPanel.setWidthFull();

                // Kontaktdaten
                FormLayout contactDataLayout = new FormLayout();
                contactDataLayout.setWidthFull();
                contactDataLayout.add(addressField,2);
                contactDataLayout.add(phoneField);
                contactDataLayout.add(emailField);
                AccordionPanel contactPanel = new AccordionPanel("Kontaktdaten", contactDataLayout);
                contactPanel.setOpened(true);
                contactPanel.setWidthFull();

                // Versicherungsdaten
                FormLayout insuranceDataLayout = new FormLayout();
                insuranceDataLayout.setWidthFull();
                insuranceDataLayout.add(healthInsuranceField);
                insuranceDataLayout.add(healthInsuranceNumberField);
                AccordionPanel insurancePanel = new AccordionPanel("Versicherungsdaten", insuranceDataLayout);
                insurancePanel.setOpened(true);
                insurancePanel.setWidthFull();

                // Zusätzliche Informationen
                FormLayout additionalInfoLayout = new FormLayout();
                additionalInfoLayout.setWidthFull();
                additionalInfoLayout.add(descriptionField, 2); // Über volle Breite
                AccordionPanel additionalPanel = new AccordionPanel("Zusätzliche Informationen", additionalInfoLayout);
                additionalPanel.setOpened(true);
                additionalPanel.setWidthFull();

                // Description Feld soll die verfügbare Höhe nutzen
                descriptionField.setMinHeight("150px");
                descriptionField.setMaxHeight("300px");

                // Hauptformular konfigurieren
                var formLayout = getContent();
                formLayout.setWidthFull();
                formLayout.add(new Accordion().add(personalPanel), 2);
                formLayout.add(new Accordion().add(contactPanel), 2);
                formLayout.add(new Accordion().add(insurancePanel), 2);
                formLayout.add(new Accordion().add(additionalPanel), 2);

                binder.forField(firstNameField).asRequired("Bitte geben Sie einen gültigen Vornamen ein")
                                .withValidator(item -> !item.trim().isEmpty() && item.trim().length() < 100,
                                                "Der Vorname muss zwischen 1 und 100 Zeichen enthalten")
                                .bind(Patient::getFirstName, Patient::setFirstName);
                binder.forField(lastNameField).asRequired("Bitte geben Sie einen gültigen Nachnamen ein")
                                .withValidator(item -> !item.trim().isEmpty() && item.trim().length() < 100,
                                                "Der Nachname muss zwischen 1 und 100 Zeichen enthalten")
                                .bind(Patient::getLastName, Patient::setLastName);
                binder.forField(birthDateField).asRequired("Bitte geben Sie ein gültiges Geburtsdatum ein")
                                .withValidator(item -> item != null && item.isBefore(java.time.LocalDate.now()),
                                                "Das Geburtsdatum muss in der Vergangenheit liegen")
                                .bind(Patient::getBirth, Patient::setBirth);
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
                binder.bind(healthInsuranceField, Patient::getHealthInsurance, Patient::setHealthInsurance);
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
