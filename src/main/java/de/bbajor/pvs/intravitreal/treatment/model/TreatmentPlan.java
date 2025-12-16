package de.bbajor.pvs.intravitreal.treatment.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.patient.model.Patient;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
public class TreatmentPlan extends BasicEntity<Long> {

    /**
     * Data isolation: All filtering is done via institution.
     * TreatmentPlan → Patient → Location → Institution (primary path).
     * During migration, also supports Patient → Practice → Tenant (legacy).
     * 
     * Explicit institution_id for performance and data isolation compliance.
     */

    private LocalDate creationDate;
    private String description;
    
    /**
     * Date when the treatment plan was finished/completed.
     * A treatment plan is considered finished when no more treatment appointments are scheduled
     * or the last treatment appointment has been completed.
     * If null, the treatment plan is still active.
     */
    private LocalDate finishedDate;
    
    /**
     * The institution this treatment plan belongs to.
     * Data isolation: All filtering is done via institution.
     * This is set automatically from patient.location.institution or patient.practice.tenant.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;
    
    @ManyToOne(fetch = FetchType.EAGER)
    private Patient patient;
    @ManyToOne(fetch = FetchType.EAGER)
    private Diagnosis diagnosis;
    @OneToOne
    private ClinicalTrial clinicalTrial;
    private String additionalInformation;
    @OneToMany(mappedBy = "treatmentPlan", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, orphanRemoval = false)
    private List<Treatment> treatments = new ArrayList<>();

    public String getFirstName() {
        return patient.getFirstName();
    }

    public String getLastName() {
        return patient.getLastName();
    }

    public String getHealthInsurance() {
        if (patient == null) {
            return null;
        }
        // Prüfe, ob Patient eine private Versicherung hat
        if (Boolean.TRUE.equals(patient.getIsPrivateInsurance())) {
            return "Privat";
        }
        // Prüfe, ob healthInsurance null ist
        if (patient.getHealthInsurance() == null) {
            return "-";
        }
        // Verwende toString() wie in der Patientenübersicht, um costCarrierName zu priorisieren
        return patient.getHealthInsurance().toString();
    }

    public LocalDate getBirth() {
        return patient.getBirth();
    }

}
