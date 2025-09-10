package de.bbajor.pvs.base.domain;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Anamnesis extends BasicEntity<Integer> {

    @OneToMany
    private List<Disease> knownDiseases;
    private String additionalInformation;
}
