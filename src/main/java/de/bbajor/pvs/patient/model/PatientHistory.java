package de.bbajor.pvs.patient.model;

import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class PatientHistory extends BasicEntity<Integer> {

    @OneToMany
    private List<PatientRecord> patientRecords;

}
