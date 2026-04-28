package de.bbajor.pvs.ivomplan.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.bbajor.pvs.base.util.State;
import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.base.util.TimeSlotRepetition;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.presenter.TimeSlotConfig;
import de.bbajor.pvs.surgicalcenter.presenter.TimeSlotCreator;

class TimeSlotCreatorTest {

    @Test
    void createTimeSlots_nullConfigReturnsEmpty() {
        assertThat(TimeSlotCreator.createTimeSlots(null)).isEmpty();
    }

    @Test
    void createTimeSlots_setsAvailableSoPlannerQueriesFindThem() {
        LocalDate monday = LocalDate.of(2030, 1, 7);
        SurgicalCenter center = new SurgicalCenter();
        center.setId(1);

        TimeSlotConfig config = new TimeSlotConfig()
                .setDayOfWeek(DayOfWeek.MONDAY)
                .setStartTime(LocalTime.of(8, 0))
                .setEndTime(LocalTime.of(9, 0))
                .setPeriodStartDate(monday)
                .setTimePeriod(TimePeriod.ONE_MONTH)
                .setTimeSlotRepetition(TimeSlotRepetition.WEEKLY)
                .setBundesland(State.NI)
                .setSurgicalCenter(center);

        List<SurgicalCenterTimeSlot> slots = TimeSlotCreator.createTimeSlots(config);
        assertThat(slots).isNotEmpty();
        assertThat(slots).allMatch(SurgicalCenterTimeSlot::isAvailable);
    }
}
