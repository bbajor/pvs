package de.bbajor.pvs.base.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.base.domain.HealthInsurance;

public interface HealthInsuranceRepository
        extends JpaRepository<HealthInsurance, Integer>, JpaSpecificationExecutor<HealthInsurance> {

    Slice<HealthInsurance> findAllBy(Pageable pageable);
}