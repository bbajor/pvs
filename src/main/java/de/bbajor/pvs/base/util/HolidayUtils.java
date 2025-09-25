package de.bbajor.pvs.base.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import de.bbajor.pvs.intravitreal.treatment.dto.State;

public class HolidayUtils {

    public static Set<LocalDate> getHolidaysForYear(int year, State bl) {
        Set<LocalDate> holidays = new HashSet<>();

        holidays.add(LocalDate.of(year, 1, 1)); // Neujahr
        holidays.add(LocalDate.of(year, 12, 25)); // Weihnachten
        holidays.add(LocalDate.of(year, 5, 1)); // Tag der Arbeit
        holidays.add(LocalDate.of(year, 8, 15)); // Mariä Himmelfahrt (beispielsweise)

        // Bewegliche Feiertage (abhängig von Ostern)
        LocalDate easterSunday = calculateEasterSunday(year);

        holidays.add(easterSunday.minusDays(2)); // Karfreitag
        holidays.add(easterSunday.plusDays(1)); // Ostermontag
        holidays.add(easterSunday.plusDays(39)); // Christi Himmelfahrt
        holidays.add(easterSunday.plusDays(50)); // Pfingstmontag

        // ---- Regionale Feiertage mit Sets ----
        if (Set.of(State.BW, State.BY, State.ST).contains(bl)) {
            holidays.add(LocalDate.of(year, 1, 6)); // Heilige Drei Könige
        }
        if (Set.of(State.BW, State.BY, State.HE, State.NW,
                State.RP, State.SL, State.SN, State.TH).contains(bl)) {
            holidays.add(easterSunday.plusDays(60)); // Fronleichnam
        }
        if (Set.of(State.SL, State.BY).contains(bl)) {
            holidays.add(LocalDate.of(year, 8, 15)); // Mariä Himmelfahrt
        }
        if (Set.of(State.BB, State.MV, State.SN, State.ST,
                State.TH, State.HB, State.HH, State.NI, State.SH).contains(bl)) {
            holidays.add(LocalDate.of(year, 10, 31)); // Reformationstag
        }
        if (Set.of(State.BW, State.BY, State.NW, State.RP, State.SL).contains(bl)) {
            holidays.add(LocalDate.of(year, 11, 1)); // Allerheiligen
        }
        if (bl == State.SN) {
            holidays.add(getBussUndBettag(year)); // Buß- und Bettag
        }

        return holidays;
    }

    public static boolean isHoliday(LocalDate date, State bundesland) {
        return getHolidaysForYear(date.getYear(), bundesland).contains(date);
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /**
     * Berechnet den Ostersonntag nach der Gaußschen Osterformel
     */
    private static LocalDate calculateEasterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;

        return LocalDate.of(year, month, day);
    }

    /** Buß- und Bettag: Mittwoch vor dem 23. November */
    private static LocalDate getBussUndBettag(int year) {
        LocalDate nov23 = LocalDate.of(year, 11, 23);
        LocalDate wednesdayBefore = nov23;
        while (wednesdayBefore.getDayOfWeek() != DayOfWeek.WEDNESDAY) {
            wednesdayBefore = wednesdayBefore.minusDays(1);
        }
        return wednesdayBefore;
    }
}
