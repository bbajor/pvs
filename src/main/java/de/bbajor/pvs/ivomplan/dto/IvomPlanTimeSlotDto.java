package de.bbajor.pvs.ivomplan.dto;

import java.time.LocalDate;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IvomPlanTimeSlotDto {

    private Long id;
    private IvomPlanDto ivomPlan;
    private SurgeryUnitTimeSlotDto timeSlotSurgeryUnit;
    private LocalDate approvalDate;
    private String remarks;

}
