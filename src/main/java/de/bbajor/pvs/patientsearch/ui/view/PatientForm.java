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

import de.bbajor.pvs.patientsearch.dto.HealthInsuranceDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.patientsearch.dto.TitleDto;

public class PatientForm extends Composite<FormLayout> {

    private Binder<PatientDto> binder = new Binder<>(PatientDto.class);

    TextField salutationField = new TextField("Anrede");
    ComboBox<TitleDto> titleField = new ComboBox<TitleDto>("Titel");

    TextField firstNameField = new TextField("Vorname");
    TextField lastNameField = new TextField("Nachname");
    DatePicker birthDateField = new DatePicker("Geburtsdatum");
    ComboBox<HealthInsuranceDto> healthInsuranceField = new ComboBox<HealthInsuranceDto>("Krankenversicherung");
    TextField healthInsuranceNumberField = new TextField("Versichertennummer");

    TextArea descriptionField = new TextArea("Zusätzliche Informationen");
    TextField streetField = new TextField("Straße");
    TextField houseNumberField = new TextField("Hausnummer");
    TextField postalCodeField = new TextField("Postleitzahl");
    TextField cityField = new TextField("Ort");
    TextField phoneField = new TextField("Telefonnummer");
    TextField emailField = new TextField("E-Mail");

    public PatientForm(List<HealthInsuranceDto> healthInsurances) {

        titleField.setItems(TitleDto.values());
        healthInsuranceField.setItems(CollectionUtils.isEmpty(healthInsurances) ? List.of() : healthInsurances);

        descriptionField.setWidthFull();
        descriptionField.setHeight("150px");

        // Configure the form
        var formLayout = getContent();
        formLayout.add(salutationField);
        formLayout.add(titleField);
        formLayout.add(firstNameField);
        formLayout.add(lastNameField);
        formLayout.add(birthDateField);
        formLayout.add(streetField);
        formLayout.add(houseNumberField);
        formLayout.add(postalCodeField);
        formLayout.add(cityField);
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
        binder.bind(streetField, dto -> dto.getPatientAddressDto().getStreet(),
                (dto, value) -> dto.getPatientAddressDto().setStreet(value));
        binder.bind(houseNumberField, dto -> dto.getPatientAddressDto().getHouseNumber(),
                (dto, value) -> dto.getPatientAddressDto().setHouseNumber(value));
        binder.bind(postalCodeField, dto -> dto.getPatientAddressDto().getPostalCode(),
                (dto, value) -> dto.getPatientAddressDto().setPostalCode(value));
        binder.bind(cityField, dto -> dto.getPatientAddressDto().getCity(),
                (dto, value) -> dto.getPatientAddressDto().setCity(value));
        binder.bind(salutationField, PatientDto::getSalutation, PatientDto::setSalutation);
        binder.bind(healthInsuranceNumberField, PatientDto::getInsuranceId,
                PatientDto::setInsuranceId);
        binder.bind(healthInsuranceField, PatientDto::getHealthInsurance, PatientDto::setHealthInsurance);
        binder.bind(titleField, PatientDto::getTitle, PatientDto::setTitle);
        binder.bind(descriptionField, PatientDto::getDescription, PatientDto::setDescription);
        // TODO: bind other fields

    }

    public void setPatient(PatientDto dto) {
        binder.setBean(dto);
    }

    public PatientDto getPatient() {
        return binder.getBean();
    }

}
