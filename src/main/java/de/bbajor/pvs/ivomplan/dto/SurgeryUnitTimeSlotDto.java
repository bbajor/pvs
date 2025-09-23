package de.bbajor.pvs.ivomplan.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SurgeryUnitTimeSlotDto {

    private Long id;
    private String description;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isAvailable;
    private SurgeryUnitDto surgeryUnit;
}
