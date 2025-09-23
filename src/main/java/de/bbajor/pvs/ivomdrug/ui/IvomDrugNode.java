package de.bbajor.pvs.ivomdrug.ui;

import java.util.Objects;

import de.bbajor.pvs.ivomdrug.dto.IvomDrugDto;

public class IvomDrugNode {
    private final String label;
    private final IvomDrugDto dto; // nur bei Leaf-Knoten gefüllt

    public IvomDrugNode(String label) {
        this(label, null);
    }

    public IvomDrugNode(String label, IvomDrugDto dto) {
        this.label = label != null ? label : "";
        this.dto = dto;
    }

    public String getLabel() {
        return label;
    }

    public IvomDrugDto getDto() {
        return dto;
    }

    public String getZulassungsRegNrOderKennziffer() {
        return dto == null ? "-" : dto.getZulassungsRegNrOderKennziffer();
    }

    public String getArzneimittelbezeichnung() {
        return dto == null ? "-" : dto.getArzneimittelbezeichnung();
    }

    public String getWirkstoffe() {
        return dto == null ? "-" : dto.getWirkstoffe();
    }

    public String getZulassungsinhaber() {
        return dto == null ? "-" : dto.getZulassungsinhaber();
    }

    public String getEingangsnummer() {
        return dto == null ? "-" : dto.getEingangsnummer();
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
        if (!(o instanceof IvomDrugNode))
            return false;
        IvomDrugNode other = (IvomDrugNode) o;
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
