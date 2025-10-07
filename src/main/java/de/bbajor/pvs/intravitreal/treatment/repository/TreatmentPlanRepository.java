package de.bbajor.pvs.intravitreal.treatment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;

public interface TreatmentPlanRepository
        extends JpaRepository<TreatmentPlan, Long>, JpaSpecificationExecutor<TreatmentPlan> {

    Slice<TreatmentPlan> findAllBy(Pageable pageable);

    List<TreatmentPlan> findByPatientId(Integer patientId);

    @Query("Select Distinct tp from TreatmentPlan tp " +
            "left join fetch tp.patient " +
            "left join fetch tp.diagnosis " +
            "left join fetch tp.treatments tm " +
            "left join fetch tm.medication m " +
            "where tp.id = :id")
    Optional<TreatmentPlan> findByIdWithDetailsWithoutSurgicalCenterTimeSlots(@Param("id") Long id);
}
