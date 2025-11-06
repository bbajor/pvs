package de.bbajor.pvs.kbv.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.kbv.model.KbvImportHistory;

@Repository
public interface KbvImportHistoryRepository extends JpaRepository<KbvImportHistory, Long> {

    List<KbvImportHistory> findByQuarterOrderByStartedAtDesc(String quarter);

    List<KbvImportHistory> findByStatusOrderByStartedAtDesc(KbvImportHistory.ImportStatus status);

    @Query("SELECT h FROM KbvImportHistory h ORDER BY h.startedAt DESC")
    List<KbvImportHistory> findAllOrderByStartedAtDesc();

    Optional<KbvImportHistory> findByQuarterAndVersionAndImportType(
            String quarter,
            String version,
            KbvImportHistory.ImportType importType);
}
