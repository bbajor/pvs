package de.bbajor.pvs.taskmanagement.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository für Behandlungsbemerkungen.
 */
public interface TreatmentRemarkRepository extends JpaRepository<TreatmentRemark, Long> {

    /**
     * Findet alle Bemerkungen für eine Behandlung, sortiert nach sortOrder und Text.
     */
    @Query("SELECT tr FROM TreatmentRemark tr WHERE tr.treatment.id = :treatmentId ORDER BY tr.sortOrder ASC NULLS LAST, tr.text ASC")
    List<TreatmentRemark> findByTreatmentIdOrderBySortOrderAscTextAsc(@Param("treatmentId") Long treatmentId);
}

