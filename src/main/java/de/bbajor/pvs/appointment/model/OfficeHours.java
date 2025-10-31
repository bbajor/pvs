package de.bbajor.pvs.appointment.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Office hours for an appointment scheduler.
 * Defines when appointments can be scheduled.
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "office_hours")
public class OfficeHours extends BasicEntity<Long> {

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private AppointmentScheduler scheduler;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @NotNull
    @Column(nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Slot duration in minutes (e.g., 15, 30, 60 minutes per appointment)
     */
    @Column(nullable = false)
    private Integer slotDurationMinutes = 30;

    @Override
    public String toString() {
        return String.format("%s: %s - %s", dayOfWeek, startTime, endTime);
    }
}
