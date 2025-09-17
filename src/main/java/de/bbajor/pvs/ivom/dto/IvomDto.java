package de.bbajor.pvs.ivom.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import de.bbajor.pvs.patientsearch.dto.PatientDto;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IvomDto {

    private Long id;
    private LocalDate creationDate;
    private LocalDateTime plannedDateOfProcedure;
    private SideOfEye sideOfEye;
    private PatientDto patient;
    private DiseaseDto diseases;
    private String additionalInformation;
    private String drugBillId;
    private String billId;
    private String diagnoseIvom;
    private String currentSideOfEye;
    private String currentDrug;

    public String getFirstName() {
        return patient != null ? patient.getFirstName() : "";
    }

    public String getLastName() {
        return patient != null ? patient.getLastName() : "";
    }

    public LocalDate getBirth() {
        return patient != null ? patient.getBirth() : null;
    }

    public String getHealthInsurance() {
        return patient != null && patient.getHealthInsurance() != null ? patient.getHealthInsurance().getCostCarrierName() : "Name n.a.";
    }
}
