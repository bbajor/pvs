package de.bbajor.pvs.patient.service;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.repository.HealthInsuranceRepository;
import de.bbajor.pvs.patient.repository.PatientRepository;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private HealthInsuranceRepository healthInsuranceRepository;
    @Autowired
    private PatientMapper mapper;
    @Autowired
    private LocationService locationService;
    @Autowired
    private UserAccountRepository userAccountRepository;
    
    public List<Patient> findPatients(String filter) {
        Long institutionId = InstitutionContext.getInstitutionId();
        
        if (StringUtils.isEmpty(filter)) {
            return getAll();
        }

        // Institution-aware search using institution-specific query
        if (institutionId != null) {
            // Use institution-aware search method
            return patientRepository.searchByNameInInstitution(institutionId, filter);
        }
        
        // No institution context - SUPER_ADMIN should not see patient data
        // Return empty list to enforce data isolation
        return List.of();
    }

    public Patient save(Patient patient) {
        if (patient == null) {
            return null;
        }
        
        // Normalize insurance_number: empty strings should be treated as NULL
        // This prevents unique constraint violations when insurance_number is empty
        if (patient.getInsuranceNumber() != null && patient.getInsuranceNumber().trim().isEmpty()) {
            patient.setInsuranceNumber(null);
        }
        
        // Set location if not already set
        setPracticeIfNeeded(patient);
        
        // Validate that location is set and has an institution
        if (patient.getLocation() == null) {
            throw new IllegalStateException(
                "Cannot save patient without location. Patient: " + 
                (patient.getFirstName() != null ? patient.getFirstName() : "?") + " " +
                (patient.getLastName() != null ? patient.getLastName() : "?"));
        }
        
        // Validate that location has an institution
        if (patient.getLocation().getInstitution() == null) {
            throw new IllegalStateException(
                "Cannot save patient: location must be assigned to an institution. Patient: " + 
                (patient.getFirstName() != null ? patient.getFirstName() : "?") + " " +
                (patient.getLastName() != null ? patient.getLastName() : "?"));
        }
        
        // Validate InstitutionContext
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException(
                "Cannot save patient without institution context. " +
                "Please ensure you are logged in with an institution context.");
        }
        
        // Verify that the location's institution matches the current institution context
        // This ensures that patients cannot be moved between institutions accidentally
        if (!patient.getLocation().getInstitution().getId().equals(institutionId)) {
            throw new IllegalStateException(
                "Cannot save patient: location's institution does not match current institution context. " +
                "Location institution: " + patient.getLocation().getInstitution().getId() + 
                ", Context institution: " + institutionId + ". " +
                "Please select a location that belongs to the current institution.");
        }
        
        // Set institution from location (required for unique constraints at institution level)
        patient.setInstitution(patient.getLocation().getInstitution());
        
        Patient saved;
        if (patient.getId() != null) {
            if (patient.getId() <= 0) {
                patient.setId(null);
                if (patient.getHealthInsurance() != null) {
                    HealthInsurance healthInsurance = patient.getHealthInsurance();
                    if (healthInsurance.getId() != null && healthInsurance.getId() <= 0) {
                        healthInsurance.setId(null);
                    }
                    HealthInsurance savedHealthInsurance = healthInsuranceRepository.save(healthInsurance);
                    patient.setHealthInsurance(savedHealthInsurance);
                }
                saved = patientRepository.save(patient);
            } else {
                Patient existingPatient = patientRepository.getReferenceById(patient.getId());
                if (patient.getHealthInsurance() != null) {
                    HealthInsurance healthInsurance = patient.getHealthInsurance();
                    if (healthInsurance.getId() != null && healthInsurance.getId() <= 0) {
                        healthInsurance.setId(null);
                    }
                    HealthInsurance savedHealthInsurance = healthInsuranceRepository.save(healthInsurance);
                    patient.setHealthInsurance(savedHealthInsurance);
                }
                mapper.updatePatientEntity(patient, existingPatient);
                saved = patientRepository.save(existingPatient);
            }
        } else {
            if (patient.getHealthInsurance() != null) {
                HealthInsurance healthInsurance = patient.getHealthInsurance();
                if (healthInsurance.getId() != null && healthInsurance.getId() <= 0) {
                    healthInsurance.setId(null);
                }
                HealthInsurance savedHealthInsurance = healthInsuranceRepository.save(healthInsurance);
                patient.setHealthInsurance(savedHealthInsurance);
            }
            saved = patientRepository.save(patient);
        }
        return saved;
    }

    public Patient findById(Integer id) {
        if (id == null) {
            return null;
        }
        
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            // Use institution-aware find method
            return patientRepository.findByIdAndInstitutionId(id, institutionId)
                    .orElseThrow(() -> new RuntimeException("Patient not found: " + id));
        }
        
        // No institution context - SUPER_ADMIN should not access patient data
        throw new IllegalStateException("Cannot access patient data without institution context");
    }

    public List<Patient> getAll() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            // Use institution-aware findAll method
            return patientRepository.findByInstitutionId(institutionId);
        }
        
        // No institution context - SUPER_ADMIN should not see patient data
        // Return empty list to enforce data isolation
        return List.of();
    }

    public Patient findEntityById(Integer id) {
        if (id == null) {
            return null;
        }
        
        // Use tenant-aware findById which calls findByIdAndTenantId if tenant is set
        return findById(id);
    }

    @Transactional
    public List<Patient> saveAll(List<Patient> patients) {
        Objects.requireNonNull(patients);
        
        // Set location and normalize insurance_number for all patients
        patients.forEach(patient -> {
            // Normalize insurance_number: empty strings should be treated as NULL
            if (patient.getInsuranceNumber() != null && patient.getInsuranceNumber().trim().isEmpty()) {
                patient.setInsuranceNumber(null);
            }
            
            setPracticeIfNeeded(patient);
            
            // Validate that location is set and has an institution
            if (patient.getLocation() == null) {
                throw new IllegalStateException(
                    "Cannot save patient without location. Patient: " + 
                    (patient.getFirstName() != null ? patient.getFirstName() : "?") + " " +
                    (patient.getLastName() != null ? patient.getLastName() : "?"));
            }
            
            if (patient.getLocation().getInstitution() == null) {
                throw new IllegalStateException(
                    "Cannot save patient: location must be assigned to an institution. Patient: " + 
                    (patient.getFirstName() != null ? patient.getFirstName() : "?") + " " +
                    (patient.getLastName() != null ? patient.getLastName() : "?"));
            }
            
            // Set institution from location (required for unique constraints at institution level)
            patient.setInstitution(patient.getLocation().getInstitution());
        });
        
        return patientRepository.saveAll(patients);
    }
    
    /**
     * Sets the location for a patient if not already set.
     * First tries to use the current user's preferred location, then falls back to default location.
     * Uses InstitutionContext to find the location for the current institution.
     * <p>
     * If the patient already has a location, it is kept as-is. The validation that the location's
     * institution matches the InstitutionContext is done in the save() method.
     * </p>
     * 
     * @throws IllegalStateException if no location can be found or set, or if InstitutionContext is not set
     */
    private void setPracticeIfNeeded(Patient patient) {
        // Set location if not already set or if location has no ID (transient)
        if (patient.getLocation() == null || 
            (patient.getLocation() != null && patient.getLocation().getId() == null)) {
            
            // Require InstitutionContext for setting location
            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId == null) {
                throw new IllegalStateException(
                    "Cannot set location for patient: InstitutionContext is not set. " +
                    "Please ensure you are logged in with an institution context.");
            }
            
            // Try to get preferred location from current user
            Location location = getCurrentUserPreferredLocation();
            
            // Fallback to default location if preferred location is not set
            if (location == null) {
                location = locationService.getDefaultLocation();
            }
            
            if (location != null) {
                // Verify that location has an institution
                if (location.getInstitution() == null) {
                    throw new IllegalStateException(
                        "Location found but has no institution assigned. " +
                        "Please ensure locations are properly configured with institutions.");
                }
                
                // Verify that location's institution matches InstitutionContext
                if (!location.getInstitution().getId().equals(institutionId)) {
                    throw new IllegalStateException(
                        "Location's institution does not match current institution context. " +
                        "Location institution: " + location.getInstitution().getId() + 
                        ", Context institution: " + institutionId);
                }
                
                patient.setLocation(location);
                return;
            }
        }
        
        // If patient already has a location, we keep it as-is
        // The validation that it matches the InstitutionContext is done in save()
        
        // Validate that location is set
        if (patient.getLocation() == null) {
            throw new IllegalStateException(
                "No location found. Ensure LocationService has a location configured for the current institution.");
        }
        
        // Validate that location has an institution (but don't check InstitutionContext match here)
        // This is done in save() to provide a clearer error message
        if (patient.getLocation().getInstitution() == null) {
            throw new IllegalStateException(
                "Location found but has no institution assigned. " +
                "Please ensure locations are properly configured with institutions.");
        }
    }
    
    /**
     * Gets the preferred location from the current user's UserAccount.
     * Returns null if no user is logged in or if the user has no preferred location.
     */
    private Location getCurrentUserPreferredLocation() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                String username = auth.getName();
                return userAccountRepository.findByUsername(username)
                        .map(UserAccount::getPreferredLocation)
                        .orElse(null);
            }
        } catch (Exception e) {
            // Log but don't fail - this is a fallback mechanism
            // LocationService will handle the fallback
        }
        return null;
    }

}