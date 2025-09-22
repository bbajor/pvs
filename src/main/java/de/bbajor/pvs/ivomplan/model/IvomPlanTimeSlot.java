package de.bbajor.pvs.ivomplan.model;

import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class IvomPlanTimeSlot extends BasicEntity<Long> {

    @OneToMany
    private List<SurgeryUnitTimeSlot> timeSlotSurgeryUnit;
}
