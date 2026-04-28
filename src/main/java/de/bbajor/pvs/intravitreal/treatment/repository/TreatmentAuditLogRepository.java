package de.bbajor.pvs.intravitreal.treatment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentAuditLog;

public interface TreatmentAuditLogRepository extends JpaRepository<TreatmentAuditLog, Long>, JpaSpecificationExecutor<TreatmentAuditLog> {
    List<TreatmentAuditLog> findByTreatmentOrderByActionTimestampAsc(Treatment treatment);
}
