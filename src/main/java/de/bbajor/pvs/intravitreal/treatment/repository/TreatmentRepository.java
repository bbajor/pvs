package de.bbajor.pvs.intravitreal.treatment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;

public interface TreatmentRepository
        extends JpaRepository<Treatment, Long>, JpaSpecificationExecutor<Treatment> {

    @Query("""
            select distinct t from Treatment t
            left join fetch t.surgicalCenterTimeSlot
            where t.treatmentPlan.id = :planId
            """)
    List<Treatment> findTreatmentsByPlanId(@Param("planId") Long planId);

}
