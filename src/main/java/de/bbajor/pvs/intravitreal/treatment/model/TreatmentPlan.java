package de.bbajor.pvs.intravitreal.treatment.model;

import java.time.LocalDate;
import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.dto.SideOfEye;
import de.bbajor.pvs.medication.model.IntravitrealMedication;
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
public class TreatmentPlan extends BasicEntity<Long> {

    private LocalDate creationDate;
    private String description;
    @ManyToOne
    private Patient patient;
    @ManyToOne
    private Diagnosis diagnosis;
    @OneToOne
    private ClinicalTrial clinicalTrial;
    private String additionalInformation;
    private String billId;
    private String sideOfEye;
    @ManyToOne
    private IntravitrealMedication drug;
    private String frequency;
    private String dosage;
    @OneToMany
    private List<TreatmentSlot> treatmentSlots;



}
