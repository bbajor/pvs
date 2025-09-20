package de.bbajor.pvs.ivomplan.dto;

import de.bbajor.pvs.ivomdrug.dto.IvomDrugDto;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TreatmentDto {

    private Long id;
    private String treatmentName;
    private String description;
    private IvomDiagnosisDto disease;
    private IvomDrugDto ivomDrug;
    private String dosage;
    private SideOfEye sideOfEye;
    private TimeSlotDto timeSlot;

}
