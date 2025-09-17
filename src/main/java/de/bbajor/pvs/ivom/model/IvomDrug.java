package de.bbajor.pvs.ivom.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class IvomDrug extends BasicEntity<Long> {

    private String name;
    private String manufacturerName;
    private String manufacturer;
    private String description;
    private String approvalNumber;

}
