package de.bbajor.pvs.intravitreal.treatment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.intravitreal.treatment.model.IvomPlanTimeSlot;

public interface IvomPlanTimeSlotRepository
        extends JpaRepository<IvomPlanTimeSlot, Long>, JpaSpecificationExecutor<IvomPlanTimeSlot> {

}
