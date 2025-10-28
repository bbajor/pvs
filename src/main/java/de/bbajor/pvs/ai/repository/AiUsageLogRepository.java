package de.bbajor.pvs.ai.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.ai.domain.AiUsageLog;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    @Query("SELECT COUNT(l) FROM AiUsageLog l WHERE l.provider = :provider "
            + "AND l.timestamp >= :start AND l.timestamp < :end AND l.status = 'success'")
    long countByProviderAndMonth(@Param("provider") String provider,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT l FROM AiUsageLog l WHERE l.timestamp >= :start AND l.timestamp < :end ORDER BY l.timestamp DESC")
    List<AiUsageLog> findByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

}

