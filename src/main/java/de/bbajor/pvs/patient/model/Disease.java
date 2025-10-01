package de.bbajor.pvs.patient.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.medication.model.IcdEntry;
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
