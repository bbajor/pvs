package de.bbajor.pvs.patient.dto;

public enum Title {

    DR("Dr."),
    PROF_DR("Prof. Dr."),
    PROF("Prof."),
    NONE("");

    private final String displayName;

    Title(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static Title byString(String title) {
        for (Title element : Title.values()) {
            if (element.toString().equals(title)) {
                return element;
            }
        }
        return NONE;
    }

}
