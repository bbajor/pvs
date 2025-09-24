package de.bbajor.pvs.base.dto;

import java.util.Locale;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AddressDto {

    private Long id;
    private Long version;
    private String street;
    private String houseNumber;
    private Double postalCode;
    private String city;
    private Locale locale;

    private String country;
    private String language;

    @Override
    public String toString() {
        return String.valueOf(street) + " " + String.valueOf(houseNumber) + ", " + String.valueOf(postalCode) + " "
                + String.valueOf(city) + " " + String.valueOf(locale != null ? locale : "-");

    }

    public Locale getLocale() {
        if (language != null && country != null) {
            return Locale.of(language, country);
        }
        return null;
    }

}
