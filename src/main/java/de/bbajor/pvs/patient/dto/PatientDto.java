package de.bbajor.pvs.patient.dto;

import java.time.LocalDate;

import de.bbajor.pvs.base.dto.AddressDto;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PatientDto {

    private Integer id;
    private Long version;
    private Salutation salutation;
    private Title title;
    private String gender;
    private String firstName;
    private String lastName;
    private LocalDate birth;
    private AddressDto address;
    private String phone;
    private String email;
    private HealthInsuranceDto healthInsurance;
    private String insuranceNumber;
    private String description;
    private PatientHistoryDto patientHistory;

    public String toString() {
        return lastName + ", " + firstName + ", " + birth + ", " + address.toString();
    }
}
