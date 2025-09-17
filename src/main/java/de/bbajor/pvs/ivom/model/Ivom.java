package de.bbajor.pvs.ivom.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.base.domain.Patient;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class Ivom extends BasicEntity<Long> {

    private LocalDate creationDate;
    private String description;
    @ManyToOne
    private Patient patient;
    @ManyToOne
    private IvomDiagnose diagnose;
    @OneToOne
    private IvomClinicalTrial clinicalTrial;




}
