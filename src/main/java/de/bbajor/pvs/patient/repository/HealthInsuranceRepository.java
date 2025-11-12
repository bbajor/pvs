package de.bbajor.pvs.patient.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.patient.model.HealthInsurance;

public interface HealthInsuranceRepository
        extends JpaRepository<HealthInsurance, Integer>, JpaSpecificationExecutor<HealthInsurance> {

    Slice<HealthInsurance> findAllBy(Pageable pageable);
    
    /**
     * Findet alle Versicherungen für eine Institution.
     */
    @Query("SELECT h FROM HealthInsurance h WHERE h.institution.id = :institutionId")
    List<HealthInsurance> findByInstitutionId(@Param("institutionId") Long institutionId);
}