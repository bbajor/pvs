package de.bbajor.pvs.ivomplan.dto;

import java.time.LocalDate;
import java.time.Month;

public enum TimePeriod {

    NONE("Einzeltermin", 0),
    ONE_MONTH("1 Monat", 1),
    TWO_MONTHS("2 Monate", 2),
    THREE_MONTHS("3 Monate", 3),
    FOUR_MONTS("4 Monate", 4),
    SIX_MONTHS("ein halbes Jahr", 6),
    END_OF_YEAR("bis zum Jahresende", -1),
    ONE_YEAR("1 Jahr", 12),
    TWO_YEARS("2 Jahre", 24);

    private final String value;
    private final int monthsToAdd;

    TimePeriod(String value, int monthsToAdd) {
        this.value = value;
        this.monthsToAdd = monthsToAdd;
    }

    @Override
    public String toString() {
        return value;
    }

    public LocalDate calculateEndDate(LocalDate startDate) {
        if (monthsToAdd == -1) { // until end of year
            return LocalDate.of(startDate.getYear(), Month.DECEMBER, 31);
        }
        return startDate.plusMonths(monthsToAdd);
    }

}
