package de.bbajor.pvs.ivomplan.dto;

public enum TimeSlotRepetition {

    EVERY_WORKING_DAY("An jedem Werktag"),
    WEEKLY("Wöchentlich"),
    EVERY_TWO_WEEKS("Alle 2 Wochen"),
    EVERY_THREE_WEEKS("Alle 3 Wochen"),
    EVERY_FOUR_WEEKS("Alle 4 Wochen"),
    EVERY_FIVE_WEEKS("Alle 5 Wochen"),
    EVERY_SIX_WEEKS("Alle 6 Wochen"),
    EVERY_EIGHT_WEEKS("Alle 8 Wochen"),
    EVERY_TEN_WEEKS("Alle 10 Wochen"),
    EVERY_TWELVE_WEEKS("Alle 12 Wochen"),
    EVERY_SIXTEEN_WEEKS("Alle 16 Wochen");

    private final String value;

    TimeSlotRepetition(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
