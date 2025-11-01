package de.bbajor.pvs.intravitreal.treatment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.tenant.repository.TenantAwareRepository;

public interface TreatmentPlanRepository
                extends TenantAwareRepository<TreatmentPlan, Long>, JpaSpecificationExecutor<TreatmentPlan> {

        /**
         * Find all treatment plans for a tenant with pagination.
         */
        @Query("SELECT tp FROM TreatmentPlan tp WHERE tp.tenant.id = :tenantId")
        Slice<TreatmentPlan> findAllByTenant(@Param("tenantId") Long tenantId, Pageable pageable);

        /**
         * Find treatment plans by patient ID within tenant.
         * IMPORTANT: Ensures cross-tenant access is prevented.
         */
        @Query("SELECT tp FROM TreatmentPlan tp " +
               "WHERE tp.tenant.id = :tenantId AND tp.patient.id = :patientId")
        List<TreatmentPlan> findByTenantAndPatientId(
                @Param("tenantId") Long tenantId,
                @Param("patientId") Integer patientId);

        /**
         * Find all treatment plans with patient and diagnosis for tenant.
         */
        @Query("""
                SELECT DISTINCT tp FROM TreatmentPlan tp
                LEFT JOIN FETCH tp.patient p
                LEFT JOIN FETCH p.address a
                LEFT JOIN FETCH tp.diagnosis d
                WHERE tp.tenant.id = :tenantId
                """)
        List<TreatmentPlan> findAllTreatmentPlansWithPatientDiagnosisForTenant(@Param("tenantId") Long tenantId);

        /**
         * Find treatment plan by ID with patient and diagnosis, ensuring tenant access.
         */
        @Query("""
                SELECT DISTINCT tp FROM TreatmentPlan tp
                LEFT JOIN FETCH tp.patient p
                LEFT JOIN FETCH p.address a
                LEFT JOIN FETCH tp.diagnosis d
                WHERE tp.id = :id AND tp.tenant.id = :tenantId
                """)
        Optional<TreatmentPlan> findTreatmentPlanByIdAndTenantWithPatientDiagnosis(
                @Param("id") Long id,
                @Param("tenantId") Long tenantId);
}
