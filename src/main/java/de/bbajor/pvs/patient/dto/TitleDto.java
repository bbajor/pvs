package de.bbajor.pvs.patient.dto;

public enum TitleDto {

    DR("Dr."),
    PROF_DR("Prof. Dr."),
    PROF("Prof."),
    NONE("");

    private final String displayName;

    TitleDto(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public TitleDto byString(String title) {
        for (TitleDto element : TitleDto.values()) {
            if (element.toString().equals(title)) {
                return element;
            }
        }
        return NONE;
    }

}
