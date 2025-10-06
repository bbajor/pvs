package de.bbajor.pvs.medication.ui;

import java.util.Objects;

import de.bbajor.pvs.medication.dto.MedicationDto;

public class MedicationNode {
    private final String label;
    private final MedicationDto dto; // nur bei Leaf-Knoten gefüllt

    public MedicationNode(String label) {
        this(label, null);
    }

    public MedicationNode(String label, MedicationDto dto) {
        this.label = label != null ? label : "";
        this.dto = dto;
    }

    public String getLabel() {
        return dto == null ? label : getArzneimittelbezeichnung();
    }

    public MedicationDto getDto() {
        return dto;
    }

    public String getArzneimittelbezeichnung() {
        return dto != null ? dto.getArzneimittelbezeichnung() : "";
    }

    public String getWirkstoffe() {
        return dto != null ? dto.getWirkstoffe() : "";
    }

    public String getAnwendungsgebiete() {
        return dto != null ? dto.getAnwendungsgebiete() : "";
    }

    public String getZulassungsinhaber() {
        return dto != null ? dto.getZulassungsinhaber() : "";
    }

    public String getEingangsnummer() {
        return dto != null ? dto.getEingangsnummer() : "";
    }

    public boolean isFavourite() {
        return dto == null ? false : dto.isFavourite();
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MedicationNode))
            return false;
        MedicationNode other = (MedicationNode) o;
        // uniqueness über label + optional DTO-ID
        return Objects.equals(label, other.label) &&
                Objects.equals(dto != null ? dto.getId() : null,
                        other.dto != null ? other.dto.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, dto != null ? dto.getId() : null);
    }

    public boolean isContainsSearchTerm(String term) {
        return dto != null && (dto.getArzneimittelbezeichnung().toLowerCase().contains(term)
                || (dto.getVertreiber() != null && dto.getVertreiber().toLowerCase().contains(term))
                || (dto.getWirkstoffe() != null && dto.getWirkstoffe().toLowerCase().contains(term))
                || (dto.getAdditionalNotes() != null && dto.getAdditionalNotes().toLowerCase().contains(term))
                || (dto.getZulassungsNr() != null && dto.getZulassungsNr().toLowerCase().contains(term)));
    }
}
