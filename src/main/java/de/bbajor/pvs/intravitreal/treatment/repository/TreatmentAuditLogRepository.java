package de.bbajor.pvs.intravitreal.treatment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentAuditLog;

public interface TreatmentAuditLogRepository extends JpaRepository<TreatmentAuditLog, Long> {
    List<TreatmentAuditLog> findByTreatmentOrderByActionTimestampAsc(Treatment treatment);
}
