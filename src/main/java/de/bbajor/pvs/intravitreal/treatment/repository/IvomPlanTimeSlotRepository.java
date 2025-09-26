package de.bbajor.pvs.intravitreal.treatment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.intravitreal.treatment.model.TreatmentSlot;

public interface IvomPlanTimeSlotRepository
        extends JpaRepository<TreatmentSlot, Long>, JpaSpecificationExecutor<TreatmentSlot> {

    List<TreatmentSlot> findAllByTreatmentPlanIdAndSideOfEye(Long treatmentPlanId, String sideOfEye);

}
