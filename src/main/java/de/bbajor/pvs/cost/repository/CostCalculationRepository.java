package de.bbajor.pvs.cost.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.cost.model.CostCalculation;

/**
 * Repository für CostCalculation-Entities.
 */
public interface CostCalculationRepository extends JpaRepository<CostCalculation, Long> {

    /**
     * Findet aktive Preismodelle für einen OP-Saal zum gegebenen Datum.
     * <p>
     * Data isolation: Filtert nach Institution.
     * </p>
     */
    @Query("""
        SELECT c FROM CostCalculation c
        WHERE c.surgicalCenter.id = :surgicalCenterId
        AND c.institution.id = :institutionId
        AND c.active = true
        AND c.validFrom <= :date
        AND (c.validTo IS NULL OR c.validTo >= :date)
        ORDER BY c.validFrom DESC
        """)
    List<CostCalculation> findActiveBySurgicalCenterAndDate(
        @Param("surgicalCenterId") Integer surgicalCenterId,
        @Param("institutionId") Long institutionId,
        @Param("date") LocalDate date
    );

    /**
     * Findet alle Preismodelle für einen OP-Saal.
     * <p>
     * Data isolation: Filtert nach Institution.
     * </p>
     */
    @Query("""
        SELECT c FROM CostCalculation c
        WHERE c.surgicalCenter.id = :surgicalCenterId
        AND c.institution.id = :institutionId
        ORDER BY c.validFrom DESC
        """)
    List<CostCalculation> findBySurgicalCenterIdAndInstitutionId(
        @Param("surgicalCenterId") Integer surgicalCenterId,
        @Param("institutionId") Long institutionId
    );
}

