package de.bbajor.pvs.ivomplan.model;

import java.time.LocalDate;
import java.time.LocalTime;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class SurgeryUnitTimeSlot extends BasicEntity<Long> {

    private String description;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    @ManyToOne
    private SurgeryUnit surgeryUnit;

}
