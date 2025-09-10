package de.bbajor.pvs.base.domain;

public enum Title {
    
    DR("Dr."),
    PROF(  "Prof."),
    NONE("");

    private String titleString;

    private Title(String titleString) {
        this.titleString = titleString;
    }

    public String getTitleString() {
        return titleString;
    }
}
