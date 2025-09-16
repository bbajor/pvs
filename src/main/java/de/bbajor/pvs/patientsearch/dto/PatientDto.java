package de.bbajor.pvs.patientsearch.dto;

import java.time.LocalDate;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PatientDto {

    private Integer patientId;
    private String salutation;
    private String title;
    private String firstName;
    private String lastName;
    private LocalDate birth;
    private PatientAddressDto PatientAddress;
    private String phone;
    private String email;
    private String healthInsurance;
    private String healthInsuranceNumber;
    private PatientHistoryDto patientHistory;

    public PatientAddressDto gPatientAddressDto() {
        if (PatientAddress == null) {
            PatientAddress = new PatientAddressDto();
        }
        return PatientAddress;
    }
}
