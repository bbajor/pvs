package de.bbajor.pvs.surgicalcenter.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;

public interface SurgicalCenterTimeSlotRepository
                extends JpaRepository<SurgicalCenterTimeSlot, Long>, JpaSpecificationExecutor<SurgicalCenterTimeSlot> {

        Slice<SurgicalCenterTimeSlot> findAllBy(Pageable pageable);

        List<SurgicalCenterTimeSlot> findBySurgicalCenter(SurgicalCenter surgicalCenter);

        List<SurgicalCenterTimeSlot> findBySurgicalCenterAndDateGreaterThanEqual(SurgicalCenter surgicalCenter,
                        LocalDate date);

        List<SurgicalCenterTimeSlot> findByDateBetween(LocalDate start, LocalDate end, Sort sort);

        List<SurgicalCenterTimeSlot> findByDateBetweenAndSurgicalCenter(LocalDate start, LocalDate end,
                        SurgicalCenter surgicalCenter, Sort sort);

        @Query("""
                                        select ts from SurgicalCenterTimeSlot ts
                                        where ts.surgicalCenter.id = :id
                                        order by ts.date asc, ts.startTime asc
                        """)
        List<SurgicalCenterTimeSlot> findBySurgicalCenterIdWithTreatmentCount(Integer id);

        @Query("""
                                        select count(t)
                                        from Treatment t
                                        where t.surgicalCenterTimeSlot.id = :timeSlotId
                        """)
        int getPatientCount(Long timeSlotId);
}
