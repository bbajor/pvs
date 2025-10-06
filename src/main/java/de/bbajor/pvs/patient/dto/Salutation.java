package de.bbajor.pvs.patient.dto;

public enum Salutation {

    MALE("Herr"),
    FEMALE("Frau"),
    DIVERSE(""),
    UNKNOWN("");

    private final String ident;

    Salutation(String ident) {
        this.ident = ident;
    }

    @Override
    public String toString() {
        return ident;
    }

    public static Salutation byString(String salutation) {
        for (Salutation element : Salutation.values()) {
            if (element.toString().equals(salutation)) {
                return element;
            }
        }
        return UNKNOWN;
    }
}
