package de.bbajor.pvs.intravitreal.treatment.model;

import java.time.LocalDate;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.patient.model.Patient;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

    private LocalDate creationDate;
    private String description;
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
        return patient.getHealthInsurance().getBillingCarrierName();
    }

    public LocalDate getBirth() {
        return patient.getBirth();
    }

}
