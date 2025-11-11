package de.bbajor.pvs.intravitreal.treatment.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
public class Diagnosis extends BasicEntity<Long> {

    // Tenant isolation: Null for system-wide diagnoses, otherwise via treatmentPlan.patient.practice.tenant

    private String name;
    private String icdCode;
    private String description;

    /**
     * KBV-Metadaten für Validierung und Synchronisation.
     * Null, wenn nicht gegen KBV validiert.
     */
    @Column(name = "kbv_quarter", length = 20)
    private String kbvQuarter;

    @Column(name = "kbv_valid_from")
    private LocalDate kbvValidFrom;

    @Column(name = "kbv_valid_to")
    private LocalDate kbvValidTo;

    @Column(name = "validated_against_kbv")
    private Boolean validatedAgainstKbv = false;

    /**
     * Prüft, ob der ICD-Code zum gegebenen Datum gültig ist.
     */
    public boolean isIcdCodeValid(LocalDate date) {
        if (!Boolean.TRUE.equals(validatedAgainstKbv) || kbvValidFrom == null) {
            return false;
        }
        if (kbvValidTo != null && date.isAfter(kbvValidTo)) {
            return false;
        }
        return !date.isBefore(kbvValidFrom);
    }

    /**
     * Prüft, ob der ICD-Code aktuell (heute) gültig ist.
     */
    public boolean isIcdCodeCurrentlyValid() {
        return isIcdCodeValid(LocalDate.now());
    }

    @Override
    public String toString() {
        return name + (icdCode != null && !icdCode.isBlank() ? " (ICD: " + icdCode + ")" : "");
    }
}