package de.bbajor.pvs.appointment.repository;

import java.util.List;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.practice.model.Practice;
import de.bbajor.pvs.tenant.repository.TenantAwareRepository;

/**
 * Repository for AppointmentScheduler entities.
 * Tenant-aware to ensure data isolation.
 */
@Repository
public interface AppointmentSchedulerRepository extends TenantAwareRepository<AppointmentScheduler, Long> {

    /**
     * Find all schedulers for a specific practice.
     */
    List<AppointmentScheduler> findByPractice(Practice practice);

    /**
     * Find all active schedulers for a practice.
     */
    List<AppointmentScheduler> findByPracticeAndActiveTrue(Practice practice);

    /**
     * Find all schedulers by practice ID.
     */
    List<AppointmentScheduler> findByPracticeId(Long practiceId);

    /**
     * Find active schedulers by practice ID.
     */
    List<AppointmentScheduler> findByPracticeIdAndActiveTrue(Long practiceId);

    /**
     * Find all schedulers for a tenant and practice.
     */
    @Query("SELECT s FROM AppointmentScheduler s WHERE s.tenant.id = :tenantId AND s.practice.id = :practiceId")
    List<AppointmentScheduler> findByTenantAndPractice(
        @Param("tenantId") Long tenantId,
        @Param("practiceId") Long practiceId);

    /**
     * Find active schedulers for a tenant.
     */
    @Query("SELECT s FROM AppointmentScheduler s WHERE s.tenant.id = :tenantId AND s.active = true")
    List<AppointmentScheduler> findActivByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Find scheduler by ID and tenant (tenant-safe access).
     */
    @Query("SELECT s FROM AppointmentScheduler s WHERE s.id = :id AND s.tenant.id = :tenantId")
    Optional<AppointmentScheduler> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
