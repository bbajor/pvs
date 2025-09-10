package de.bbajor.pvs.icd10.domain;

import java.sql.Date;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class IcdVersion extends BasicEntity<Integer> {
 
    private String version;
    private String description;
    private Date validFrom;
    private Date validTo;
}
