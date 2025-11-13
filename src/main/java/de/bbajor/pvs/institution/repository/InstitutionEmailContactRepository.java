package de.bbajor.pvs.institution.repository;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.model.InstitutionEmailContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstitutionEmailContactRepository extends JpaRepository<InstitutionEmailContact, Long> {
    
    /**
     * Finds all email contacts for a specific institution.
     */
    List<InstitutionEmailContact> findByInstitution(Institution institution);
    
    /**
     * Finds all active email contacts for a specific institution.
     */
    List<InstitutionEmailContact> findByInstitutionAndActiveTrue(Institution institution);
    
    /**
     * Finds an email contact by email address and institution.
     */
    Optional<InstitutionEmailContact> findByEmailAndInstitution(String email, Institution institution);
    
    /**
     * Finds an email contact by email address (across all institutions).
     */
    Optional<InstitutionEmailContact> findByEmail(String email);
}

