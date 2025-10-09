package de.bbajor.pvs.base.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class DayOfWeekItemLabelGeneratorTest {

    @Test
    void testApplyReturnsFullDayNameInEnglish() {
        DayOfWeekItemLabelGenerator generator = new DayOfWeekItemLabelGenerator(Locale.ENGLISH);
        assertEquals("Monday", generator.apply(DayOfWeek.MONDAY));
        assertEquals("Sunday", generator.apply(DayOfWeek.SUNDAY));
    }

    @Test
    void testApplyReturnsFullDayNameInGerman() {
        DayOfWeekItemLabelGenerator generator = new DayOfWeekItemLabelGenerator(Locale.GERMAN);
        assertEquals(DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, Locale.GERMAN), generator.apply(DayOfWeek.MONDAY));
        assertEquals(DayOfWeek.SUNDAY.getDisplayName(TextStyle.FULL, Locale.GERMAN), generator.apply(DayOfWeek.SUNDAY));
    }

    @Test
    void testApplyWithNullReturnsEmptyString() {
        DayOfWeekItemLabelGenerator generator = new DayOfWeekItemLabelGenerator(Locale.ENGLISH);
        assertEquals("", generator.apply(null));
    }
}