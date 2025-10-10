package de.bbajor.pvs.ivomplan.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.presenter.TimeSlotConfig;
import de.bbajor.pvs.surgicalcenter.presenter.TimeSlotCreator;

public class TimeSlotCreatorTest {

    @Test
    void testCreateTimeSlots() {

        List<SurgicalCenterTimeSlot> resultList = TimeSlotCreator.createTimeSlots(null);
        assertNotNull(resultList);
        assertEquals(0, resultList.size());

        TimeSlotConfig config = new TimeSlotConfig();
        TimeSlotCreator.createTimeSlots(config);
        assertNotNull(config);
        assertEquals(0, resultList.size());

        //TODO write more sophisticated test cases

    }
}
