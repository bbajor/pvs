package de.bbajor.pvs.cost.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.cost.model.TreatmentCost;

/**
 * Repository für TreatmentCost-Entities.
 */
public interface TreatmentCostRepository extends JpaRepository<TreatmentCost, Long> {

    /**
     * Findet Kosten für eine Behandlung.
     */
    Optional<TreatmentCost> findByTreatmentId(Long treatmentId);

    /**
     * Findet alle Kosten für Behandlungen in einem Zeitraum.
     * <p>
     * Data isolation: Filtert nach Institution.
     * </p>
     */
    @Query("""
        SELECT tc FROM TreatmentCost tc
        JOIN tc.treatment t
        JOIN t.surgicalCenterTimeSlot ts
        WHERE ts.surgicalCenter.institution.id = :institutionId
        AND ts.date BETWEEN :startDate AND :endDate
        ORDER BY ts.date DESC, ts.startTime DESC
        """)
    List<TreatmentCost> findByInstitutionAndDateRange(
        @Param("institutionId") Long institutionId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Aggregiert monatliche Kosten.
     * <p>
     * Data isolation: Filtert nach Institution.
     * </p>
     * <p>
     * Returns: Object[] mit [0] = month (String "YYYY-MM"), [1] = totalCost (BigDecimal)
     * </p>
     */
    @Query("""
        SELECT FUNCTION('TO_CHAR', ts.date, 'YYYY-MM') as month,
               SUM(tc.totalCost) as totalCost
        FROM TreatmentCost tc
        JOIN tc.treatment t
        JOIN t.surgicalCenterTimeSlot ts
        WHERE ts.surgicalCenter.institution.id = :institutionId
        AND ts.date >= :startDate
        GROUP BY FUNCTION('TO_CHAR', ts.date, 'YYYY-MM')
        ORDER BY month ASC
        """)
    List<Object[]> getMonthlyCosts(
        @Param("institutionId") Long institutionId,
        @Param("startDate") LocalDate startDate
    );

    /**
     * Zählt Behandlungen in einem Monat für einen OP-Saal.
     * <p>
     * Data isolation: Filtert nach Institution.
     * </p>
     */
    @Query("""
        SELECT COUNT(t) FROM Treatment t
        JOIN t.surgicalCenterTimeSlot ts
        WHERE ts.surgicalCenter.id = :surgicalCenterId
        AND ts.surgicalCenter.institution.id = :institutionId
        AND FUNCTION('TO_CHAR', ts.date, 'YYYY-MM') = :month
        """)
    Long countTreatmentsInMonth(
        @Param("surgicalCenterId") Integer surgicalCenterId,
        @Param("institutionId") Long institutionId,
        @Param("month") String month
    );
}

