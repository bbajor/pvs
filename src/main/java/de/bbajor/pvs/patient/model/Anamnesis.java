package de.bbajor.pvs.patient.model;

import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Anamnesis extends BasicEntity<Integer> {

    @OneToMany
    private List<Disease> knownDiseases;
    private String additionalInformation;
}
