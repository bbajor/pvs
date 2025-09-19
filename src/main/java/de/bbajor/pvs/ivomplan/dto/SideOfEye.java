package de.bbajor.pvs.ivomplan.dto;

public enum SideOfEye {
    LEFT("Linkes Auge"),
    RIGHT("Rechtes Auge"),
    BOTH("Beide Augen"),
    UNKNOWN("Nicht angegeben");

    private final String displayName;

    SideOfEye(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
