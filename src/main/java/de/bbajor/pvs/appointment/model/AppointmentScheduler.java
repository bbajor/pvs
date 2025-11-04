package de.bbajor.pvs.appointment.model;

import java.util.ArrayList;
import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.location.model.Location;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Entity representing an appointment scheduler for a location.
 * Each location can have multiple schedulers (e.g., per doctor, for MFA, for pre-examinations).
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "appointment_scheduler")
public class AppointmentScheduler extends BasicEntity<Long> {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * The location this scheduler belongs to.
     * Data isolation is ensured via location -> institution relationship.
     * 
     * This replaces the old "practice" field.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Type of scheduler to distinguish different use cases
     */
    @Column(nullable = false)
    private SchedulerType type = SchedulerType.DOCTOR;

    @OneToMany(mappedBy = "scheduler", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchedulerAssignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "scheduler", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OfficeHours> officeHours = new ArrayList<>();

    @OneToMany(mappedBy = "scheduler", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Appointment> appointments = new ArrayList<>();

    @Override
    public String toString() {
        if (location != null) {
            return String.format("%s (%s)", name, location.getLocationName());
        }
        return name;
    }
}
