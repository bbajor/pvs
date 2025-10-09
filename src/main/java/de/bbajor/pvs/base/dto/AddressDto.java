package de.bbajor.pvs.base.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AddressDto {

    private Long id;
    private Long version;
    private String street;
    private String houseNo;
    private Integer postalCode;
    private String city;
    private String country;

    @Override
    public String toString() {
        return String.valueOf(street) + " " + String.valueOf(houseNo) + ", " + String.valueOf(postalCode) + " "
                + String.valueOf(city) + " " + String.valueOf(country != null ? country : "-");

    }

}
