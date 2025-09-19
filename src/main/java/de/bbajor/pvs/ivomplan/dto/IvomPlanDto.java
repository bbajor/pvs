package de.bbajor.pvs.ivomplan.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String currentSideOfEye;
    private TreatmentDto treatment;

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

    public String getDiagnose() {
        return treatment != null && treatment.getDisease() != null ? treatment.getDisease().getName()
                : "Diagnose n.a.";
    }

    public String getCurrentDrug() {
        return treatment != null && treatment.getIvomDrug() != null
                ? treatment.getIvomDrug().getArzneimittelbezeichnung()
                : "Medikament n.b.";
    }
}
