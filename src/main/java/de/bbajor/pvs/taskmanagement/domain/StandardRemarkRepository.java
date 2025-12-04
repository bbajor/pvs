package de.bbajor.pvs.taskmanagement.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository für Standardbemerkungen.
 */
public interface StandardRemarkRepository extends JpaRepository<StandardRemark, Long> {

    /**
     * Findet alle Standardbemerkungen für eine Institution, sortiert nach sortOrder und Text.
     */
    @Query("SELECT sr FROM StandardRemark sr WHERE sr.institution.id = :institutionId ORDER BY sr.sortOrder ASC NULLS LAST, sr.text ASC")
    List<StandardRemark> findByInstitutionIdOrderBySortOrderAscTextAsc(@Param("institutionId") Long institutionId);
}

