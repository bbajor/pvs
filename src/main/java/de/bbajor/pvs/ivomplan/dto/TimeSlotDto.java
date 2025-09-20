package de.bbajor.pvs.ivomplan.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TimeSlotDto {

    private Long id;
    private String description;
    private LocalDate startDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private SurgeryUnitDto surgeryUnit;
    private boolean isAvailable;
    private List<IvomPlanDto> ivom;

    String getSurgeryUnit() {
        return surgeryUnit != null ? surgeryUnit.toString() : "Standort n.b.";
    }
}
