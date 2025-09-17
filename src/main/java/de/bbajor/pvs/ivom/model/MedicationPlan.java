package de.bbajor.pvs.ivom.model;

import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class MedicationPlan extends BasicEntity<Long> {

    @ManyToOne
    private IvomDiagnose reasonForIvom;

    @OneToMany
    private List<Treatment> treatments;

}
