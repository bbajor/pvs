package de.bbajor.pvs.intravitreal.treatment.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
public class ClinicalTrial extends BasicEntity<Long> {

    private String name;
    private String description;
    private String code;
    private String sponsor;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;

    @ManyToOne
    private TreatmentPlan ivom;

}
