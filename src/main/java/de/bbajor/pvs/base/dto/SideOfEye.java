package de.bbajor.pvs.base.dto;

public enum SideOfEye {
    LEFT("Linkes Auge"),
    RIGHT("Rechtes Auge"),
    BOTH("Beide Augen"),
    UNKNOWN("Nicht angegeben");

    private final String displayName;

    SideOfEye(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

}
