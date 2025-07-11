package de.bbajor.pvs.base.domain;

import de.bbajor.pvs.icd10.domain.IcdEntry;
import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
public class Disease {
    private int id;
    private String name;
    private IcdEntry icdEntry;
}
