package de.bbajor.pvs.base.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class HolidayUtilsTest {

    @Test
    void testGetHolidaysForYear_CommonHolidays() {
        Set<LocalDate> holidays = HolidayUtils.getHolidaysForYear(2024, State.BY);
        assertTrue(holidays.contains(LocalDate.of(2024, 1, 1))); // Neujahr
        assertTrue(holidays.contains(LocalDate.of(2024, 12, 25))); // Weihnachten
        assertTrue(holidays.contains(LocalDate.of(2024, 5, 1))); // Tag der Arbeit
    }

    @Test
    void testGetHolidaysForYear_MovableHolidays() {
        Set<LocalDate> holidays = HolidayUtils.getHolidaysForYear(2024, State.BY);
        // Easter Sunday 2024: March 31
        assertTrue(holidays.contains(LocalDate.of(2024, 3, 29))); // Karfreitag
        assertTrue(holidays.contains(LocalDate.of(2024, 4, 1))); // Ostermontag
        assertTrue(holidays.contains(LocalDate.of(2024, 5, 9))); // Christi Himmelfahrt
        assertTrue(holidays.contains(LocalDate.of(2024, 5, 20))); // Pfingstmontag
    }

    @Test
    void testGetHolidaysForYear_RegionalHolidays() {
        Set<LocalDate> holidaysBY = HolidayUtils.getHolidaysForYear(2024, State.BY);
        assertTrue(holidaysBY.contains(LocalDate.of(2024, 1, 6))); // Heilige Drei Könige
        assertTrue(holidaysBY.contains(LocalDate.of(2024, 5, 30))); // Fronleichnam
        assertTrue(holidaysBY.contains(LocalDate.of(2024, 8, 15))); // Mariä Himmelfahrt
        assertTrue(holidaysBY.contains(LocalDate.of(2024, 11, 1))); // Allerheiligen

        Set<LocalDate> holidaysSN = HolidayUtils.getHolidaysForYear(2024, State.SN);
        assertTrue(holidaysSN.contains(LocalDate.of(2024, 10, 31))); // Reformationstag
        // Buß- und Bettag 2024: 20.11.2024
        assertTrue(holidaysSN.contains(LocalDate.of(2024, 11, 20)));
    }

    @Test
    void testIsHoliday() {
        assertTrue(HolidayUtils.isHoliday(LocalDate.of(2024, 1, 1), State.BY));
        assertFalse(HolidayUtils.isHoliday(LocalDate.of(2024, 2, 1), State.BY));
    }

    @Test
    void testIsWeekend() {
        assertTrue(HolidayUtils.isWeekend(LocalDate.of(2024, 6, 8))); // Saturday
        assertTrue(HolidayUtils.isWeekend(LocalDate.of(2024, 6, 9))); // Sunday
        assertFalse(HolidayUtils.isWeekend(LocalDate.of(2024, 6, 10))); // Monday
    }

    @Test
    void testGetHolidaysForYear_Reformationstag() {
        Set<LocalDate> holidays = HolidayUtils.getHolidaysForYear(2024, State.BB);
        assertTrue(holidays.contains(LocalDate.of(2024, 10, 31))); // Reformationstag
    }

    @Test
    void testGetHolidaysForYear_Allerheiligen() {
        Set<LocalDate> holidays = HolidayUtils.getHolidaysForYear(2024, State.BW);
        assertTrue(holidays.contains(LocalDate.of(2024, 11, 1))); // Allerheiligen
    }

    @Test
    void testGetBussUndBettag_KnownDates() {
        // 2024: 20.11.2024
        assertTrue(HolidayUtils.getBussUndBettag(2024).equals(LocalDate.of(2024, 11, 20)));
        // 2023: 22.11.2023
        assertTrue(HolidayUtils.getBussUndBettag(2023).equals(LocalDate.of(2023, 11, 22)));
        // 2022: 16.11.2022
        assertTrue(HolidayUtils.getBussUndBettag(2022).equals(LocalDate.of(2022, 11, 16)));
        // 2021: 17.11.2021
        assertTrue(HolidayUtils.getBussUndBettag(2021).equals(LocalDate.of(2021, 11, 17)));
    }

    @Test
    void testGetBussUndBettag_IsWednesday() {
        for (int year = 2000; year <= 2030; year++) {
            LocalDate bussUndBettag = HolidayUtils.getBussUndBettag(year);
            assertTrue(bussUndBettag.getDayOfWeek() == java.time.DayOfWeek.WEDNESDAY,
                "Buß- und Bettag should be a Wednesday in year " + year);
            assertTrue(bussUndBettag.isBefore(LocalDate.of(year, 11, 23)),
                "Buß- und Bettag should be before November 23 in year " + year);
        }
    }

    @Test
    void testCalculateEasterSunday_KnownDates() {
        // Known Easter Sundays (Gregorian calendar)
        assertTrue(HolidayUtils.calculateEasterSunday(2024).equals(LocalDate.of(2024, 3, 31)));
        assertTrue(HolidayUtils.calculateEasterSunday(2023).equals(LocalDate.of(2023, 4, 9)));
        assertTrue(HolidayUtils.calculateEasterSunday(2022).equals(LocalDate.of(2022, 4, 17)));
        assertTrue(HolidayUtils.calculateEasterSunday(2021).equals(LocalDate.of(2021, 4, 4)));
        assertTrue(HolidayUtils.calculateEasterSunday(2020).equals(LocalDate.of(2020, 4, 12)));
        assertTrue(HolidayUtils.calculateEasterSunday(2019).equals(LocalDate.of(2019, 4, 21)));
        assertTrue(HolidayUtils.calculateEasterSunday(2000).equals(LocalDate.of(2000, 4, 23)));
        assertTrue(HolidayUtils.calculateEasterSunday(1990).equals(LocalDate.of(1990, 4, 15)));
        assertTrue(HolidayUtils.calculateEasterSunday(1954).equals(LocalDate.of(1954, 4, 18)));
    }

    @Test
    void testCalculateEasterSunday_AlwaysSunday() {
        for (int year = 1900; year <= 2100; year++) {
            LocalDate easter = HolidayUtils.calculateEasterSunday(year);
            assertTrue(easter.getDayOfWeek() == java.time.DayOfWeek.SUNDAY,
                "Easter Sunday should be a Sunday in year " + year);
        }
    }

    @Test
    void testGetHolidaysForNiedersachsen_2025to2030() {
        // Test für jedes Jahr von 2025 bis 2030
        int[] years = {2025, 2026, 2027, 2028, 2029, 2030};
        
        for (int year : years) {
            Set<LocalDate> holidays = HolidayUtils.getHolidaysForYear(year, State.NI);
            
            // Teste fixe Feiertage
            assertTrue(holidays.contains(LocalDate.of(year, 1, 1)), 
                "Neujahr " + year + " fehlt");
            assertTrue(holidays.contains(LocalDate.of(year, 5, 1)), 
                "Tag der Arbeit " + year + " fehlt");
            assertTrue(holidays.contains(LocalDate.of(year, 10, 3)), 
                "Tag der Deutschen Einheit " + year + " fehlt");
            assertTrue(holidays.contains(LocalDate.of(year, 10, 31)), 
                "Reformationstag " + year + " fehlt");
            assertTrue(holidays.contains(LocalDate.of(year, 12, 25)), 
                "1. Weihnachtstag " + year + " fehlt");
            assertTrue(holidays.contains(LocalDate.of(year, 12, 26)), 
                "2. Weihnachtstag " + year + " fehlt");

            // Berechne und teste bewegliche Feiertage
            LocalDate easterSunday = HolidayUtils.calculateEasterSunday(year);
            
            // Karfreitag (2 Tage vor Ostersonntag)
            LocalDate goodFriday = easterSunday.minusDays(2);
            assertTrue(holidays.contains(goodFriday), 
                "Karfreitag " + year + " fehlt");
            
            // Ostermontag (1 Tag nach Ostersonntag)
            LocalDate easterMonday = easterSunday.plusDays(1);
            assertTrue(holidays.contains(easterMonday), 
                "Ostermontag " + year + " fehlt");
            
            // Christi Himmelfahrt (39 Tage nach Ostersonntag)
            LocalDate ascensionDay = easterSunday.plusDays(39);
            assertTrue(holidays.contains(ascensionDay), 
                "Christi Himmelfahrt " + year + " fehlt");
            
            // Pfingstmontag (50 Tage nach Ostersonntag)
            LocalDate whitMonday = easterSunday.plusDays(50);
            assertTrue(holidays.contains(whitMonday), 
                "Pfingstmontag " + year + " fehlt");
            
            // Prüfe die korrekte Gesamtzahl der Feiertage
            assertTrue(holidays.size() == 11, 
                "Falsche Anzahl von Feiertagen für " + year + ": " + holidays.size() + " (erwartet: 11)");
        }
    }
}