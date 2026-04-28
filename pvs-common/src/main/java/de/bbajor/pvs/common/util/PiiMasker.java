package de.bbajor.pvs.common.util;

/**
 * Utility class for masking PII (Personally Identifiable Information) in logs and responses.
 * 
 * DSGVO-compliant masking of sensitive data.
 * Moved from base.util to common for use across all microservices.
 */
public class PiiMasker {
    
    private static final String MASK_CHAR = "*";
    private static final int MIN_VISIBLE_CHARS = 2;
    
    /**
     * Mask a name (first or last name).
     * Shows first 2 characters, masks the rest.
     * 
     * @param name the name to mask
     * @return masked name, or null if input is null
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() <= MIN_VISIBLE_CHARS) {
            return MASK_CHAR.repeat(name.length());
        }
        return name.substring(0, MIN_VISIBLE_CHARS) + MASK_CHAR.repeat(name.length() - MIN_VISIBLE_CHARS);
    }
    
    /**
     * Mask a birth date.
     * Shows only year, masks day and month.
     * 
     * @param birthDate the birth date string (format: YYYY-MM-DD)
     * @return masked birth date, or null if input is null
     */
    public static String maskBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isEmpty()) {
            return birthDate;
        }
        // Format: YYYY-MM-DD -> YYYY-**-**
        String[] parts = birthDate.split("-");
        if (parts.length == 3) {
            return parts[0] + "-**-**";
        }
        return MASK_CHAR.repeat(birthDate.length());
    }
    
    /**
     * Mask an insurance number.
     * Shows first 2 and last 2 characters, masks the middle.
     * 
     * @param insuranceNumber the insurance number to mask
     * @return masked insurance number, or null if input is null
     */
    public static String maskInsuranceNumber(String insuranceNumber) {
        if (insuranceNumber == null || insuranceNumber.isEmpty()) {
            return insuranceNumber;
        }
        if (insuranceNumber.length() <= 4) {
            return MASK_CHAR.repeat(insuranceNumber.length());
        }
        return insuranceNumber.substring(0, 2) + 
               MASK_CHAR.repeat(insuranceNumber.length() - 4) + 
               insuranceNumber.substring(insuranceNumber.length() - 2);
    }
    
    /**
     * Mask an email address.
     * Shows first 2 characters of local part, masks the rest.
     * 
     * @param email the email address to mask
     * @return masked email, or null if input is null
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex == -1) {
            return maskName(email);
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (localPart.length() <= MIN_VISIBLE_CHARS) {
            return MASK_CHAR.repeat(localPart.length()) + domain;
        }
        return localPart.substring(0, MIN_VISIBLE_CHARS) + 
               MASK_CHAR.repeat(localPart.length() - MIN_VISIBLE_CHARS) + 
               domain;
    }
}


