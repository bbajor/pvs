package de.bbajor.pvs.ivom.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.ivom.model.Ivom;

public interface IvomRepository extends JpaRepository<Ivom, Long>, JpaSpecificationExecutor<Ivom> {

    Slice<Patient> findAllBy(Pageable pageable);
}
