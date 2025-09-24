package de.bbajor.pvs.ivomplan.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import de.bbajor.pvs.ivomplan.dto.Bundesland;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.dto.TimePeriod;
import de.bbajor.pvs.ivomplan.dto.TimeSlotRepetition;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TimeSlotConfig {

    private String description;
    private DayOfWeek dayOfWeek;
    private LocalDate periodStart;
    private LocalTime startTime;
    private LocalTime endTime;
    private TimeSlotRepetition timeSlotRepetition;
    private TimePeriod timePeriod;
    private SurgeryUnitDto surgeryUnit;
    private Bundesland bundesland;
    public boolean isSingleAppointment;

}
