package de.bbajor.pvs.ivom.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.ivom.model.IvomPlan;

public interface IvomRepository extends JpaRepository<IvomPlan, Long>, JpaSpecificationExecutor<IvomPlan> {

    Slice<Patient> findAllBy(Pageable pageable);
}
