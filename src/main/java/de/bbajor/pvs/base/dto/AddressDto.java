package de.bbajor.pvs.base.dto;

import java.util.Locale;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AddressDto {

    private long id;
    private String street;
    private String houseNumber;
    private Double postalCode;
    private String city;
    private Locale locale;

    @Override
    public String toString() {
        return String.valueOf(street) + " " + String.valueOf(houseNumber) + ", " + String.valueOf(postalCode) + " "
                + String.valueOf(city) + " " + String.valueOf(locale != null ? locale : "-");

    }

}
