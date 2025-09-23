package de.bbajor.pvs.ivomplan.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
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

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private IvomPlan ivomPlan;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private SurgeryUnitTimeSlot timeSlotSurgeryUnit;
    private LocalDate approvalDate;
    private String remarks;
}
