package de.bbajor.pvs.base.domain;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class PatientHistory extends BasicEntity<Integer> {

    @ManyToOne
    private Anamnesis anamnesis;
    @OneToMany
    private List<Disease> disease;
    @OneToOne
    private Patient patient;
    private Date dateOfDiagnosis;
    private String additionalInformation;
    
}
