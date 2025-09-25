package de.bbajor.pvs.intravitreal.treatment.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class IvomClinicalTrial extends BasicEntity<Long> {

    private String name;
    private String description;
    private String code;
    private String sponsor;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;

    @ManyToOne
    private IvomPlan ivom;

}
