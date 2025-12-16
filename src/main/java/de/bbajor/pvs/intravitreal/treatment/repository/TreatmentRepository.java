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
                        left join fetch t.medicationFavourite mf
                        left join fetch mf.medication
                        left join fetch t.surgicalCenterTimeSlot ts
                        left join fetch ts.surgicalCenter sc
                        left join fetch t.treatingDoctors td
                        left join fetch td.institution
                        where tp.id = :id
                        order by ts.date asc
                        """)
        List<Treatment> findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(Long id);

        @Query("""
                        select distinct t from Treatment t
                        left join fetch t.surgicalCenterTimeSlot ts
                        left join fetch ts.surgicalCenter sc
                        left join fetch t.medicationFavourite mf
                        left join fetch mf.medication
                        inner join fetch t.treatmentPlan tp
                        inner join fetch tp.patient p
                        left join fetch p.healthInsurance hi
                        left join fetch hi.institution hiInst
                        left join fetch p.location loc
                        left join fetch t.treatingDoctors td
                        left join fetch td.institution
                        where ts.date between :startDate and :endDate
                        and t.treatmentPlan is not null
                        order by ts.date asc
                        """)
        List<Treatment> findTreatmentsByDateRangeWithSurgicalCenterAndTreatmentPlan(LocalDate startDate,
                        LocalDate endDate);
        
        @Query("""
                        select distinct t from Treatment t
                        left join fetch t.surgicalCenterTimeSlot ts
                        left join fetch ts.surgicalCenter sc
                        left join fetch t.medicationFavourite mf
                        left join fetch mf.medication
                        inner join fetch t.treatmentPlan tp
                        inner join fetch tp.patient p
                        left join fetch p.healthInsurance hi
                        left join fetch hi.institution hiInst
                        left join fetch p.location loc
                        left join fetch t.treatingDoctors td
                        left join fetch td.institution
                        where ts.date between :startDate and :endDate
                        and t.treatmentPlan is not null
                        and loc.institution.id = :institutionId
                        order by ts.date asc
                        """)
        List<Treatment> findTreatmentsByDateRangeAndInstitution(LocalDate startDate, LocalDate endDate,
                        Long institutionId);

        @Query("""
                        select distinct t from Treatment t
                        left join fetch t.medicationFavourite mf
                        left join fetch mf.medication
                        left join fetch t.surgicalCenterTimeSlot ts
                        left join fetch ts.surgicalCenter sc
                        left join fetch t.treatmentPlan tp
                        left join fetch tp.patient p
                        left join fetch p.healthInsurance hi
                        left join fetch t.treatingDoctors td
                        left join fetch td.institution
                        where ts.id = :timeSlotId
                        order by ts.date asc
                        """)
        List<Treatment> findByTimeSlotId(Long timeSlotId);
        
        @Query("""
                        select distinct t from Treatment t
                        inner join fetch t.treatmentPlan tp
                        left join fetch tp.patient p
                        left join fetch t.medicationFavourite mf
                        left join fetch t.surgicalCenterTimeSlot ts
                        left join fetch ts.surgicalCenter sc
                        where t.id = :id
                        """)
        java.util.Optional<Treatment> findByIdWithAllRelationships(Long id);

}
