package de.bbajor.pvs.patientsearch.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PatientAddressDto {
    private String street;
    private String houseNumber;
    private String postalCode;
    private String city;
    private String countryCode;
}
