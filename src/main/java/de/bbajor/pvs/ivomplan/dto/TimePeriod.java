package de.bbajor.pvs.ivomplan.dto;

public enum TimePeriod {

    ONE_MONTH("1 Monat"),
    TWO_MONTHS("2 Monate"),
    THREE_MONTHS("3 Monate"),
    FOUR_MONTS("4 Monate"),
    SIX_MONTHS("ein halbes Jahr"),
    END_OF_YEAR("bis zum Jahresende"),
    ONE_YEAR("1 Jahr"),
    TWO_YEARS("2 Jahre");

    private String value;

    TimePeriod(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

}
