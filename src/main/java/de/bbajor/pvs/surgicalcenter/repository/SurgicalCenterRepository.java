package de.bbajor.pvs.surgicalcenter.repository;

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
            "LEFT JOIN FETCH sc.surgicalCenterAddress " +
            "LEFT JOIN FETCH sc.availableTimeSlots scts " +
            "WHERE sc.id = :id " +
            "ORDER BY scts.date ASC")
    Optional<SurgicalCenter> findByIdWithDetails(@Param("id") Integer id);
}
