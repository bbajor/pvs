package de.bbajor.pvs.surgicalcenter.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;

public interface SurgicalCenterRepository
        extends JpaRepository<SurgicalCenter, Integer>, JpaSpecificationExecutor<SurgicalCenter> {

    Slice<SurgicalCenter> findAllBy(Pageable pageable);

    @Query("SELECT DISTINCT sc FROM SurgicalCenter sc " +
            "LEFT JOIN FETCH sc.availableTimeSlots scts " +
            "WHERE sc.id = :id " +
            "ORDER BY scts.date ASC")
    Optional<SurgicalCenter> findByIdWithDetails(@Param("id") Integer id);

    /**
     * Find all surgical centers for an institution.
     * <p>
     * Data isolation: All filtering is done via institution.
     * SurgicalCenter → Institution (primary path).
     * </p>
     */
    @Query("SELECT sc FROM SurgicalCenter sc WHERE sc.institution.id = :institutionId")
    List<SurgicalCenter> findByInstitutionId(@Param("institutionId") Long institutionId);
    
    /**
     * Find all surgical centers for an institution with paging.
     * <p>
     * Data isolation: All filtering is done via institution.
     * SurgicalCenter → Institution (primary path).
     * </p>
     */
    @Query("SELECT sc FROM SurgicalCenter sc WHERE sc.institution.id = :institutionId")
    Slice<SurgicalCenter> findByInstitutionId(@Param("institutionId") Long institutionId, Pageable pageable);
}
