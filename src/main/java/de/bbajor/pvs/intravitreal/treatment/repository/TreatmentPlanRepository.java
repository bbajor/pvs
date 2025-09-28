package de.bbajor.pvs.intravitreal.treatment.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;

import java.util.List;


public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, Long>, JpaSpecificationExecutor<TreatmentPlan> {

    Slice<Patient> findAllBy(Pageable pageable);

    List<TreatmentPlan> findByPatientId(Integer patientId);
}
