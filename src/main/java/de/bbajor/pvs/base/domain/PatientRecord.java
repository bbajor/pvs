package de.bbajor.pvs.base.domain;

import java.sql.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class PatientRecord extends BasicEntity<Integer> {

    private Date dateOfRecord;
    private String description;
    private boolean isActive;
    @ManyToOne
    private ReasonForVisit reasonForVisit;
    @OneToOne
    private Anamnesis patientAnamnesis;
}