package de.bbajor.pvs.ivom.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TreatmentDto {

    private Long id;
    private String treatmentName;
    private String description;
    private DiseaseDto disease;
    private IvomDrugDto ivomDrug;
    private String dosage;
    private SideOfEye sideOfEye;
    private TimeSlotDto timeSlot;

}
