package de.bbajor.pvs.ivomdrug.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.ivomdrug.model.IvomDrug;

public interface IvomDrugRepository extends JpaRepository<IvomDrug, Long>, JpaSpecificationExecutor<IvomDrug> {

    Slice<IvomDrug> findAllBy(Pageable pageable);
}
