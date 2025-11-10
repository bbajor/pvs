package de.bbajor.pvs.medication.ui;

import java.util.Objects;

import de.bbajor.pvs.medication.model.Medication;

public class MedicationNode {
    private final String label;
    private final Medication medication; // nur bei Leaf-Knoten gefüllt
    private final boolean favourite;

    public MedicationNode(String label) {
        this(label, null, false);
    }

    public MedicationNode(String label, Medication medication, boolean favourite) {
        this.label = label != null ? label : "";
        this.medication = medication;
        this.favourite = favourite;
    }

    public String getLabel() {
        return medication == null ? label : getArzneimittelbezeichnung();
    }

    public Medication getMedication() {
        return medication;
    }

    public String getArzneimittelbezeichnung() {
        return medication != null ? medication.getArzneimittelbezeichnung() : "";
    }

    public String getWirkstoffe() {
        return medication != null ? medication.getWirkstoffe() : "";
    }

    public String getAnwendungsgebiete() {
        return medication != null ? medication.getAnwendungsgebiete() : "";
    }

    public String getZulassungsinhaber() {
        return medication != null ? medication.getZulassungsinhaber() : "";
    }

    public String getEingangsnummer() {
        return medication != null ? medication.getEingangsnummer() : "";
    }

    public boolean isFavourite() {
        return medication != null && favourite;
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
                Objects.equals(medication != null ? medication.getId() : null,
                        other.medication != null ? other.medication.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, medication != null ? medication.getId() : null);
    }

    public boolean isContainsSearchTerm(String term) {
        return medication != null && (medication.getArzneimittelbezeichnung().toLowerCase().contains(term)
                || (medication.getVertreiber() != null && medication.getVertreiber().toLowerCase().contains(term))
                || (medication.getWirkstoffe() != null && medication.getWirkstoffe().toLowerCase().contains(term))
                || (medication.getAdditionalNotes() != null && medication.getAdditionalNotes().toLowerCase().contains(term))
                || (medication.getZulassungsNr() != null && medication.getZulassungsNr().toLowerCase().contains(term)));
    }
}
