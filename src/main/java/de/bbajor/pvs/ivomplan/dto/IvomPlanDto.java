package de.bbajor.pvs.ivomplan.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import de.bbajor.pvs.ivomdrug.dto.IvomDrugDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IvomPlanDto {

    private Long id;
    private LocalDate creationDate;
    private LocalDateTime plannedDateOfProcedure;
    private PatientDto patient;
    private String additionalInformation;
    private String billId;
    private SideOfEye sideOfEye;
    private IvomDiagnosisDto diagnosis;
    private IvomDrugDto drug;
    private String frequency;
    private String dosage;
    private List<IvomPlanTimeSlotDto> timeSlot;

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
        return patient != null && patient.getHealthInsurance() != null
                ? patient.getHealthInsurance().getCostCarrierName()
                : "Name n.a.";
    }

}
