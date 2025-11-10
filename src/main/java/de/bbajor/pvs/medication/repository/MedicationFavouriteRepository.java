package de.bbajor.pvs.medication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.medication.model.MedicationFavourite;

public interface MedicationFavouriteRepository extends JpaRepository<MedicationFavourite, Long> {

    List<MedicationFavourite> findByInstitutionIdAndActiveTrue(Long institutionId);

    @Query("SELECT mf FROM MedicationFavourite mf " +
           "LEFT JOIN FETCH mf.medication " +
           "WHERE mf.institution.id = :institutionId AND mf.active = true")
    List<MedicationFavourite> findByInstitutionIdAndActiveTrueWithMedication(@Param("institutionId") Long institutionId);

    Optional<MedicationFavourite> findByInstitutionIdAndMedicationId(Long institutionId, Long medicationId);

    List<MedicationFavourite> findByMedicationId(Long medicationId);

    List<MedicationFavourite> findByMedicationIdAndActiveTrue(Long medicationId);
}

