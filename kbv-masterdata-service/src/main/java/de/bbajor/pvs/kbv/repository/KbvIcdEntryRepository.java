package de.bbajor.pvs.kbv.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.kbv.model.KbvIcdEntry;

@Repository
public interface KbvIcdEntryRepository extends JpaRepository<KbvIcdEntry, Long> {

    List<KbvIcdEntry> findByQuarter(String quarter);

    List<KbvIcdEntry> findByCodeAndQuarter(String code, String quarter);

    @Query("SELECT e FROM KbvIcdEntry e WHERE e.quarter = :quarter AND e.validFrom <= :date AND (e.validTo IS NULL OR e.validTo >= :date)")
    List<KbvIcdEntry> findActiveByQuarterAndDate(@Param("quarter") String quarter, @Param("date") LocalDate date);

    @Query("SELECT e FROM KbvIcdEntry e WHERE e.code = :code AND e.validFrom <= :date AND (e.validTo IS NULL OR e.validTo >= :date)")
    Optional<KbvIcdEntry> findActiveByCodeAndDate(@Param("code") String code, @Param("date") LocalDate date);
}
