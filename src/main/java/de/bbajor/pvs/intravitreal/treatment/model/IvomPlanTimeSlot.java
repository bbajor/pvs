package de.bbajor.pvs.intravitreal.treatment.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class IvomPlanTimeSlot extends BasicEntity<Long> {

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private IvomPlan ivomPlan;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SurgicalCenterTimeSlot surgicalCenterTimeSlot;
    private LocalDate approvalDate;
    private String remarks;
}
