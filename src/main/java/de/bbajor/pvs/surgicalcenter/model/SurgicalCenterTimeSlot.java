package de.bbajor.pvs.surgicalcenter.model;

import java.time.LocalDate;
import java.time.LocalTime;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
public class SurgicalCenterTimeSlot extends BasicEntity<Long> {

    private String description;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    @ManyToOne
    private SurgicalCenter surgicalCenter;
    private boolean isAvailable;
    private boolean isApproved;

}
