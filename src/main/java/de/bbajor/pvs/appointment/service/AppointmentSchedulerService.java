package de.bbajor.pvs.appointment.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.SchedulerAssignment;
import de.bbajor.pvs.appointment.repository.AppointmentSchedulerRepository;
import de.bbajor.pvs.appointment.repository.SchedulerAssignmentRepository;
import de.bbajor.pvs.practice.model.Practice;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.tenant.context.TenantContext;
import de.bbajor.pvs.tenant.service.TenantAccessValidator;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing appointment schedulers.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentSchedulerService {

    private final AppointmentSchedulerRepository schedulerRepository;
    private final SchedulerAssignmentRepository assignmentRepository;
    private final TenantAccessValidator tenantAccessValidator;

    /**
     * Find all schedulers for a practice.
     * Ensures tenant isolation.
     */
    public List<AppointmentScheduler> findByPractice(Practice practice) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            tenantAccessValidator.validateTenantAccess(practice.getTenant().getId(), 
                "Practice", practice.getId());
            return schedulerRepository.findByTenantAndPractice(tenantId, practice.getId());
        }
        return schedulerRepository.findByPractice(practice);
    }

    /**
     * Find all active schedulers for a practice.
     */
    public List<AppointmentScheduler> findActiveByPractice(Practice practice) {
        return schedulerRepository.findByPracticeAndActiveTrue(practice);
    }

    /**
     * Find scheduler by ID.
     * Ensures tenant isolation.
     */
    public Optional<AppointmentScheduler> findById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            Optional<AppointmentScheduler> scheduler = schedulerRepository.findByIdAndTenantId(id, tenantId);
            scheduler.ifPresent(s -> 
                tenantAccessValidator.validateTenantAccess(s.getTenant().getId(), "AppointmentScheduler", s.getId()));
            return scheduler;
        }
        return schedulerRepository.findById(id);
    }

    /**
     * Create or update a scheduler.
     * Ensures tenant consistency.
     */
    public AppointmentScheduler save(AppointmentScheduler scheduler) {
        validateAndSetTenant(scheduler);
        return schedulerRepository.save(scheduler);
    }

    /**
     * Validate and set tenant for scheduler.
     * Ensures tenant consistency with practice.
     */
    private void validateAndSetTenant(AppointmentScheduler scheduler) {
        Long tenantId = TenantContext.getTenantId();
        
        if (tenantId != null) {
            // Validate practice belongs to current tenant
            if (scheduler.getPractice() != null) {
                tenantAccessValidator.validateTenantAccess(
                    scheduler.getPractice().getTenant().getId(),
                    "Practice",
                    scheduler.getPractice().getId());
            }
            
            // Set tenant from practice if not set
            if (scheduler.getTenant() == null && scheduler.getPractice() != null) {
                scheduler.setTenant(scheduler.getPractice().getTenant());
            }
            
            // Validate tenant consistency
            if (scheduler.getTenant() != null && !scheduler.getTenant().getId().equals(tenantId)) {
                throw new IllegalArgumentException(
                    "Terminplaner geh?rt nicht zum aktuellen Mandanten");
            }
        }
    }

    /**
     * Delete a scheduler.
     */
    public void delete(AppointmentScheduler scheduler) {
        schedulerRepository.delete(scheduler);
    }

    /**
     * Deactivate a scheduler instead of deleting it.
     */
    public void deactivate(AppointmentScheduler scheduler) {
        scheduler.setActive(false);
        schedulerRepository.save(scheduler);
    }

    /**
     * Assign a user to a scheduler.
     */
    public SchedulerAssignment assignUser(AppointmentScheduler scheduler, UserAccount userAccount) {
        SchedulerAssignment assignment = new SchedulerAssignment()
            .setScheduler(scheduler)
            .setUserAccount(userAccount);
        return assignmentRepository.save(assignment);
    }

    /**
     * Assign a role to a scheduler.
     */
    public SchedulerAssignment assignRole(AppointmentScheduler scheduler, String role) {
        SchedulerAssignment assignment = new SchedulerAssignment()
            .setScheduler(scheduler)
            .setRole(role);
        return assignmentRepository.save(assignment);
    }

    /**
     * Get all assignments for a scheduler.
     */
    public List<SchedulerAssignment> getAssignments(AppointmentScheduler scheduler) {
        return assignmentRepository.findByScheduler(scheduler);
    }

    /**
     * Get all schedulers assigned to a user.
     */
    public List<SchedulerAssignment> getSchedulersForUser(UserAccount userAccount) {
        return assignmentRepository.findByUserAccount(userAccount);
    }

    /**
     * Get all schedulers assigned to a role.
     */
    public List<SchedulerAssignment> getSchedulersForRole(String role) {
        return assignmentRepository.findByRole(role);
    }

    /**
     * Check if a user has access to a scheduler.
     */
    public boolean hasAccess(AppointmentScheduler scheduler, UserAccount userAccount) {
        // Check direct user assignment
        if (assignmentRepository.existsBySchedulerAndUserAccount(scheduler, userAccount)) {
            return true;
        }
        
        // Check role-based assignment
        for (String role : userAccount.getRoles()) {
            if (assignmentRepository.existsBySchedulerAndRole(scheduler, role)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Find all schedulers.
     * Tenant-aware: Returns only schedulers for current tenant.
     */
    public List<AppointmentScheduler> findAll() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return schedulerRepository.findByTenantId(tenantId);
        }
        return schedulerRepository.findAll();
    }

    /**
     * Find all active schedulers for current tenant.
     */
    public List<AppointmentScheduler> findAllActive() {
        Long tenantId = tenantAccessValidator.requireCurrentTenantId();
        return schedulerRepository.findActivByTenantId(tenantId);
    }
}
