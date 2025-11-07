package de.bbajor.pvs.appointment.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.SchedulerAssignment;
import de.bbajor.pvs.appointment.repository.AppointmentSchedulerRepository;
import de.bbajor.pvs.appointment.repository.SchedulerAssignmentRepository;
import de.bbajor.pvs.institution.service.InstitutionAccessValidator;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.institution.context.InstitutionContext;
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
    private final InstitutionAccessValidator institutionAccessValidator;
    private final LocationService locationService;

    /**
     * Find all schedulers for a location.
     * Ensures tenant isolation.
     * <p>
     * Data isolation: All filtering is done via institution.
     * AppointmentScheduler → Location → Institution (primary path).
     * </p>
     */
    public List<AppointmentScheduler> findByLocation(Location location) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            // Validate location belongs to current institution
            if (location.getInstitution() != null && location.getInstitution().getId() != null) {
                institutionAccessValidator.validateInstitutionAccess(location.getInstitution().getId(), 
                    "Location", location.getId());
            }
            return schedulerRepository.findByLocation(location);
        }
        return schedulerRepository.findByLocation(location);
    }

    /**
     * Find all active schedulers for a location.
     */
    public List<AppointmentScheduler> findActiveByLocation(Location location) {
        return schedulerRepository.findByLocationAndActiveTrue(location);
    }

    /**
     * Find scheduler by ID.
     * Ensures tenant isolation.
     */
    public Optional<AppointmentScheduler> findById(Long id) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            Optional<AppointmentScheduler> scheduler = schedulerRepository.findByIdAndInstitutionId(id, institutionId);
            scheduler.ifPresent(s -> {
                if (s.getLocation() != null && s.getLocation().getInstitution() != null) {
                    institutionAccessValidator.validateInstitutionAccess(
                        s.getLocation().getInstitution().getId(), 
                        "AppointmentScheduler", 
                        s.getId());
                }
            });
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
     * Validate tenant for scheduler.
     * Ensures tenant consistency with location.
     * <p>
     * Data isolation: All filtering is done via institution.
     * AppointmentScheduler → Location → Institution (primary path).
     * </p>
     */
    private void validateAndSetTenant(AppointmentScheduler scheduler) {
        Long institutionId = InstitutionContext.getInstitutionId();
        
        if (institutionId != null) {
            // Validate location belongs to current institution/tenant
            if (scheduler.getLocation() != null && scheduler.getLocation().getInstitution() != null) {
                institutionAccessValidator.validateInstitutionAccess(
                    scheduler.getLocation().getInstitution().getId(),
                    "Location",
                    scheduler.getLocation().getId());
            }
            
            // Ensure scheduler has location
            if (scheduler.getLocation() == null) {
                // Try to set default location
                Location defaultLocation = locationService.getDefaultLocation();
                if (defaultLocation != null) {
                    scheduler.setLocation(defaultLocation);
                } else {
                    throw new IllegalStateException(
                        "No location found. Ensure LocationService has a location configured for the current institution.");
                }
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
     * Institution-aware: Returns only schedulers for current institution.
     */
    public List<AppointmentScheduler> findAll() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            return schedulerRepository.findByInstitutionId(institutionId);
        }
        return schedulerRepository.findAll();
    }

    /**
     * Find all active schedulers for current institution.
     */
    public List<AppointmentScheduler> findAllActive() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("No institution context available");
        }
        return schedulerRepository.findActivByInstitutionId(institutionId);
    }
}
