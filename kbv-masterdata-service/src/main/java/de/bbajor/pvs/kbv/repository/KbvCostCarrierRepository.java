package de.bbajor.pvs.kbv.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.kbv.model.KbvCostCarrier;

@Repository
public interface KbvCostCarrierRepository extends JpaRepository<KbvCostCarrier, Long> {

    List<KbvCostCarrier> findByQuarter(String quarter);

    List<KbvCostCarrier> findByCodeAndQuarter(String code, String quarter);

    @Query("SELECT c FROM KbvCostCarrier c WHERE c.quarter = :quarter AND c.validFrom <= :date AND (c.validTo IS NULL OR c.validTo >= :date)")
    List<KbvCostCarrier> findActiveByQuarterAndDate(@Param("quarter") String quarter, @Param("date") LocalDate date);

    @Query("SELECT c FROM KbvCostCarrier c WHERE c.code = :code AND c.validFrom <= :date AND (c.validTo >= :date OR c.validTo IS NULL)")
    Optional<KbvCostCarrier> findActiveByCodeAndDate(@Param("code") String code, @Param("date") LocalDate date);
}
