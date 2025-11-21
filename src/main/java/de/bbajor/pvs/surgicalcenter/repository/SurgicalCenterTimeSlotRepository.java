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
import org.springframework.data.repository.query.Param;

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
        
        /**
         * Findet verfügbare Termine für einen Behandlungsort im Zeitraum.
         * Filtert direkt nach Institution für bessere Datenisolation.
         * Lädt auch die Patientenzahl pro Timeslot.
         */
        @Query("""
                select new de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot(
                    ts.id, ts.version, ts.description, ts.date, ts.startTime, ts.endTime,
                    ts.surgicalCenter, ts.isAvailable, ts.isApproved, count(t) as patientCount
                )
                from SurgicalCenterTimeSlot ts
                left join Treatment t on t.surgicalCenterTimeSlot = ts
                where ts.surgicalCenter.id = :surgicalCenterId
                and ts.surgicalCenter.institution.id = :institutionId
                and ts.date between :start and :end
                and ts.isAvailable = true
                group by ts.id, ts.version, ts.description, ts.date, ts.startTime, ts.endTime,
                         ts.surgicalCenter, ts.isAvailable, ts.isApproved
                order by ts.date asc, ts.startTime asc
                """)
        List<SurgicalCenterTimeSlot> findAvailableTimeSlotsBySurgicalCenterAndInstitution(
                @Param("surgicalCenterId") Integer surgicalCenterId,
                @Param("institutionId") Long institutionId,
                @Param("start") LocalDate start,
                @Param("end") LocalDate end);

        @Query("""
                                        select new de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot(
                                            ts.id, ts.version, ts.description, ts.date, ts.startTime, ts.endTime,
                                            ts.surgicalCenter, ts.isAvailable, ts.isApproved, count(t) as patientCount
                                        )
                                        from SurgicalCenterTimeSlot ts
                                        left join Treatment t on t.surgicalCenterTimeSlot = ts
                                        left join t.treatmentPlan tp
                                        left join tp.patient p
                                        left join p.location loc
                                        where ts.surgicalCenter.id = :id
                                        and (t.id is null or loc.institution.id = :institutionId)
                                        group by ts.id, ts.version, ts.description, ts.date, ts.startTime, ts.endTime,
                                                 ts.surgicalCenter, ts.isAvailable, ts.isApproved
                                        order by ts.date asc, ts.startTime asc
                        """)
        List<SurgicalCenterTimeSlot> findBySurgicalCenterIdWithTreatmentCount(@Param("id") Integer id, @Param("institutionId") Long institutionId);

        @Query("""
                        SELECT ts FROM SurgicalCenterTimeSlot ts
                        LEFT JOIN FETCH ts.surgicalCenter sc
                        WHERE ts.id NOT IN :timeSlotIds
                        AND sc.institution.id = :institutionId
                        AND ts.date <= :today
                        AND EXISTS (
                            SELECT t FROM Treatment t
                            WHERE t.surgicalCenterTimeSlot = ts
                            AND t.approvalDate is NULL
                        )
                """)
        List<SurgicalCenterTimeSlot> findAllContainingNotApprovedTreatmentsAndNotInTimeSlotIdList(
                @Param("institutionId") Long institutionId,
                @Param("today") LocalDate today,
                List<Long> timeSlotIds);

        @Query("""
                        SELECT ts FROM SurgicalCenterTimeSlot ts
                        LEFT JOIN FETCH ts.surgicalCenter sc
                        WHERE sc.institution.id = :institutionId
                        AND ts.date <= :today
                        AND EXISTS (
                            SELECT t FROM Treatment t
                            WHERE t.surgicalCenterTimeSlot = ts
                            AND t.approvalDate is NULL
                        )
                """)
        List<SurgicalCenterTimeSlot> findAllContainingNotApprovedTreatments(
                @Param("institutionId") Long institutionId,
                @Param("today") LocalDate today);

        boolean existsBySurgicalCenterAndDateAndStartTimeAndEndTime(
                SurgicalCenter surgicalCenter,
                LocalDate date,
                LocalTime startTime,
                LocalTime endTime);
}
