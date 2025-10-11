package de.bbajor.pvs.surgicalcenter.presenter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import de.bbajor.pvs.base.util.State;
import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.base.util.TimeSlotRepetition;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
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
    private SurgicalCenter surgicalCenter;
    private State bundesland;
    public boolean isSingleAppointment;

}
