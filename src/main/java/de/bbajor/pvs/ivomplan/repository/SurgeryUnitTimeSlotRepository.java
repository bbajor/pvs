package de.bbajor.pvs.ivomplan.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.ivomplan.model.SurgeryUnit;
import de.bbajor.pvs.ivomplan.model.SurgeryUnitTimeSlot;

public interface SurgeryUnitTimeSlotRepository
        extends JpaRepository<SurgeryUnitTimeSlot, Integer>, JpaSpecificationExecutor<SurgeryUnitTimeSlot> {

    Slice<SurgeryUnitTimeSlot> findAllBy(Pageable pageable);

    List<SurgeryUnitTimeSlot> findBySurgeryUnit(SurgeryUnit surgeryUnit);

    List<SurgeryUnitTimeSlot> findByDateBetween(LocalDate start, LocalDate end, Sort sort);

    List<SurgeryUnitTimeSlot> findByDateBetweenAndSurgeryUnit(LocalDate start, LocalDate end, SurgeryUnit surgeryUnit, Sort sort);
}
