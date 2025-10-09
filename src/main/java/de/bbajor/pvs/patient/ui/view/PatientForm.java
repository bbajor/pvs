package de.bbajor.pvs.patient.ui.view;

import java.util.List;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import de.bbajor.pvs.base.dto.AddressDto;
import de.bbajor.pvs.base.ui.component.AddressField;
import de.bbajor.pvs.patient.dto.HealthInsuranceDto;
import de.bbajor.pvs.patient.dto.PatientDto;
import de.bbajor.pvs.patient.dto.Salutation;
import de.bbajor.pvs.patient.dto.Title;

public class PatientForm extends AbstractCompositeField<FormLayout, PatientForm, PatientDto> {

        private final Binder<PatientDto> binder = new Binder<>(PatientDto.class);

        private final ComboBox<Salutation> salutationComboBox = new ComboBox<>("Anrede");
        private final ComboBox<Title> titleComboBox = new ComboBox<Title>("Titel");

        private final TextField firstNameField = new TextField("Vorname");
        private final TextField lastNameField = new TextField("Nachname");
        private final DatePicker birthDateField = new DatePicker("Geburtsdatum");
        private final ComboBox<HealthInsuranceDto> healthInsuranceField = new ComboBox<>("Krankenversicherung");
        private final TextField healthInsuranceNumberField = new TextField("Versichertennummer");

        private final TextArea descriptionField = new TextArea("Zusätzliche Informationen");
        private final AddressField<AddressDto> addressField = new AddressField<>(new AddressDto());
        private final TextField phoneField = new TextField("Telefonnummer");
        private final TextField emailField = new TextField("E-Mail");

        public PatientForm(List<HealthInsuranceDto> healthInsurances, PatientDto patientDto,
                        ValueChangeListener<? super ValueChangeEvent<?>> listener) {
                super(patientDto);

                titleComboBox.setItems(Title.values());
                salutationComboBox.setItems(Salutation.values());
                healthInsuranceField.setItems(healthInsurances);

                descriptionField.setWidthFull();
                descriptionField.setHeight("150px");

                // Configure the form
                var formLayout = getContent();

                formLayout.add(salutationComboBox);
                formLayout.add(titleComboBox);
                formLayout.add(firstNameField);
                formLayout.add(lastNameField);
                formLayout.add(birthDateField);
                formLayout.add(addressField);
                formLayout.add(phoneField);
                formLayout.add(emailField);
                formLayout.add(healthInsuranceField);
                formLayout.add(healthInsuranceNumberField);
                formLayout.add(descriptionField, 2);

                formLayout.setResponsiveSteps(
                                // Use one column by default
                                new ResponsiveStep("0", 1),
                                // Use two columns, if the layout's width exceeds 320px
                                new ResponsiveStep("320px", 2),
                                // Use three columns, if the layout's width exceeds 500px
                                new ResponsiveStep("500px", 3));

                binder.forField(firstNameField).asRequired("Bitte geben Sie einen gültigen Vornamen ein")
                                .withValidator(item -> !item.trim().isEmpty() && item.trim().length() < 100,
                                                "Der Vorname muss zwischen 1 und 100 Zeichen enthalten")
                                .bind(PatientDto::getFirstName, PatientDto::setFirstName);
                binder.forField(lastNameField).asRequired("Bitte geben Sie einen gültigen Nachnamen ein")
                                .withValidator(item -> !item.trim().isEmpty() && item.trim().length() < 100,
                                                "Der Nachname muss zwischen 1 und 100 Zeichen enthalten")
                                .bind(PatientDto::getLastName, PatientDto::setLastName);
                binder.forField(birthDateField).asRequired("Bitte geben Sie ein gültiges Geburtsdatum ein")
                                .withValidator(item -> item != null && item.isBefore(java.time.LocalDate.now()),
                                                "Das Geburtsdatum muss in der Vergangenheit liegen")
                                .bind(PatientDto::getBirth, PatientDto::setBirth);
                binder.forField(phoneField).withValidator(item -> item.isEmpty() || item.trim().length() < 30,
                                "Die Telefonnummer darf maximal 30 Zeichen enthalten")
                                .bind(PatientDto::getPhone, PatientDto::setPhone);
                binder.forField(emailField).withValidator(item -> item.isEmpty() || item.contains("@"),
                                "Bitte eine gültige E-Mail-Adresse eingeben")
                                .bind(PatientDto::getEmail, PatientDto::setEmail);
                binder.bind(salutationComboBox, PatientDto::getSalutation, PatientDto::setSalutation);
                binder.forField(healthInsuranceNumberField)
                                .withValidator(item -> item != null, "Die Versichertennummer darf nicht leer sein")
                                .withValidator(item -> item.isEmpty() || item.trim().length() < 30,
                                                "Die Versichertennummer darf maximal 30 Zeichen enthalten")
                                .bind(PatientDto::getInsuranceNumber,
                                                PatientDto::setInsuranceNumber);
                binder.bind(healthInsuranceField, PatientDto::getHealthInsurance, PatientDto::setHealthInsurance);
                binder.bind(titleComboBox, PatientDto::getTitle, PatientDto::setTitle);
                binder.forField(descriptionField).withValidator(item -> item.isEmpty() || item.trim().length() < 2000,
                                "Die Beschreibung darf maximal 2000 Zeichen enthalten")
                                .bind(PatientDto::getDescription, PatientDto::setDescription);
                binder.forField(addressField).bind(PatientDto::getAddress, PatientDto::setAddress);
                binder.addValueChangeListener(listener);
                setValue(patientDto);
        }

        @Override
        public void setValue(PatientDto value) {
                binder.setBean(value);
                super.setValue(value);
        }

        @Override
        public PatientDto getValue() {
                return binder.getBean();
        }

        @Override
        protected void setPresentationValue(PatientDto newPresentationValue) {
                binder.setBean(newPresentationValue);
        }

        public boolean isValidateOk() {
                return binder.validate().isOk();
        }

        public void writeIfValid() throws ValidationException {
                binder.writeBean(binder.getBean());
        }
}
