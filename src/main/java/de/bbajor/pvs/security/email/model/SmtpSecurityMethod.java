package de.bbajor.pvs.security.email.model;

/**
 * Enum for SMTP security methods.
 */
public enum SmtpSecurityMethod {
    /**
     * No encryption - plain connection (usually port 25)
     */
    NONE("Keine Verschlüsselung", 25),
    
    /**
     * STARTTLS - upgrade plain connection to TLS (usually port 587)
     */
    STARTTLS("STARTTLS", 587),
    
    /**
     * SSL/TLS - direct SSL/TLS connection (usually port 465)
     */
    SSL_TLS("SSL/TLS", 465);

    private final String displayName;
    private final int defaultPort;

    SmtpSecurityMethod(String displayName, int defaultPort) {
        this.displayName = displayName;
        this.defaultPort = defaultPort;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultPort() {
        return defaultPort;
    }
}

