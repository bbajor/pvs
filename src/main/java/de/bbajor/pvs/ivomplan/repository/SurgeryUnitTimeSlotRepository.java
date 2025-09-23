package de.bbajor.pvs.ivomplan.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.ivomplan.model.SurgeryUnit;
import de.bbajor.pvs.ivomplan.model.SurgeryUnitTimeSlot;
import java.util.List;


public interface SurgeryUnitTimeSlotRepository
        extends JpaRepository<SurgeryUnitTimeSlot, Integer>, JpaSpecificationExecutor<SurgeryUnitTimeSlot> {

    Slice<SurgeryUnit> findAllBy(Pageable pageable);

    List<SurgeryUnitTimeSlot> findBySurgeryUnit(SurgeryUnit surgeryUnit);
}
