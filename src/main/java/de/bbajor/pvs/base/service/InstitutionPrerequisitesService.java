package de.bbajor.pvs.base.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;
import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.base.util.TimeSlotRepetition;

/**
 * Service zur Prüfung, ob alle notwendigen Grunddaten für eine Institution vorhanden sind.
 * Wird verwendet, um Buttons zu deaktivieren und Hinweise anzuzeigen.
 */
@Service
public class InstitutionPrerequisitesService {

    @Autowired
    private LocationService locationService;
    
    @Autowired
    private PatientService patientService;
    
    @Autowired
    private SurgicalCenterService surgicalCenterService;

    /**
     * Prüft, ob mindestens ein aktiver Standort vorhanden ist.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveLocation() {
        if (!InstitutionContext.hasInstitution()) {
            return false;
        }
        Location defaultLocation = locationService.getDefaultLocation();
        return defaultLocation != null;
    }

    /**
     * Prüft, ob mindestens ein Patient vorhanden ist.
     */
    @Transactional(readOnly = true)
    public boolean hasPatients() {
        if (!InstitutionContext.hasInstitution()) {
            return false;
        }
        List<Patient> patients = patientService.getAll();
        return patients != null && !patients.isEmpty();
    }

    /**
     * Prüft, ob mindestens eine operative Einrichtung vorhanden ist.
     */
    @Transactional(readOnly = true)
    public boolean hasSurgicalCenter() {
        if (!InstitutionContext.hasInstitution()) {
            return false;
        }
        List<SurgicalCenter> centers = surgicalCenterService.getSurgicalCenters();
        return centers != null && !centers.isEmpty();
    }

    /**
     * Prüft, ob mindestens eine operative Einrichtung mit verfügbaren Zeitslots vorhanden ist.
     */
    @Transactional(readOnly = true)
    public boolean hasSurgicalCenterWithTimeSlots() {
        if (!InstitutionContext.hasInstitution()) {
            return false;
        }
        
        if (!hasSurgicalCenter()) {
            return false;
        }
        
        // Prüfe, ob es verfügbare Zeitslots gibt
        try {
            var timeSlots = surgicalCenterService.findAvailableTimeSlotsFilteredBy(
                java.time.LocalDate.now(),
                TimePeriod.THREE_MONTHS,
                null // Alle Einrichtungen
            );
            return timeSlots != null && !timeSlots.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gibt eine Liste aller fehlenden Voraussetzungen zurück.
     */
    @Transactional(readOnly = true)
    public List<String> getMissingPrerequisites() {
        List<String> missing = new ArrayList<>();
        
        if (!InstitutionContext.hasInstitution()) {
            missing.add("Keine Institution ausgewählt");
            return missing;
        }
        
        if (!hasActiveLocation()) {
            missing.add("Mindestens ein aktiver Standort muss in den Einstellungen unter 'Standorte' angelegt werden");
        }
        
        return missing;
    }

    /**
     * Gibt eine Liste aller fehlenden Voraussetzungen für die Patientenanlage zurück.
     */
    @Transactional(readOnly = true)
    public List<String> getMissingPrerequisitesForPatientCreation() {
        List<String> missing = getMissingPrerequisites();
        return missing;
    }

    /**
     * Gibt eine Liste aller fehlenden Voraussetzungen für die Erstellung eines IVOM-Behandlungsplans zurück.
     */
    @Transactional(readOnly = true)
    public List<String> getMissingPrerequisitesForTreatmentPlanCreation() {
        List<String> missing = getMissingPrerequisites();
        
        if (!hasPatients()) {
            missing.add("Mindestens ein Patient muss vorhanden sein");
        }
        
        if (!hasSurgicalCenterWithTimeSlots()) {
            if (!hasSurgicalCenter()) {
                missing.add("Mindestens eine operative Einrichtung muss in den Einstellungen unter 'Terminplaner' angelegt werden");
            } else {
                missing.add("Mindestens eine operative Einrichtung muss verfügbare Zeitslots haben");
            }
        }
        
        return missing;
    }

    /**
     * Prüft, ob alle Voraussetzungen für die Patientenanlage erfüllt sind.
     */
    @Transactional(readOnly = true)
    public boolean canCreatePatient() {
        return getMissingPrerequisitesForPatientCreation().isEmpty();
    }

    /**
     * Prüft, ob alle Voraussetzungen für die Erstellung eines IVOM-Behandlungsplans erfüllt sind.
     */
    @Transactional(readOnly = true)
    public boolean canCreateTreatmentPlan() {
        return getMissingPrerequisitesForTreatmentPlanCreation().isEmpty();
    }
}

