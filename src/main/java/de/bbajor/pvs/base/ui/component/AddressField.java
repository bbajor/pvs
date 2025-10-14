package de.bbajor.pvs.base.ui.component;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.data.value.ValueChangeMode;

import de.bbajor.pvs.patient.model.Address;

public class AddressField<T extends Address> extends AbstractCompositeField<FormLayout, AddressField<T>, T> {

    private final Binder<T> binder = new Binder<>();
    private final TextField streetField = new TextField("Straße");
    private final TextField houseNoField = new TextField("Hausnummer");
    private final IntegerField zipCodeField = new IntegerField("Postleitzahl");
    private final TextField cityField = new TextField("Stadt");
    private final CountrySelectionComboBox countryCodeBox = new CountrySelectionComboBox();

    public AddressField(T value) {
        this("", value);
    }

    public AddressField(String label, T value) {
        super(value);

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
                .bind(Address::getStreet, Address::setStreet);

        binder.forField(houseNoField)
                .asRequired("Hausnummer darf nicht leer sein")
                .bind(Address::getHouseNo, Address::setHouseNo);

        binder.forField(zipCodeField)
                .asRequired("PLZ darf nicht leer sein")
                .withValidator(plz -> plz != null && plz >= 1000 && plz <= 99999,
                        "Bitte eine gültige Postleitzahl angeben")
                .bind(Address::getPostalCode, Address::setPostalCode);

        binder.forField(cityField)
                .asRequired("Stadt darf nicht leer sein")
                .bind(Address::getCity, Address::setCity);

        binder.forField(countryCodeBox)
                .asRequired("Land auswählen").bind(Address::getCountry, Address::setCountry);

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
        streetField.setEnabled(enabled);
        houseNoField.setEnabled(enabled);
        zipCodeField.setEnabled(enabled);
        cityField.setEnabled(enabled);
        countryCodeBox.setEnabled(enabled);
        binder.setReadOnly(!enabled);
    }

    @Override
    public void setValue(T value) {
        binder.setBean(value);
        super.setValue(value); // wichtig für AbstractCompositeField
    }

    @Override
    public T getValue() {
        return binder.getBean();
    }

    @Override
    protected void setPresentationValue(T newPresentationValue) {
        binder.setBean(newPresentationValue);
    }

    /**
     * Führt eine Validierung der Eingabefelder durch.
     * 
     * @return true, wenn alle Eingaben gültig sind
     */
    public BinderValidationStatus<T> validate() {
        return binder.validate();
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
