package de.bbajor.pvs.cost.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.cost.model.PatientCostHistory;

/**
 * Repository für PatientCostHistory-Entities.
 */
public interface PatientCostHistoryRepository extends JpaRepository<PatientCostHistory, Long> {

    /**
     * Findet Kostenhistorie für einen Patienten.
     * <p>
     * Data isolation: Filtert nach Institution.
     * </p>
     */
    @Query("""
        SELECT pch FROM PatientCostHistory pch
        WHERE pch.patient.id = :patientId
        AND pch.patient.institution.id = :institutionId
        ORDER BY pch.treatmentDate DESC
        """)
    List<PatientCostHistory> findByPatientId(
        @Param("patientId") Integer patientId,
        @Param("institutionId") Long institutionId
    );

    /**
     * Berechnet Gesamtkosten für einen Patienten.
     * <p>
     * Data isolation: Filtert nach Institution.
     * </p>
     */
    @Query("""
        SELECT SUM(pch.costAmount) FROM PatientCostHistory pch
        WHERE pch.patient.id = :patientId
        AND pch.patient.institution.id = :institutionId
        """)
    Optional<BigDecimal> getTotalCostsByPatientId(
        @Param("patientId") Integer patientId,
        @Param("institutionId") Long institutionId
    );
}

