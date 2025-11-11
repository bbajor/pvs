package de.bbajor.pvs.institution.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.institution.persistence.InstitutionFilterConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Entity for storing email contacts of institutions with their OpenPGP public keys.
 * 
 * <p>
 * This allows encrypted email communication with institutions.
 * Each email address can have an associated OpenPGP public key for encryption.
 * </p>
 */
@Entity
@Table(name = "institution_email_contact")
@Getter
@Setter
@Filter(name = InstitutionFilterConstants.FILTER_NAME, condition = InstitutionFilterConstants.FILTER_CONDITION)
public class InstitutionEmailContact extends BasicEntity<Long> {

    /**
     * The institution this email contact belongs to.
     * Can be null for system-wide contacts (e.g., Super-Admin recovery email).
     */
    @ManyToOne
    @JoinColumn(name = "institution_id", nullable = true)
    private Institution institution;

    /**
     * Email address for this contact.
     */
    @Email
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Display name for this contact (e.g., "Hauptkontakt", "IT-Support").
     */
    @Column(name = "display_name", length = 255)
    private String displayName;

    /**
     * OpenPGP public key in ASCII-armored format.
     * This is the public key used to encrypt emails sent to this address.
     */
    @Column(name = "openpgp_public_key", columnDefinition = "TEXT")
    private String openpgpPublicKey;

    /**
     * Key ID of the OpenPGP key (for quick lookup).
     * Format: 16 hex characters (e.g., "A1B2C3D4E5F6G7H8").
     */
    @Column(name = "key_id", length = 16)
    private String keyId;

    /**
     * Fingerprint of the OpenPGP key (for verification).
     * Format: 40 hex characters (e.g., "A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0").
     */
    @Column(name = "key_fingerprint", length = 40)
    private String keyFingerprint;

    /**
     * Whether this email contact is active.
     * Inactive contacts are not used for sending emails.
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Email encryption method to use when sending emails to this contact.
     * Defaults to OPENPGP if a public key is available, otherwise NONE.
     */
    @Column(name = "encryption_method", length = 20)
    @Enumerated(EnumType.STRING)
    private EmailEncryptionMethod encryptionMethod;

    /**
     * S/MIME certificate in PEM format (for S/MIME encryption).
     * Required if encryptionMethod is SMIME.
     */
    @Column(name = "smime_certificate", columnDefinition = "TEXT")
    private String smimeCertificate;

    /**
     * Notes about this contact (e.g., "Primary contact for password resets").
     */
    @Column(name = "notes", length = 1000)
    private String notes;
}

