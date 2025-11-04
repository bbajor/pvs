package de.bbajor.pvs.location.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.repository.LocationRepository;
import de.bbajor.pvs.institution.context.InstitutionContext;
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

    /**
     * Gets the first/default location for the current institution.
     * Uses InstitutionContext to find the institution and returns its first active location.
     * Returns null if no location is configured for the current institution.
     */
    @Transactional(readOnly = true)
    public Location getDefaultLocation() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            // Find first active location for current institution
            List<Location> locations = locationRepository.findByInstitutionIdAndActive(institutionId, true);
            if (!locations.isEmpty()) {
                return locations.get(0);
            }
        }
        
        // Fallback: Return first active location if available
        return locationRepository.findByActive(true).stream().findFirst().orElse(null);
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
     */
    @Transactional(readOnly = true)
    public List<Location> getAllLocations(boolean activeOnly) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            if (activeOnly) {
                return locationRepository.findByInstitutionIdAndActive(institutionId, true);
            } else {
                return locationRepository.findByInstitutionId(institutionId);
            }
        }
        
        // Fallback: Return all locations (for super admin)
        if (activeOnly) {
            return locationRepository.findByActive(true);
        }
        return locationRepository.findAll();
    }

    /**
     * Finds a location by ID for the current institution.  
     * Ensures institution isolation.
     */
    @Transactional(readOnly = true)
    public Optional<Location> findById(Long id) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            return locationRepository.findByIdAndInstitutionId(id, institutionId);
        }
        return locationRepository.findById(id);
    }

    /**
     * Saves or updates a location.
     * Sets institution if not already set (using InstitutionContext).
     */
    @Transactional
    public Location saveLocation(Location location) {
        // Set institution if not already set
        if (location.getInstitution() == null) {
            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId != null) {
                // For now, map institutionId to institutionId
                institutionRepository.findById(institutionId)
                        .ifPresent(location::setInstitution);
                
                // Fallback: Try to find institution by institution code
                if (location.getInstitution() == null) {
                    // This will be handled in multi-DB implementation
                }
            }
        }
        
        // Validate that institution is set
        if (location.getInstitution() == null) {
            throw new IllegalStateException(
                    "Cannot save location without institution. Ensure at least one institution exists.");
        }

        return locationRepository.save(location);
    }

    /**
     * Deletes a location.
     * Note: Consider using deactivateLocation() instead to preserve data integrity.
     */
    @Transactional
    public void deleteLocation(Long id) {
        locationRepository.findById(id).ifPresent(locationRepository::delete);
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

