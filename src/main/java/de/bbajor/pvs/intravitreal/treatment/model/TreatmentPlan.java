package de.bbajor.pvs.intravitreal.treatment.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.medication.model.IntravitrealMedication;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Patient patient;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Diagnosis diagnosis;
    @OneToOne
    private ClinicalTrial clinicalTrial;
    private String additionalInformation;
    private String billId;
    private String sideOfEye;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private IntravitrealMedication drug;
    private String frequency;
    private String dosage;
    @OneToMany(mappedBy = "treatmentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Treatment> treatments = new ArrayList<>();



}
