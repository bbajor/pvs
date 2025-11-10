package de.bbajor.pvs.kbv.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.kbv.model.KbvInsurance;

@Repository
public interface KbvInsuranceRepository extends JpaRepository<KbvInsurance, Long> {

    List<KbvInsurance> findByQuarter(String quarter);

    List<KbvInsurance> findByCodeAndQuarter(String code, String quarter);

    @Query("SELECT i FROM KbvInsurance i WHERE i.quarter = :quarter AND i.validFrom <= :date AND (i.validTo IS NULL OR i.validTo >= :date)")
    List<KbvInsurance> findActiveByQuarterAndDate(@Param("quarter") String quarter, @Param("date") LocalDate date);

    @Query("SELECT i FROM KbvInsurance i WHERE i.code = :code AND i.validFrom <= :date AND (i.validTo >= :date OR i.validTo IS NULL)")
    Optional<KbvInsurance> findActiveByCodeAndDate(@Param("code") String code, @Param("date") LocalDate date);
}
