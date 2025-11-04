package de.bbajor.pvs.institution.service;

/**
 * Exception thrown when a cross-institution access violation is detected.
 */
public class InstitutionAccessViolationException extends RuntimeException {

    public InstitutionAccessViolationException(String message) {
        super(message);
    }

    public InstitutionAccessViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}

