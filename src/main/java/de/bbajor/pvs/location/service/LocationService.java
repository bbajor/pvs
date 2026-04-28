package de.bbajor.pvs.location.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.appointment.repository.AppointmentSchedulerRepository;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.repository.LocationRepository;
import de.bbajor.pvs.patient.repository.PatientRepository;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing locations (Standorte) of an institution.
 * <p>
 * Locations replace the old "Practice" entity. An institution can have
 * multiple locations where patients are treated.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final InstitutionRepository institutionRepository;
    private final PatientRepository patientRepository;
    private final AppointmentSchedulerRepository appointmentSchedulerRepository;
    private final UserAccountRepository userAccountRepository;

    /**
     * Gets the first/default location for the current institution.
     * Uses InstitutionContext to find the institution and returns its first active location.
     * Returns null if no location is configured for the current institution.
     * 
     * @throws IllegalStateException if no institution context is set (prevents cross-institution data leakage)
     */
    @Transactional(readOnly = true)
    public Location getDefaultLocation() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot access default location without institution context. Institution isolation required.");
        }
        
        // Find first active location for current institution
        List<Location> locations = locationRepository.findByInstitutionIdAndActive(institutionId, true);
        if (!locations.isEmpty()) {
            return locations.get(0);
        }
        
        // No active location found for current institution
        return null;
    }

    /**
     * Gets all locations for the current institution.
     * Uses InstitutionContext to find the institution.
     * Only returns active locations by default.
     */
    @Transactional(readOnly = true)
    public List<Location> getAllLocations() {
        return getAllLocations(true);
    }
    
    /**
     * Gets all locations for the current institution.
     * Uses InstitutionContext to find the institution.
     * 
     * @param activeOnly if true, only returns active locations; if false, returns all locations
     * @return list of locations for current institution, empty list if no institution context (prevents cross-institution data leakage)
     */
    @Transactional(readOnly = true)
    public List<Location> getAllLocations(boolean activeOnly) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            // No institution context - return empty list to enforce data isolation
            // Consistent with other services (e.g., PatientService, SurgicalCenterService)
            return List.of();
        }
        
        if (activeOnly) {
            return locationRepository.findByInstitutionIdAndActive(institutionId, true);
        } else {
            return locationRepository.findByInstitutionId(institutionId);
        }
    }

    /**
     * Finds a location by ID for the current institution.  
     * Ensures institution isolation.
     * 
     * @param id the location ID
     * @return Optional containing the location if found and belongs to current institution, empty otherwise
     * @throws IllegalStateException if no institution context is set (prevents cross-institution data leakage)
     */
    @Transactional(readOnly = true)
    public Optional<Location> findById(Long id) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot access location without institution context. Institution isolation required.");
        }
        
        return locationRepository.findByIdAndInstitutionId(id, institutionId);
    }

    /**
     * Saves or updates a location.
     * Sets institution if not already set (using InstitutionContext) and ensures
     * main-location invariant per institution (there is always exactly one).
     */
    @Transactional
    public Location saveLocation(Location location) {
        // Set institution if not already set
        if (location.getInstitution() == null) {
            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId != null) {
                institutionRepository.findById(institutionId)
                        .ifPresent(location::setInstitution);
            }
        }

        if (location.getInstitution() == null) {
            throw new IllegalStateException(
                    "Cannot save location without institution. Ensure at least one institution exists.");
        }

        // Ensure there is exactly one main location per institution
        Long institutionId = location.getInstitution().getId();
        List<Location> existingMainLocations =
                locationRepository.findMainLocationsByInstitutionId(institutionId);

        boolean isNew = location.getId() == null;
        boolean wantsToBeMain = location.isMainLocation();

        if (isNew && existingMainLocations.isEmpty()) {
            // First location for this institution becomes main location automatically
            location.setMainLocation(true);
        } else if (wantsToBeMain) {
            // Demote all other main locations
            for (Location other : existingMainLocations) {
                if (location.getId() == null || !other.getId().equals(location.getId())) {
                    other.setMainLocation(false);
                    locationRepository.save(other);
                }
            }
        } else if (!wantsToBeMain && !existingMainLocations.isEmpty()) {
            // Prevent removing main flag if it would leave institution without main location
            boolean isCurrentlyMain = existingMainLocations.stream()
                    .anyMatch(l -> l.getId().equals(location.getId()));
            if (isCurrentlyMain && existingMainLocations.size() == 1) {
                // Keep flag on; caller must assign another main location first
                location.setMainLocation(true);
            }
        }

        return locationRepository.save(location);
    }

    /**
     * Deletes a location.
     * Deletes only if there are no relational dependencies (patients, schedulers, users).
     */
    @Transactional
    public void deleteLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));

        long patientCount = patientRepository.countByLocation(location);
        long schedulerCount = appointmentSchedulerRepository.countByLocation(location);
        long userCount = userAccountRepository.countByPreferredLocation(location);

        if (patientCount > 0 || schedulerCount > 0 || userCount > 0) {
            throw new IllegalStateException("Standort kann nicht gelöscht werden, da noch abhängige Daten existieren.");
        }

        locationRepository.delete(location);
    }
    
    /**
     * Activates a location.
     * Makes the location available for selection in patient forms and appointments.
     */
    @Transactional
    public Location activateLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
        location.setActive(true);
        return locationRepository.save(location);
    }
    
    /**
     * Deactivates a location.
     * The location will no longer be available for selection in patient forms or appointments,
     * but existing patients and appointments will remain associated with it.
     */
    @Transactional
    public Location deactivateLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
        location.setActive(false);
        return locationRepository.save(location);
    }

    /**
     * Finds all locations for a specific institution.
     */
    @Transactional(readOnly = true)
    public List<Location> findByInstitution(Institution institution) {
        if (institution != null && institution.getId() != null) {
            return locationRepository.findByInstitutionId(institution.getId());
        }
        return List.of();
    }

    /**
     * Finds all locations for a specific institution ID.
     * Used during migration when institution ID maps to institution ID.
     */
    @Transactional(readOnly = true)
    public List<Location> findByInstitutionId(Long institutionId) {
        if (institutionId != null) {
            return locationRepository.findByInstitutionId(institutionId);
        }
        return List.of();
    }
}

