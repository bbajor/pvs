package de.bbajor.pvs.intravitreal.treatment.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.intravitreal.treatment.model.Diagnose;

public interface IvomDiagnosisRepository
        extends JpaRepository<Diagnose, Long>, JpaSpecificationExecutor<Diagnose> {

    Slice<Diagnose> findAllBy(Pageable pageable);

    List<Diagnose> findByName(String name);

}
