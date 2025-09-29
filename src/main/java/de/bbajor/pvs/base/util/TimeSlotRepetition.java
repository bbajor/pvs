package de.bbajor.pvs.base.util;

public enum TimeSlotRepetition {

    NO_REPETITION("Keine Wiederholung", 0),
    WEEKLY("Wöchentlich", 1),
    EVERY_TWO_WEEKS("Alle 2 Wochen", 2),
    EVERY_THREE_WEEKS("Alle 3 Wochen", 3),
    EVERY_FOUR_WEEKS("Alle 4 Wochen", 4),
    EVERY_FIVE_WEEKS("Alle 5 Wochen", 5),
    EVERY_SIX_WEEKS("Alle 6 Wochen", 6),
    EVERY_EIGHT_WEEKS("Alle 8 Wochen", 8),
    EVERY_TEN_WEEKS("Alle 10 Wochen", 10),
    EVERY_TWELVE_WEEKS("Alle 12 Wochen", 12),
    EVERY_SIXTEEN_WEEKS("Alle 16 Wochen", 16);

    private final String value;
    private final int repeteEveryWeeks;

    TimeSlotRepetition(String value, int repeteEveryWeeks) {
        this.value = value;
        this.repeteEveryWeeks = repeteEveryWeeks;
    }

    @Override
    public String toString() {
        return value;
    }

    public int getRepeatEveryWeeks() {
        return repeteEveryWeeks;
    }
}
