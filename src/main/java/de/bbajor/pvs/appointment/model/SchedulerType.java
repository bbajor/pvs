package de.bbajor.pvs.appointment.model;

/**
 * Type of appointment scheduler to distinguish different use cases.
 */
public enum SchedulerType {
    /**
     * Scheduler for a specific doctor
     */
    DOCTOR,

    /**
     * Scheduler for medical staff (MFA)
     */
    MEDICAL_STAFF,

    /**
     * Scheduler for pre-examinations
     */
    PRE_EXAMINATION,

    /**
     * Generic scheduler for other purposes
     */
    GENERAL
}
