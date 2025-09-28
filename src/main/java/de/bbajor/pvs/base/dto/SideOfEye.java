package de.bbajor.pvs.base.dto;

public enum SideOfEye {
    LEFT("Linkes Auge", "l"),
    RIGHT("Rechtes Auge", "r");

    private final String displayName;
    private final String dbString;

    SideOfEye(String displayName, String dbString) {
        this.displayName = displayName;
        this.dbString = dbString;
    }

    public static SideOfEye byDbString(String dbString) {
        if (dbString == null) {
            return null;
        }
        for (SideOfEye sideOfEye : SideOfEye.values()) {
            if (sideOfEye.dbString.equals(dbString.trim())) {
                return sideOfEye;
            }
        }
        return null;
    }

    public String asDbString() {
        return dbString;
    }

    @Override
    public String toString() {
        return displayName;
    }

}
