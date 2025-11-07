package de.bbajor.pvs.institution.model;

/**
 * Enum for email encryption methods.
 * Defines how emails should be encrypted when sent to a contact.
 */
public enum EmailEncryptionMethod {
    
    /**
     * No encryption - emails are sent in plain text.
     * ⚠️ WARNING: Not recommended for sensitive data!
     */
    NONE("Keine Verschlüsselung", "⚠️ Nicht empfohlen - E-Mails werden unverschlüsselt versendet"),
    
    /**
     * OpenPGP encryption using PGP/MIME format.
     * Standard encryption method for email.
     */
    OPENPGP("OpenPGP (PGP/MIME)", "✅ Empfohlen - Standard-Verschlüsselungsmethode"),
    
    /**
     * S/MIME encryption using X.509 certificates.
     * Alternative encryption method, often used in corporate environments.
     */
    SMIME("S/MIME", "✅ Empfohlen - Alternative Verschlüsselungsmethode");
    
    private final String displayName;
    private final String description;
    
    EmailEncryptionMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}

