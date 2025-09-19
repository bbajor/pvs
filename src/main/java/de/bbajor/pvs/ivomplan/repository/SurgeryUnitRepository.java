package de.bbajor.pvs.ivomplan.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.ivomplan.model.SurgeryUnit;

public interface SurgeryUnitRepository
        extends JpaRepository<SurgeryUnit, Integer>, JpaSpecificationExecutor<SurgeryUnit> {

    Slice<SurgeryUnit> findAllBy(Pageable pageable);
}
