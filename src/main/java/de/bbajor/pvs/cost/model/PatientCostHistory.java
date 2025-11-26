package de.bbajor.pvs.cost.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Entity für Kostenhistorie am Patienten.
 * Speichert alle Kosten, die einem Patienten zugeordnet sind.
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "patient_cost_history")
public class PatientCostHistory extends BasicEntity<Long> {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "treatment_id", nullable = false)
    private Treatment treatment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "treatment_cost_id", nullable = false)
    private TreatmentCost treatmentCost;

    /**
     * Kostenanteil dieses Patienten (in Euro)
     */
    @Column(name = "cost_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal costAmount;

    /**
     * Datum der Behandlung
     */
    @Column(name = "treatment_date", nullable = false)
    private LocalDate treatmentDate;

    /**
     * OP-Saal
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "surgical_center_id")
    private SurgicalCenter surgicalCenter;
}

