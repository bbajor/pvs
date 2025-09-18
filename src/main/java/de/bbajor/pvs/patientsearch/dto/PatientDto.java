package de.bbajor.pvs.patientsearch.dto;

import java.time.LocalDate;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PatientDto {

    private Integer patientId;
    private String salutation;
    private TitleDto title;
    private String gender;
    private String firstName;
    private String lastName;
    private LocalDate birth;
    private PatientAddressDto patientAddress;
    private String phone;
    private String email;
    private HealthInsuranceDto healthInsurance;
    private String insuranceId;
    private String description;
    private PatientHistoryDto patientHistory;

    public PatientAddressDto getPatientAddressDto() {
        if (patientAddress == null) {
            patientAddress = new PatientAddressDto();
        }
        return patientAddress;
    }

    public String toString() {
        return lastName + ", " + firstName + ", " + birth + ", " + patientAddress.toString();
    }
}
