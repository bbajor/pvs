package de.bbajor.pvs.appointment.model;

import java.time.LocalDateTime;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.tenant.model.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Appointment in the appointment scheduler.
 * Can be linked to a patient and optionally to a treatment.
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "appointment")
public class Appointment extends BasicEntity<Long> {

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private AppointmentScheduler scheduler;

    /**
     * The tenant this appointment belongs to.
     * Ensures data isolation between different practices/clinics.
     * Must match scheduler.tenant and patient.tenant.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime startTime;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime endTime;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private Patient patient;

    /**
     * Optional: Link to a treatment from the treatment plan
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private Treatment treatment;

    @NotNull
    @Column(nullable = false, length = 200)
    private String reason;

    @Column(length = 1000)
    private String notes;

    @Column(length = 500)
    private String additionalInfo;

    /**
     * Status of the appointment
     */
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    /**
     * Metadata for auditing
     */
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime lastModifiedAt;
    private String lastModifiedBy;

    @Override
    public String toString() {
        return String.format("%s - %s: %s", 
            startTime.toLocalDate(), 
            patient != null ? patient.toString() : "Kein Patient", 
            reason);
    }
}
