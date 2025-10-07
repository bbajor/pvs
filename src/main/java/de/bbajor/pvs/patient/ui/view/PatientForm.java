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

        public PatientForm(List<HealthInsuranceDto> healthInsurances, PatientDto patientDto) {
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

                binder.bind(firstNameField, PatientDto::getFirstName, PatientDto::setFirstName);
                binder.bind(lastNameField, PatientDto::getLastName, PatientDto::setLastName);
                binder.bind(birthDateField, PatientDto::getBirth, PatientDto::setBirth);
                binder.bind(phoneField, PatientDto::getPhone, PatientDto::setPhone);
                binder.bind(emailField, PatientDto::getEmail, PatientDto::setEmail);
                binder.bind(salutationComboBox, PatientDto::getSalutation, PatientDto::setSalutation);
                binder.bind(healthInsuranceNumberField, PatientDto::getInsuranceId,
                                PatientDto::setInsuranceId);
                binder.bind(healthInsuranceField, PatientDto::getHealthInsurance, PatientDto::setHealthInsurance);
                binder.bind(titleComboBox, PatientDto::getTitle, PatientDto::setTitle);
                binder.bind(descriptionField, PatientDto::getDescription, PatientDto::setDescription);
                binder.bind(addressField, PatientDto::getAddress, PatientDto::setAddress);
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
