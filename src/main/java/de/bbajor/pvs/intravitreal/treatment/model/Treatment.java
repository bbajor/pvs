package de.bbajor.pvs.intravitreal.treatment.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class Treatment extends BasicEntity<Long> {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "treatment_plan_id")
    private TreatmentPlan treatmentPlan;
    private String sideOfEye;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private SurgicalCenterTimeSlot surgicalCenterTimeSlot;
    private LocalDate approvalDate;
    private String remarks;
}
