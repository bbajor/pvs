package de.bbajor.pvs.surgicalcenter.repository;

import java.time.LocalDate;
import java.time.LocalTime;
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
                                        select new de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot(
                                            ts.id, ts.version, ts.description, ts.date, ts.startTime, ts.endTime,
                                            ts.surgicalCenter, ts.isAvailable, ts.isApproved, count(t) as patientCount
                                        )
                                        from SurgicalCenterTimeSlot ts
                                        left join Treatment t on t.surgicalCenterTimeSlot = ts
                                        where ts.surgicalCenter.id = :id
                                        group by ts.id, ts.version, ts.description, ts.date, ts.startTime, ts.endTime,
                                                 ts.surgicalCenter, ts.isAvailable, ts.isApproved
                                        order by ts.date asc, ts.startTime asc
                        """)
        List<SurgicalCenterTimeSlot> findBySurgicalCenterIdWithTreatmentCount(Integer id);

        @Query("""
                        SELECT ts FROM SurgicalCenterTimeSlot ts
                        LEFT JOIN FETCH ts.surgicalCenter sc
                        WHERE ts.id NOT IN :timeSlotIds
                        AND EXISTS (
                            SELECT t FROM Treatment t
                            WHERE t.surgicalCenterTimeSlot = ts
                            AND t.approvalDate is NULL
                        )
                """)
        List<SurgicalCenterTimeSlot> findAllContainingNotApprovedTreatmentsAndNotInTimeSlotIdList(
                List<Long> timeSlotIds);

        boolean existsBySurgicalCenterAndDateAndStartTimeAndEndTime(
                SurgicalCenter surgicalCenter,
                LocalDate date,
                LocalTime startTime,
                LocalTime endTime);
}
