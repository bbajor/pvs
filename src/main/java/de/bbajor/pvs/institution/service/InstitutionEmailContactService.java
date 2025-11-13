package de.bbajor.pvs.institution.service;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.model.InstitutionEmailContact;
import de.bbajor.pvs.institution.repository.InstitutionEmailContactRepository;
import de.bbajor.pvs.security.email.service.OpenPgpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing institution email contacts with OpenPGP keys.
 */
@Service
public class InstitutionEmailContactService {

    private static final Logger log = LoggerFactory.getLogger(InstitutionEmailContactService.class);

    private final InstitutionEmailContactRepository repository;
    private final OpenPgpService openPgpService;

    public InstitutionEmailContactService(
            InstitutionEmailContactRepository repository,
            OpenPgpService openPgpService) {
        this.repository = repository;
        this.openPgpService = openPgpService;
    }

    /**
     * Finds all email contacts for an institution.
     */
    @Transactional(readOnly = true)
    public List<InstitutionEmailContact> findByInstitution(Institution institution) {
        return repository.findByInstitution(institution);
    }

    /**
     * Finds all active email contacts for an institution.
     */
    @Transactional(readOnly = true)
    public List<InstitutionEmailContact> findActiveByInstitution(Institution institution) {
        return repository.findByInstitutionAndActiveTrue(institution);
    }

    /**
     * Saves an email contact and extracts OpenPGP key information if provided.
     */
    @Transactional
    public InstitutionEmailContact save(InstitutionEmailContact contact) {
        // If OpenPGP key is provided, extract key ID and fingerprint
        if (contact.getOpenpgpPublicKey() != null && !contact.getOpenpgpPublicKey().trim().isEmpty()) {
            try {
                String keyId = openPgpService.extractKeyId(contact.getOpenpgpPublicKey());
                String fingerprint = openPgpService.extractFingerprint(contact.getOpenpgpPublicKey());
                contact.setKeyId(keyId);
                contact.setKeyFingerprint(fingerprint);
                log.info("OpenPGP key extracted for email {}: Key ID={}, Fingerprint={}", 
                        contact.getEmail(), keyId, fingerprint);
            } catch (Exception e) {
                log.error("Failed to extract OpenPGP key information for email {}", contact.getEmail(), e);
                throw new IllegalArgumentException("Ungültiger OpenPGP-Schlüssel: " + e.getMessage(), e);
            }
        }

        return repository.save(contact);
    }

    /**
     * Deletes an email contact.
     */
    @Transactional
    public void delete(InstitutionEmailContact contact) {
        repository.delete(contact);
    }

    /**
     * Finds an email contact by email address and institution.
     */
    @Transactional(readOnly = true)
    public InstitutionEmailContact findByEmailAndInstitution(String email, Institution institution) {
        return repository.findByEmailAndInstitution(email, institution).orElse(null);
    }

    /**
     * Validates an OpenPGP public key.
     */
    public boolean isValidOpenPgpKey(String armoredKey) {
        return openPgpService.isValidPublicKey(armoredKey);
    }
}

