package de.bbajor.pvs.ivomplan.repository;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.ivomplan.model.SurgeryUnit;

public interface SurgeryUnitRepository
        extends JpaRepository<SurgeryUnit, Integer>, JpaSpecificationExecutor<SurgeryUnit> {

    Slice<SurgeryUnit> findAllBy(Pageable pageable);

    @Query("SELECT DISTINCT su FROM SurgeryUnit su " +
            "LEFT JOIN FETCH su.surgeryUnitAddress " +
            "LEFT JOIN FETCH su.availableTimeSlots " +
            "WHERE su.id = :id")
    Optional<SurgeryUnit> findByIdWithDetails(@Param("id") Integer id);
}
