package de.bbajor.pvs.patientsearch.presenter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.vaadin.flow.data.binder.BinderValidationStatus;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.service.HealthInsuranceService;
import de.bbajor.pvs.base.service.PatientService;
import de.bbajor.pvs.base.util.ModelToDtoMapper;
import de.bbajor.pvs.egk.reader.EgkReader;
import de.bbajor.pvs.intravitreal.treatment.dto.IvomDiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.model.IvomDiagnosis;
import de.bbajor.pvs.intravitreal.treatment.model.IvomPlan;
import de.bbajor.pvs.intravitreal.treatment.service.IvomDiagnosisService;
import de.bbajor.pvs.intravitreal.treatment.service.IvomPlanService;
import de.bbajor.pvs.medication.dto.IntravitrealMedicationDto;
import de.bbajor.pvs.medication.model.IntravitrealMedication;
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

    private final IntravitrealMedicationService ivomDrugService;
    private final IvomPlanService ivomPlanService;
    private final IvomDiagnosisService ivomDiagnosisService;
    private final SurgicalCenterService surgeryUnitService;
    private final PatientService patientService;
    private final HealthInsuranceService healthInsuranceService;
    private final EgkReader egkReader;
    private final ModelToDtoMapper modelToDtoMapper;
    private PatientDto workingCopy;
    private Patient original;

    public PatientDialogPresenter(PatientService patientService, HealthInsuranceService healthInsuranceService,
            EgkReader egkReader, ModelToDtoMapper modelToDtoMapper, SurgicalCenterService surgeryUnitService,
            IvomPlanService ivomPlanService, IntravitrealMedicationService ivomDrugService,
            IvomDiagnosisService ivomDiagnosisService) {
        this.patientService = patientService;
        this.healthInsuranceService = healthInsuranceService;
        this.surgeryUnitService = surgeryUnitService;
        this.ivomPlanService = ivomPlanService;
        this.egkReader = egkReader;
        this.modelToDtoMapper = modelToDtoMapper;
        this.ivomDrugService = ivomDrugService;
        this.ivomDiagnosisService = ivomDiagnosisService;
    }

    public void loadPatientById(Integer id) {
        if (id != null) {
            this.original = patientService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + id));
            this.workingCopy = modelToDtoMapper.toDto(original);
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
                modelToDtoMapper.updateDto(newPatient, getWorkingCopy());
                Patient newEntity = modelToDtoMapper.toEntity(getWorkingCopy());
                if (newEntity.getAddress() != null) {
                    newEntity.getAddress().setId(null);
                }
                patientService.save(newEntity);
            } else { // existing patient
                modelToDtoMapper.updateDtoFromEntity(original, getWorkingCopy());
                patientService.save(original);
            }
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
            modelToDtoMapper.updateDto(patientData, getWorkingCopy());
            getWorkingCopy().setInsuranceId(patientData.getInsuranceId());
            getWorkingCopy().setHealthInsurance(healthInsurance);
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Lesen der eGK: " + e.getMessage(), e);
        }
    }

    public List<PatientDto> getPatients() {
        return patientService.findAll().stream().map(this::copyFromEntity).toList();
    }

    public PatientDto copyFromEntity(Patient patient) {
        return modelToDtoMapper.toDto(patient);
    }

    public List<HealthInsuranceDto> getHealthInsurances() {
        return healthInsuranceService.findAll();
    }

    public List<IntravitrealMedicationDto> getDrugs() {
        return ivomDrugService.findAll().stream().map(this::toDto).toList();
    }

    private IntravitrealMedicationDto toDto(IntravitrealMedication ivomDrug) {
        return modelToDtoMapper.toDto(ivomDrug);
    }

    public Optional<IvomPlan> findById(Long id) {
        return ivomPlanService.findById(id);
    }

    public void save(IvomPlan entity) {
        ivomPlanService.save(entity);
    }

    public List<SurgicalCenterDto> getSurgeryUnits() {
        Collection<SurgicalCenter> surgeryUnits = surgeryUnitService.findAll();
        return surgeryUnits.stream()
                .map(modelToDtoMapper::toDto)
                .toList();
    }

    public IvomDiagnosisDto save(IvomDiagnosis newEntity) {
        IvomDiagnosis ivomDiagnosis = ivomDiagnosisService.save(newEntity);
        return toDto(ivomDiagnosis);
    }

    public List<SurgicalCenterTimeSlot> getAvailableSurgeryUnitTimeSlots(Integer id) {
        return surgeryUnitService.findSurgeryUnitTimeSlots(id);
    }

    public Collection<IvomDiagnosisDto> getDiagnoses() {
        Collection<IvomDiagnosis> diagnoses = ivomDiagnosisService.findAll();
        if (diagnoses == null || diagnoses.isEmpty()) {
            return Collections.emptyList();
        }
        return diagnoses.stream().map(this::toDto).toList();
    }

    private IvomDiagnosisDto toDto(IvomDiagnosis entity) {
        return modelToDtoMapper.toDto(entity);
    }

}
