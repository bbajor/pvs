package de.bbajor.pvs.patientsearch.presenter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.flow.data.binder.BinderValidationStatus;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.service.HealthInsuranceService;
import de.bbajor.pvs.base.service.PatientMapper;
import de.bbajor.pvs.base.service.PatientService;
import de.bbajor.pvs.egk.reader.EgkReader;
import de.bbajor.pvs.intravitreal.treatment.dto.DiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.intravitreal.treatment.service.IvomDiagnosisService;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.medication.dto.IntravitrealMedicationDto;
import de.bbajor.pvs.medication.service.IntravitrealMedicationService;
import de.bbajor.pvs.patientsearch.dto.HealthInsuranceDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.patientsearch.ui.view.PatientForm;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;

@Component
public class PatientDialogPresenter {

    @Autowired
    private IntravitrealMedicationService ivomDrugService;
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

    private PatientDto workingCopy;
    private Patient original;

    public void loadPatientById(Integer id) {
        if (id != null) {
            Optional<Patient> patient = patientService.findEntityById(id);
            this.original = patient
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + id));
            this.workingCopy = patientMapper.toDto(original);
        } else {
            this.original = null;
            this.workingCopy = new PatientDto(); // leere WorkingCopy für Neuanlage
        }
    }

    public BinderValidationStatus<PatientDto> saveChanges(PatientForm form) {
        // TODO BinderValidationStatus in View auslagern
        BinderValidationStatus<PatientDto> validationStatus = form.validate();
        if (validationStatus.isOk()) {
            if (original == null) { // new patient
                PatientDto newPatient = form.getPatient();
                patientMapper.updateDto(newPatient, getWorkingCopy());
                Patient newEntity = patientMapper.toEntity(getWorkingCopy());
                if (newEntity.getAddress() != null) {
                    newEntity.getAddress().setId(null);
                }
                workingCopy = patientService.save(newEntity);
            } else { // existing patient
                patientMapper.updateDtoFromEntity(original, getWorkingCopy());
                workingCopy = patientService.save(original);
            }
            loadPatientById(workingCopy.getId());
        }
        return validationStatus;
    }

    public PatientDto getWorkingCopy() {
        if (workingCopy == null) {
            workingCopy = new PatientDto();
        }
        return workingCopy;
    }

    public void readDataFromEgk() {
        try {
            PatientDto patientData = egkReader.readPatientFromCard();
            HealthInsuranceDto healthInsurance = egkReader.readHealthInsuranceFromCard();
            patientMapper.updateDto(patientData, getWorkingCopy());
            getWorkingCopy().setInsuranceId(patientData.getInsuranceId());
            getWorkingCopy().setHealthInsurance(healthInsurance);
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Lesen der eGK: " + e.getMessage(), e);
        }
    }

    public List<PatientDto> getPatients() {
        return patientService.findAll();
    }

    public List<HealthInsuranceDto> getHealthInsurances() {
        return healthInsuranceService.findAll();
    }

    public List<IntravitrealMedicationDto> getDrugs() {
        return ivomDrugService.getMedicationListFavorites();
    }

    public TreatmentPlanDto findById(Long id) {
        return treatmentPlanService.loadTreatmentPlanDto(id);
    }

    public void save(TreatmentPlanDto treatmentPlan) {
        treatmentPlanService.saveTreatmentPlan(treatmentPlan);
    }

    public List<SurgicalCenterDto> getSurgicalCenterList() {
        return surgicalCenterService.findAll();
    }

    public DiagnosisDto save(DiagnosisDto dto) {
        return ivomDiagnosisService.save(dto);
    }

    public List<SurgicalCenterTimeSlot> getAvailableSurgeryUnitTimeSlots(Integer id) {
        return surgicalCenterService.findTimeSlotsBySurgicalCenterId(id);
    }

    public Collection<DiagnosisDto> getDiagnoses() {
        return ivomDiagnosisService.getDiagnosisDtos();
    }

}
