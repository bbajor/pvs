package de.bbajor.pvs.security.email.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.institution.model.EmailEncryptionMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity for storing SMTP server configuration.
 * Only one configuration should exist (singleton pattern via application logic).
 */
@Entity
@Table(name = "smtp_config")
@Getter
@Setter
public class SmtpConfig extends BasicEntity<Long> {

    @Column(name = "host", nullable = false, length = 255)
    private String host;

    @Column(name = "port", nullable = false)
    private Integer port = 587;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "password", length = 500) // Encrypted password
    private String password;

    @Column(name = "from_address", length = 255)
    private String fromAddress;

    @Column(name = "security_method", length = 20)
    @Enumerated(EnumType.STRING)
    private SmtpSecurityMethod securityMethod = SmtpSecurityMethod.STARTTLS;

    @Column(name = "use_tls", nullable = false)
    private Boolean useTls = true; // Deprecated, use securityMethod instead

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    /**
     * OpenPGP private key in ASCII-armored format (encrypted).
     * This is the private key used to sign outgoing emails.
     */
    @Column(name = "openpgp_private_key", columnDefinition = "TEXT")
    private String openpgpPrivateKey;

    /**
     * Passphrase for the OpenPGP private key (encrypted).
     * Required if the private key is password-protected.
     */
    @Column(name = "openpgp_private_key_passphrase", length = 500)
    private String openpgpPrivateKeyPassphrase;

    /**
     * OpenPGP public key in ASCII-armored format (optional, not encrypted).
     * This is the public key used for the Autocrypt header in signed emails.
     * If not provided, it will be extracted from the private key at runtime.
     * 
     * <p>
     * Storing the public key separately improves performance and Thunderbird compatibility.
     * Thunderbird sends the public key in email headers (Autocrypt) for signature recognition.
     * </p>
     */
    @Column(name = "openpgp_public_key", columnDefinition = "TEXT")
    private String openpgpPublicKey;

    /**
     * Default email encryption method for outgoing emails.
     * This is used when no specific encryption method is configured for a recipient.
     * Can be overridden per email contact in InstitutionEmailContact.
     */
    @Column(name = "default_encryption_method", length = 20)
    @Enumerated(EnumType.STRING)
    private EmailEncryptionMethod defaultEncryptionMethod = EmailEncryptionMethod.NONE;
}

