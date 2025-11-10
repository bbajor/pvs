package de.bbajor.pvs.institution.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.institution.model.InstitutionSettings;

@Repository
public interface InstitutionSettingsRepository extends JpaRepository<InstitutionSettings, Long> {

    Optional<InstitutionSettings> findByInstitutionInstitutionCode(String institutionCode);

    boolean existsByInstitutionInstitutionCode(String institutionCode);
}
