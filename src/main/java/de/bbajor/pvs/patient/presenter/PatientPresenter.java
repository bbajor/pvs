package de.bbajor.pvs.patient.presenter;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.egk.reader.EgkReader;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.service.IvomDiagnosisService;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.medication.service.MedicationFavouriteService;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.HealthInsuranceService;
import de.bbajor.pvs.patient.service.PatientMapper;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;
import jakarta.transaction.Transactional;

@Component
@Scope("prototype")
public class PatientPresenter {

    @Autowired
    private MedicationFavouriteService medicationFavouriteService;
    @Autowired
    private TreatmentPlanService treatmentPlanService;
    @Autowired
    private IvomDiagnosisService ivomDiagnosisService;
    @Autowired
    private SurgicalCenterService surgicalCenterService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private HealthInsuranceService healthInsuranceService;
    @Autowired
    private EgkReader egkReader;
    @Autowired
    private PatientMapper patientMapper;
    @Autowired
    private LocationService locationService;

    @Transactional
    public Patient savePatient(Patient update) {
        if (update.getId() != null) {
            Patient existingPatient = patientService.findEntityById(update.getId());
            patientMapper.updatePatientEntity(update, existingPatient);
            return patientService.save(existingPatient);
        } else {
            if (update.getHealthInsurance() != null && update.getHealthInsurance().getId() != null) {
                update.setHealthInsurance(healthInsuranceService.findById(update.getHealthInsurance()));
            }
            return patientService.save(update);
        }
    }

    public Patient readDataFromEgk() throws Exception {
        Patient patientDto = egkReader.readPatientFromCard();
        HealthInsurance healthInsurance = egkReader.readHealthInsuranceFromCard();
        patientDto.setHealthInsurance(healthInsurance);
        return patientDto;
    }

    public List<Patient> getPatients() {
        return patientService.getAll();
    }

    public List<HealthInsurance> getHealthInsurances() {
        return healthInsuranceService.findAll();
    }

    public List<MedicationFavourite> getDrugs() {
        return medicationFavouriteService.getActiveFavouritesForCurrentInstitution();
    }

    public TreatmentPlan findById(Long id) {
        return treatmentPlanService.loadTreatmentPlanWithFullDetails(id);
    }

    public TreatmentPlan saveTreatmentPlan(TreatmentPlan treatmentPlan) {
        return treatmentPlanService.saveTreatmentPlan(treatmentPlan);
    }

    public List<SurgicalCenter> getSurgicalCenterList() {
        return surgicalCenterService.findAll();
    }

    public Diagnosis saveDiagnosis(Diagnosis diagnosis) {
        return ivomDiagnosisService.save(diagnosis);
    }

    public Collection<Diagnosis> getDiagnoses() {
        return ivomDiagnosisService.getDiagnoses();
    }

    public List<Location> getLocations() {
        return locationService.getAllLocations();
    }

    public HealthInsuranceService getHealthInsuranceService() {
        return healthInsuranceService;
    }
}
