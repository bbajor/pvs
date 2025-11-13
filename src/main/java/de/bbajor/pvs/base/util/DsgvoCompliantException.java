package de.bbajor.pvs.base.util;

/**
 * Exception that ensures DSGVO-compliant error messages.
 * Never includes PII in the message.
 */
public class DsgvoCompliantException extends RuntimeException {

    public DsgvoCompliantException(String message) {
        super(message);
    }

    public DsgvoCompliantException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a DSGVO-compliant error message for user-facing errors.
     * Never includes PII like names, emails, or personal data.
     */
    public static String createUserMessage(String operation, String entityType) {
        return String.format("Fehler bei %s von %s. Bitte versuchen Sie es erneut oder kontaktieren Sie den Administrator.", 
                operation, entityType);
    }

    /**
     * Creates a DSGVO-compliant error message for validation errors.
     */
    public static String createValidationMessage(String field) {
        return String.format("Validierungsfehler: %s ist ungültig.", field);
    }
}
