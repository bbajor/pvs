package de.bbajor.pvs.ivomplan.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class IvomDiagnosis extends BasicEntity<Integer> {

    private String name;
    private String icdCode;
    private String description;

}
