package de.bbajor.pvs.ivomplan.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.ivomplan.model.IvomDiagnosis;

public interface IvomDiagnosisRepository
        extends JpaRepository<IvomDiagnosis, Integer>, JpaSpecificationExecutor<IvomDiagnosis> {

    Slice<IvomDiagnosis> findAllBy(Pageable pageable);

    List<IvomDiagnosis> findByName(String name);

}
