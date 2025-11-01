package de.bbajor.pvs.intravitreal.treatment.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.tenant.model.Tenant;
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

    /**
     * The tenant this diagnosis belongs to.
     * Null for system-wide diagnoses available to all tenants.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    private String name;
    private String icdCode;
    private String description;

    @Override
    public String toString() {
        return name + (icdCode != null && !icdCode.isBlank() ? " (ICD: " + icdCode + ")" : "");
    }
}