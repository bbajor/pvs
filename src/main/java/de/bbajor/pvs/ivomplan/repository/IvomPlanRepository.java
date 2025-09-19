package de.bbajor.pvs.ivomplan.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.ivomplan.model.IvomPlan;
import java.util.List;


public interface IvomPlanRepository extends JpaRepository<IvomPlan, Long>, JpaSpecificationExecutor<IvomPlan> {

    Slice<Patient> findAllBy(Pageable pageable);

    List<IvomPlan> findByPatientId(Integer patientId);
}
