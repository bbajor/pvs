package de.bbajor.pvs.base.ui.view;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import de.bbajor.pvs.base.dto.AddressDto;
import de.bbajor.pvs.base.ui.component.CountrySelectionComboBox;

public class AddressField extends AbstractCompositeField<FormLayout, AddressField, AddressDto> {

    private final Binder<AddressDto> binder = new Binder<>(AddressDto.class);

    private final TextField streetField = new TextField("Straße");
    private final TextField houseNoField = new TextField("Hausnummer");
    private final NumberField zipCodeField = new NumberField("Postleitzahl");
    private final TextField cityField = new TextField("Stadt");
    private final CountrySelectionComboBox countryCodeBox = new CountrySelectionComboBox();

    private final AddressDto address = new AddressDto();

    public AddressField() {
        this("");
    }

    public AddressField(String label) {
        super(null);

        streetField.setWidthFull();
        houseNoField.setWidthFull();
        zipCodeField.setWidthFull();
        cityField.setWidthFull();

        binder.setBean(address);
        binder.forField(streetField).bind(AddressDto::getStreet, AddressDto::setStreet);
        binder.forField(houseNoField).bind(AddressDto::getHouseNumber, AddressDto::setHouseNumber);
        binder.forField(zipCodeField).bind(AddressDto::getPostalCode, AddressDto::setPostalCode);

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
        super.setValue(value);
        binder.setBean(value);
    }

    @Override
    protected void setPresentationValue(AddressDto newPresentationValue) {
        streetField.setValue(newPresentationValue.getStreet());
        houseNoField.setValue(newPresentationValue.getHouseNumber());
        zipCodeField.setValue(newPresentationValue.getPostalCode());
        cityField.setValue(newPresentationValue.getCity());
        countryCodeBox.setValue(newPresentationValue.getCountryCode());
    }

}