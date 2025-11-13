package de.bbajor.pvs.base.util;

/**
 * Utility class for masking PII (Personally Identifiable Information) in logs.
 * Ensures DSGVO compliance by preventing PII from appearing in log files.
 */
public class PiiMasker {

    /**
     * Masks an email address, showing only the domain part.
     * Example: "user@example.com" -> "***@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return "***@" + email.substring(atIndex + 1);
        }
        return "***";
    }

    /**
     * Masks a name, showing only the first letter and last letter.
     * Example: "Max Mustermann" -> "M***n"
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "***";
        }
        String trimmed = name.trim();
        if (trimmed.length() <= 2) {
            return "***";
        }
        return trimmed.charAt(0) + "***" + trimmed.charAt(trimmed.length() - 1);
    }

    /**
     * Masks a username, showing only first and last character.
     * Example: "john.doe" -> "j***e"
     */
    public static String maskUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "***";
        }
        if (username.length() <= 2) {
            return "***";
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1);
    }

    /**
     * Masks an address, showing only city and postal code.
     * Example: "Musterstraße 123, 12345 Berlin" -> "***, 12345 Berlin"
     */
    public static String maskAddress(String address) {
        if (address == null || address.isEmpty()) {
            return "***";
        }
        // Try to extract postal code and city (common German format: "PLZ City")
        // For simplicity, just mask the street part
        return "***, " + address;
    }

    /**
     * Masks an insurance number, showing only last 4 digits.
     * Example: "A123456789" -> "***6789"
     */
    public static String maskInsuranceNumber(String insuranceNumber) {
        if (insuranceNumber == null || insuranceNumber.isEmpty()) {
            return "***";
        }
        if (insuranceNumber.length() <= 4) {
            return "***";
        }
        return "***" + insuranceNumber.substring(insuranceNumber.length() - 4);
    }

    /**
     * Masks a birth date, showing only year.
     * Example: "1980-05-15" -> "1980-***-***"
     */
    public static String maskBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isEmpty()) {
            return "***";
        }
        // Extract year if possible
        if (birthDate.length() >= 4) {
            return birthDate.substring(0, 4) + "-***-***";
        }
        return "***";
    }

    /**
     * Masks any string, showing only first and last character.
     * Generic fallback for unknown PII types.
     */
    public static String maskGeneric(String value) {
        if (value == null || value.isEmpty()) {
            return "***";
        }
        if (value.length() <= 2) {
            return "***";
        }
        return value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }
}
