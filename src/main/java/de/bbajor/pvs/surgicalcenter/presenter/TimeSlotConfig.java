package de.bbajor.pvs.surgicalcenter.presenter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import de.bbajor.pvs.base.dto.TimePeriod;
import de.bbajor.pvs.base.dto.TimeSlotRepetition;
import de.bbajor.pvs.intravitreal.treatment.dto.State;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TimeSlotConfig {

    private String description;
    private DayOfWeek dayOfWeek;
    private LocalDate periodStartDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private TimeSlotRepetition timeSlotRepetition;
    private TimePeriod timePeriod;
    private SurgicalCenterDto surgicalCenter;
    private State bundesland;
    public boolean isSingleAppointment;

}
