package de.bbajor.pvs.base.domain;

import de.bbajor.pvs.icd10.domain.IcdEntry;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Disease extends BasicEntity<Integer> {
    private String name;
    @ManyToOne
    private IcdEntry icdEntry;
}
