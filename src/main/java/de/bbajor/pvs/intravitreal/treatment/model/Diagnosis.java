package de.bbajor.pvs.intravitreal.treatment.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
public class Diagnosis extends BasicEntity<Long> {

    // Tenant isolation: Null for system-wide diagnoses, otherwise via treatmentPlan.patient.practice.tenant

    private String name;
    private String icdCode;
    private String description;

    @Override
    public String toString() {
        return name + (icdCode != null && !icdCode.isBlank() ? " (ICD: " + icdCode + ")" : "");
    }
}