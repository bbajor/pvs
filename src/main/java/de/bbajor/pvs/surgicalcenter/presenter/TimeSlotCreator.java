package de.bbajor.pvs.surgicalcenter.presenter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.bbajor.pvs.base.util.HolidayUtils;
import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.intravitreal.treatment.dto.State;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;

public class TimeSlotCreator {

    public static List<SurgicalCenterTimeSlotDto> createTimeSlots(TimeSlotConfig timeSlotConfig) {

        List<SurgicalCenterTimeSlotDto> resultList = new ArrayList<>();
        if (timeSlotConfig == null || timeSlotConfig.getDayOfWeek() == null
                || timeSlotConfig.getStartTime() == null || timeSlotConfig.getEndTime() == null
                || timeSlotConfig.getPeriodStartDate() == null || timeSlotConfig.getTimePeriod() == null
                || timeSlotConfig.getTimeSlotRepetition() == null || timeSlotConfig.getBundesland() == null) {
            return resultList;
        }

        LocalTime startTime = timeSlotConfig.getStartTime();
        LocalTime endTime = timeSlotConfig.getEndTime();

        TimePeriod timePeriod = timeSlotConfig.getTimePeriod();
        LocalDate periodStart = timeSlotConfig.getPeriodStartDate();
        LocalDate periodEnd = timePeriod.calculateEndDate(periodStart);
        int repeatEveryWeeks = timeSlotConfig.getTimeSlotRepetition().getRepeatEveryWeeks();
        DayOfWeek dayOfWeek = timeSlotConfig.getDayOfWeek();
        State bundesland = timeSlotConfig.getBundesland();

        LocalDate currentDate = periodStart.with(TemporalAdjusters.nextOrSame(dayOfWeek));

        while (currentDate.getDayOfWeek() != dayOfWeek) {
            currentDate = currentDate.plusDays(1);
        }

        while (!currentDate.isAfter(periodEnd)) {

            if (!HolidayUtils.isHoliday(currentDate, bundesland) && !HolidayUtils.isWeekend(currentDate)) {
                SurgicalCenterTimeSlotDto timeSlotDto = new SurgicalCenterTimeSlotDto()
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

    public static Collection<SurgicalCenterTimeSlotDto> getNewInvalidTimeSlots(
            List<SurgicalCenterTimeSlotDto> availableTimeSlots,
            List<SurgicalCenterTimeSlotDto> newTimeSlots) {

        Collection<SurgicalCenterTimeSlotDto> invalidSlots = new ArrayList<>();
        if (availableTimeSlots == null || availableTimeSlots.isEmpty() || newTimeSlots == null
                || newTimeSlots.isEmpty()) {
            return invalidSlots;
        }

        for (SurgicalCenterTimeSlotDto availableTimeSlot : availableTimeSlots) {
            for (SurgicalCenterTimeSlotDto newTimeSlot : newTimeSlots) {
                if (availableTimeSlot.getDate().isEqual(newTimeSlot.getDate())
                        && isTimeCollision(availableTimeSlot, newTimeSlot)) {
                    invalidSlots.add(newTimeSlot);
                }
            }
        }
        return invalidSlots;
    }

    private static boolean isTimeCollision(SurgicalCenterTimeSlotDto availableTimeSlot,
            SurgicalCenterTimeSlotDto newTimeSlot) {
        return isHasSameHourAndMinute(availableTimeSlot, newTimeSlot)
                || isLocalTimeInAvailableSlot(availableTimeSlot, newTimeSlot.getStartTime())
                || isLocalTimeInAvailableSlot(availableTimeSlot, newTimeSlot.getEndTime());
    }

    private static boolean isLocalTimeInAvailableSlot(SurgicalCenterTimeSlotDto availableTimeSlot,
            LocalTime newSlotStart) {
        return newSlotStart.isAfter(availableTimeSlot.getStartTime())
                && newSlotStart.isBefore(availableTimeSlot.getEndTime());
    }

    private static boolean isHasSameHourAndMinute(SurgicalCenterTimeSlotDto availableTimeSlot,
            SurgicalCenterTimeSlotDto newTimeSlot) {
        return availableTimeSlot.getStartTime().getHour() == newTimeSlot.getStartTime().getHour()
                && availableTimeSlot.getEndTime().getHour() == newTimeSlot.getEndTime().getHour()
                && availableTimeSlot.getStartTime().getMinute() == newTimeSlot.getStartTime().getMinute()
                && availableTimeSlot.getEndTime().getMinute() == newTimeSlot.getEndTime().getMinute();
    }
}
