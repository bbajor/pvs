package de.bbajor.pvs.appointment.model;

/**
 * Status of an appointment.
 */
public enum AppointmentStatus {
    /**
     * Appointment is scheduled (confirmed)
     */
    SCHEDULED,

    /**
     * Patient has arrived and is waiting
     */
    ARRIVED,

    /**
     * Appointment is currently in progress
     */
    IN_PROGRESS,

    /**
     * Appointment has been completed
     */
    COMPLETED,

    /**
     * Appointment was cancelled by patient or practice
     */
    CANCELLED,

    /**
     * Patient did not show up (no-show)
     */
    NO_SHOW
}
