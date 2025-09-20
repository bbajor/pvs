package de.bbajor.pvs.base.ui.view;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.data.value.ValueChangeMode;

import de.bbajor.pvs.base.dto.AddressDto;
import de.bbajor.pvs.base.ui.component.CountrySelectionComboBox;

public class AddressField extends AbstractCompositeField<FormLayout, AddressField, AddressDto> {

    private final Binder<AddressDto> binder = new Binder<>(AddressDto.class);

    private final TextField streetField = new TextField("Straße");
    private final TextField houseNoField = new TextField("Hausnummer");
    private final NumberField zipCodeField = new NumberField("Postleitzahl");
    private final TextField cityField = new TextField("Stadt");
    private final CountrySelectionComboBox countryCodeBox = new CountrySelectionComboBox();

    public AddressField() {
        this("");
    }

    public AddressField(String label) {
        super(null);

        streetField.setWidthFull();
        houseNoField.setWidthFull();
        zipCodeField.setWidthFull();
        cityField.setWidthFull();

        // Eingabefelder auf "live" Änderungen stellen
        streetField.setValueChangeMode(ValueChangeMode.EAGER);
        houseNoField.setValueChangeMode(ValueChangeMode.EAGER);
        zipCodeField.setValueChangeMode(ValueChangeMode.EAGER);
        cityField.setValueChangeMode(ValueChangeMode.EAGER);

        // ---- Binding + Validation ----
        binder.forField(streetField)
                .asRequired("Straße darf nicht leer sein")
                .withValidator(new StringLengthValidator("Straße muss mind. 2 Zeichen haben", 2, null))
                .bind(AddressDto::getStreet, AddressDto::setStreet);

        binder.forField(houseNoField)
                .asRequired("Hausnummer darf nicht leer sein")
                .bind(AddressDto::getHouseNumber, AddressDto::setHouseNumber);

        binder.forField(zipCodeField)
                .asRequired("PLZ darf nicht leer sein")
                .withValidator(plz -> plz != null && plz >= 1000 && plz <= 99999,
                        "Bitte eine gültige Postleitzahl angeben")
                .bind(AddressDto::getPostalCode, AddressDto::setPostalCode);

        binder.forField(cityField)
                .asRequired("Stadt darf nicht leer sein")
                .bind(AddressDto::getCity, AddressDto::setCity);

        binder.forField(countryCodeBox)
                .asRequired("Land auswählen")
                .bind(AddressDto::getCountryCode, AddressDto::setCountryCode);

        // Änderungen weiterleiten
        binder.addValueChangeListener(e -> {
            if (binder.getBean() != null) {
                setModelValue(binder.getBean(), true);
            }
        });

        getContent().add(streetField, houseNoField, zipCodeField, cityField, countryCodeBox);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        binder.setReadOnly(!enabled);
        streetField.setEnabled(enabled);
        houseNoField.setEnabled(enabled);
        zipCodeField.setEnabled(enabled);
        cityField.setEnabled(enabled);
        countryCodeBox.setEnabled(enabled);
    }

    @Override
    public void setValue(AddressDto value) {
        if (value == null) {
            value = new AddressDto();
        }
        binder.setBean(value);
        super.setValue(value); // wichtig für AbstractCompositeField
    }

    @Override
    public AddressDto getValue() {
        return binder.getBean();
    }

    @Override
    protected void setPresentationValue(AddressDto newPresentationValue) {
        if (newPresentationValue == null) {
            newPresentationValue = new AddressDto();
        }
        binder.setBean(newPresentationValue);
    }

    /**
     * Führt eine Validierung der Eingabefelder durch.
     * 
     * @return true, wenn alle Eingaben gültig sind
     */
    public boolean validate() {
        return binder.validate().isOk();
    }

    /**
     * Versucht, die Daten aus den Eingaben ins DTO zu schreiben.
     * 
     * @throws ValidationException wenn Eingaben ungültig sind
     */
    public void writeIfValid() throws ValidationException {
        binder.writeBean(binder.getBean());
    }
}
