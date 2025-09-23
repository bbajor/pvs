package de.bbajor.pvs.ivomplan.model;

import java.time.LocalDate;
import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.ivomdrug.model.IvomDrug;
import de.bbajor.pvs.ivomplan.dto.SideOfEye;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class IvomPlan extends BasicEntity<Long> {

    private LocalDate creationDate;
    private String description;
    @ManyToOne
    private Patient patient;
    @ManyToOne
    private IvomDiagnosis diagnosis;
    @OneToOne
    private IvomClinicalTrial clinicalTrial;
    private String additionalInformation;
    private String billId;
    private SideOfEye sideOfEye;
    @ManyToOne
    private IvomDrug drug;
    private String frequency;
    private String dosage;
    @OneToMany
    private List<IvomPlanTimeSlot> timeSlotsPatient;



}
