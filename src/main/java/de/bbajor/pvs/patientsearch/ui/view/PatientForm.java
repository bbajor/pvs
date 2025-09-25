package de.bbajor.pvs.patientsearch.ui.view;

import java.util.List;

import org.springframework.util.CollectionUtils;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;

import de.bbajor.pvs.base.dto.AddressDto;
import de.bbajor.pvs.base.ui.view.AddressField;
import de.bbajor.pvs.patientsearch.dto.HealthInsuranceDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.patientsearch.dto.SalutationDto;
import de.bbajor.pvs.patientsearch.dto.TitleDto;

public class PatientForm extends Composite<FormLayout> {

        private final Binder<PatientDto> binder = new Binder<>(PatientDto.class);

        private final ComboBox<SalutationDto> salutationComboBox = new ComboBox<>("Anrede");
        private final ComboBox<TitleDto> titleComboBox = new ComboBox<TitleDto>("Titel");

        private final TextField firstNameField = new TextField("Vorname");
        private final TextField lastNameField = new TextField("Nachname");
        private final DatePicker birthDateField = new DatePicker("Geburtsdatum");
        private final ComboBox<HealthInsuranceDto> healthInsuranceField = new ComboBox<>("Krankenversicherung");
        private final TextField healthInsuranceNumberField = new TextField("Versichertennummer");

        private final TextArea descriptionField = new TextArea("Zusätzliche Informationen");
        private final AddressField<AddressDto> addressField = new AddressField<>(new AddressDto());
        TextField phoneField = new TextField("Telefonnummer");
        TextField emailField = new TextField("E-Mail");

        public PatientForm(List<HealthInsuranceDto> healthInsurances) {

                titleComboBox.setItems(TitleDto.values());
                salutationComboBox.setItems(SalutationDto.values());
                healthInsuranceField.setItems(CollectionUtils.isEmpty(healthInsurances) ? List.of() : healthInsurances);

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
                binder.setBean(new PatientDto());
        }

        public PatientDto getPatient() {
                return binder.getBean();
        }

        public void writeValuesToPatient(PatientDto patient) {
                binder.setBean(patient);
        }

        public BinderValidationStatus<PatientDto> validate() {
                return binder.validate();
        }
}
