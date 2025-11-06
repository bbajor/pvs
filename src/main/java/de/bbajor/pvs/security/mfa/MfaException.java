package de.bbajor.pvs.security.mfa;

/**
 * Exception thrown when MFA operations fail.
 */
public class MfaException extends RuntimeException {

    public MfaException(String message) {
        super(message);
    }

    public MfaException(String message, Throwable cause) {
        super(message, cause);
    }
}
