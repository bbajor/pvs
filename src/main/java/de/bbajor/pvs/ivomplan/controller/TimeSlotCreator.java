package de.bbajor.pvs.ivomplan.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.bbajor.pvs.ivomplan.dto.Bundesland;
import de.bbajor.pvs.ivomplan.dto.TimePeriod;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitTimeSlotDto;

public class TimeSlotCreator {

    public static List<SurgeryUnitTimeSlotDto> createTimeSlots(TimeSlotConfig timeSlotConfig) {

        List<SurgeryUnitTimeSlotDto> resultList = new ArrayList<>();
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
                SurgeryUnitTimeSlotDto timeSlotDto = new SurgeryUnitTimeSlotDto()
                        .setDescription(timeSlotConfig.getDescription())
                        .setDate(currentDate)
                        .setStartTime(startTime)
                        .setEndTime(endTime);
                resultList.add(timeSlotDto);
            }

            if (repeatEveryWeeks == 0) {
                currentDate = periodEnd.plusDays(1);
            } else {
                currentDate = currentDate.plusWeeks(repeatEveryWeeks);
            }
        }

        return resultList;
    }

    public static Collection<SurgeryUnitTimeSlotDto> getNewInvalidTimeSlots(
            List<SurgeryUnitTimeSlotDto> availableTimeSlots,
            List<SurgeryUnitTimeSlotDto> newTimeSlots) {
        Collection<SurgeryUnitTimeSlotDto> invalidSlots = new ArrayList<>();
        if (availableTimeSlots == null || availableTimeSlots.isEmpty() || newTimeSlots == null
                || newTimeSlots.isEmpty()) {
            return invalidSlots;
        }

        for (SurgeryUnitTimeSlotDto availableTimeSlot : availableTimeSlots) {
            for (SurgeryUnitTimeSlotDto newTimeSlot : newTimeSlots) {
                if (availableTimeSlot.getDate().isEqual(newTimeSlot.getDate())
                        && isTimeCollision(availableTimeSlot, newTimeSlot)) {
                    invalidSlots.add(newTimeSlot);
                }
            }
        }
        return invalidSlots;
    }

    private static boolean isTimeCollision(SurgeryUnitTimeSlotDto availableTimeSlot,
            SurgeryUnitTimeSlotDto newTimeSlot) {
        return isHasSameHourAndMinute(availableTimeSlot, newTimeSlot)
                || isLocalTimeInAvailableSlot(availableTimeSlot, newTimeSlot.getStartTime())
                || isLocalTimeInAvailableSlot(availableTimeSlot, newTimeSlot.getEndTime());
    }

    private static boolean isLocalTimeInAvailableSlot(SurgeryUnitTimeSlotDto availableTimeSlot,
            LocalTime newSlotStart) {
        return newSlotStart.isAfter(availableTimeSlot.getStartTime())
                && newSlotStart.isBefore(availableTimeSlot.getEndTime());
    }

    private static boolean isHasSameHourAndMinute(SurgeryUnitTimeSlotDto availableTimeSlot,
            SurgeryUnitTimeSlotDto newTimeSlot) {
        return availableTimeSlot.getStartTime().getHour() == newTimeSlot.getStartTime().getHour()
                && availableTimeSlot.getEndTime().getHour() == newTimeSlot.getEndTime().getHour()
                && availableTimeSlot.getStartTime().getMinute() == newTimeSlot.getStartTime().getMinute()
                && availableTimeSlot.getEndTime().getMinute() == newTimeSlot.getEndTime().getMinute();
    }
}
