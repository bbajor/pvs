package de.bbajor.pvs.ivom.model;

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
public class TimeSlot extends BasicEntity<Integer> {

    private LocalDate dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    @ManyToOne
    private CooperatingAmbulance ambulance;

}
