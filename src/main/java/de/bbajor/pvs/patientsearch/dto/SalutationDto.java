package de.bbajor.pvs.patientsearch.dto;

public enum SalutationDto {

    MALE("Herr"),
    FEMALE("Frau"),
    DIVERSE(""),
    UNKNOWN("");

    private final String ident;

    SalutationDto(String ident) {
        this.ident = ident;
    }

    @Override
    public String toString() {
        return ident;
    }

    public SalutationDto byString(String salutation) {
        for (SalutationDto element : SalutationDto.values()) {
            if (element.toString().equals(salutation)) {
                return element;
            }
        }
        return UNKNOWN;
    }
}
