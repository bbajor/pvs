package de.bbajor.pvs.intravitreal.treatment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;

public interface TreatmentPlanTimeSlotRepository
        extends JpaRepository<Treatment, Long>, JpaSpecificationExecutor<Treatment> {

    List<Treatment> findAllByTreatmentPlanId(Long treatmentPlanId);

}
