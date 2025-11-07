package de.bbajor.pvs.intravitreal.treatment.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;

public interface TreatmentRepository
                extends JpaRepository<Treatment, Long>, JpaSpecificationExecutor<Treatment> {

        @Query("""
                        select distinct t from Treatment t
                        inner join fetch t.treatmentPlan tp
                        left join fetch t.medication
                        left join fetch t.surgicalCenterTimeSlot ts
                        left join fetch ts.surgicalCenter sc
                        where tp.id = :id
                        order by ts.date asc
                        """)
        List<Treatment> findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(Long id);

        @Query("""
                        select distinct t from Treatment t
                        left join fetch t.surgicalCenterTimeSlot ts
                        left join fetch ts.surgicalCenter sc
                        left join fetch t.medication
                        inner join fetch t.treatmentPlan tp
                        where ts.date between :startDate and :endDate
                        and t.treatmentPlan is not null
                        order by ts.date asc
                        """)
        List<Treatment> findTreatmentsByDateRangeWithSurgicalCenterAndTreatmentPlan(LocalDate startDate,
                        LocalDate endDate);

        @Query("""
                        select t from Treatment t
                        where t.surgicalCenterTimeSlot.id = :timeSlotId
                        order by t.surgicalCenterTimeSlot.date asc
                        """)
        List<Treatment> findByTimeSlotId(Long timeSlotId);

}
