package de.bbajor.pvs.patient.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.patient.model.HealthInsurance;

public interface HealthInsuranceRepository
        extends JpaRepository<HealthInsurance, Integer>, JpaSpecificationExecutor<HealthInsurance> {

    Slice<HealthInsurance> findAllBy(Pageable pageable);
    
    /**
     * Findet alle Versicherungen für eine Institution.
     */
    @Query("SELECT h FROM HealthInsurance h WHERE h.institution.id = :institutionId AND h.active = true")
    List<HealthInsurance> findByInstitutionId(@Param("institutionId") Long institutionId);

    /**
     * Prüft, ob es innerhalb einer Institution bereits eine Versicherung gibt,
     * die anhand Kostenträgername, Kostenträger-ID oder Abrechnungsstellen-ID
     * als Dublette gewertet werden muss.
     *
     * - Beim Bearbeiten wird der aktuelle Datensatz (currentId) ausgeschlossen.
     * - Nur übergebene Kriterien werden berücksichtigt.
     */
    @Query("""
            SELECT (COUNT(h) > 0)
            FROM HealthInsurance h
            WHERE h.institution.id = :institutionId
              AND h.active = true
              AND (:currentId IS NULL OR h.id <> :currentId)
              AND (
                   (:costCarrierName IS NOT NULL AND LOWER(h.costCarrierName) = LOWER(:costCarrierName))
                OR (:costCarrierId IS NOT NULL AND h.costCarrierId = :costCarrierId)
                OR (:billingCarrierId IS NOT NULL AND h.billingCarrierId = :billingCarrierId)
              )
            """)
    boolean existsDuplicateForInstitution(
            @Param("institutionId") Long institutionId,
            @Param("currentId") Integer currentId,
            @Param("costCarrierName") String costCarrierName,
            @Param("costCarrierId") String costCarrierId,
            @Param("billingCarrierId") String billingCarrierId);
}