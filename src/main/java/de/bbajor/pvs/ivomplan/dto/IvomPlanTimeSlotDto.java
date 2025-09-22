package de.bbajor.pvs.ivomplan.dto;

import de.bbajor.pvs.ivomplan.model.IvomPlan;
import jakarta.persistence.ManyToOne;

public class IvomPlanTimeSlotDto {

    private Long id;
    @ManyToOne
    private IvomPlan ivomPlan;
    @ManyToOne
    private SurgeryUnitTimeSlotDto timeSlot;

}
