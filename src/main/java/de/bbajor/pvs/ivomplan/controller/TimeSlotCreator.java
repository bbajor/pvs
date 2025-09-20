package de.bbajor.pvs.ivomplan.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import de.bbajor.pvs.ivomplan.dto.Bundesland;
import de.bbajor.pvs.ivomplan.dto.TimePeriod;
import de.bbajor.pvs.ivomplan.dto.TimeSlotDto;

public class TimeSlotCreator {

    public static List<TimeSlotDto> createTimeSlots(TimeSlotConfig timeSlotConfig) {

        List<TimeSlotDto> resultList = new ArrayList<>();
        if (timeSlotConfig == null) {
            return resultList;
        }

        LocalTime startTime = timeSlotConfig.getStartTime();
        LocalTime endTime = timeSlotConfig.getEndTime();

        TimePeriod timePeriod = timeSlotConfig.getTimePeriod();
        LocalDate periodStart = timeSlotConfig.getPeriodStart();
        LocalDate periodEnd = timePeriod.calculateEndDate(periodStart);
        int repeatEveryWeeks = timeSlotConfig.getTimeSlotRepetition().getRepeatEveryWeeks();
        DayOfWeek dayOfWeek = timeSlotConfig.getDayOfWeek();
        Bundesland bundesland = timeSlotConfig.getBundesland();

        LocalDate currentDate = periodStart.with(TemporalAdjusters.nextOrSame(dayOfWeek));

        while (currentDate.getDayOfWeek() != dayOfWeek) {
            currentDate = currentDate.plusDays(1);
        }

        while (!currentDate.isAfter(periodEnd)) {

            if (!HolidayUtils.isHoliday(currentDate, bundesland) && !HolidayUtils.isWeekend(currentDate)) {
                TimeSlotDto timeSlotDto = new TimeSlotDto()
                        .setDescription(timeSlotConfig.getDescription())
                        .setStartDate(currentDate)
                        .setStartTime(startTime)
                        .setEndTime(endTime);
                resultList.add(timeSlotDto);
            }

            currentDate = currentDate.plusWeeks(repeatEveryWeeks);
        }

        return resultList;
    }
}
