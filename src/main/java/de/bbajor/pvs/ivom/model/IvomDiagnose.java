package de.bbajor.pvs.ivom.model;

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
public class IvomDiagnose extends BasicEntity<Integer> {

    private String name;
    private String icdCode;
    private String description;
    @ManyToOne
    private Treatment treatment;

}
