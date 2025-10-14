package de.bbajor.pvs.patient.model;

import java.util.Locale;

import de.bbajor.pvs.base.util.LocaleConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Embeddable
@Accessors(chain = true)
public class Address {

    private String street;
    private String houseNo;
    private Integer postalCode;
    private String city;
    @Convert(converter = LocaleConverter.class)
    private Locale country;

    @Override
    public String toString() {
        return (street != null ? street : "") + (houseNo != null && !houseNo.isBlank() ? " " + houseNo : "") + ", "
                + (postalCode != null ? postalCode : "") + (city != null && !city.isBlank() ? " " + city : "")
                + (country != null ? " (" + country.getDisplayCountry() + ")" : "");
    }
}
