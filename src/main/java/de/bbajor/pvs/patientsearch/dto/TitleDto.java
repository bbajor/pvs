package de.bbajor.pvs.patientsearch.dto;

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


}
