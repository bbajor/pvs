package de.bbajor.pvs.intravitreal.treatment.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private DiagnosisDto diagnosis;
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

    public List<TreatmentDto> getTreatments() {
        if (treatments == null) {
            treatments = new ArrayList<>();
        }
        return treatments;
    }

}
