package de.bbajor.pvs.surgicalcenter.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class SurgicalCenterTimeSlotDto {

    private Long id;
    private Long version;
    private String description;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isAvailable;
    private SurgicalCenterDto surgicalCenter;

}
