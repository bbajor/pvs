package de.bbajor.pvs.patientsearch.dto;

import lombok.Data;

@Data
public class PatientAddressDto {
    private String street;
    private String houseNumber;
    private String postalCode;
    private String city;
    private String country;
}
