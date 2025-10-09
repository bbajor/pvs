package de.bbajor.pvs.intravitreal.treatment.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
public class Treatment extends BasicEntity<Long> {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "treatment_plan_id", nullable = false)
    private TreatmentPlan treatmentPlan;
    private String sideOfEye;
    @ManyToOne(fetch = FetchType.EAGER)
    private SurgicalCenterTimeSlot surgicalCenterTimeSlot;
    private LocalDate approvalDate;

    @ManyToOne(fetch = FetchType.EAGER)
    private Medication medication;
    private String frequency;
    private String dosage;
    private String billId;
    private String additionalInfo;
}
