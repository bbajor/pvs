package de.bbajor.pvs.intravitreal.treatment.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;

public interface IvomDiagnosisRepository
        extends JpaRepository<Diagnosis, Long>, JpaSpecificationExecutor<Diagnosis> {

    Slice<Diagnosis> findAllBy(Pageable pageable);

    List<Diagnosis> findByName(String name);

}
