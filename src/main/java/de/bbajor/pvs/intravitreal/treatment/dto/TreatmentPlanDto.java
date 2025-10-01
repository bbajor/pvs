package de.bbajor.pvs.intravitreal.treatment.dto;

import java.time.LocalDate;
import java.util.List;

import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.medication.dto.MedicationDto;
import de.bbajor.pvs.patient.dto.PatientDto;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TreatmentPlanDto {

    private Long id;
    private Long version;
    private LocalDate creationDate;
    private String description;
    private PatientDto patient;
    private String additionalInformation;
    private String billId;
    private SideOfEye sideOfEye;
    private DiagnosisDto diagnosis;
    private MedicationDto drug;
    private String frequency;
    private String dosage;
    private List<TreatmentDto> treatments;

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
