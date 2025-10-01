package de.bbajor.pvs.patient.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class PatientRecord extends BasicEntity<Integer> {

    private LocalDate dateOfRecord;
    private String description;
    private boolean isActive;
    @ManyToOne
    private ReasonForVisit reasonForVisit;
    @OneToOne
    private Anamnesis patientAnamnesis;
}