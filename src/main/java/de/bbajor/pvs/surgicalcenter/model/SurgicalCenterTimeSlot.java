package de.bbajor.pvs.surgicalcenter.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.intravitreal.treatment.model.IvomPlanTimeSlot;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class SurgicalCenterTimeSlot extends BasicEntity<Long> {

    private String description;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    @ManyToOne
    private SurgicalCenter surgicalCenter;
    @OneToMany(mappedBy = "surgicalCenterTimeSlot", fetch = FetchType.LAZY)
    private List<IvomPlanTimeSlot> ivomPlanTimeSlots;
    private boolean isAvailable;

}
