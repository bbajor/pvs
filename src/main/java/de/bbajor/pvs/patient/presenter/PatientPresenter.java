package de.bbajor.pvs.patient.presenter;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.egk.reader.EgkReader;
import de.bbajor.pvs.intravitreal.treatment.dto.DiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.intravitreal.treatment.service.IvomDiagnosisService;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.medication.dto.MedicationDto;
import de.bbajor.pvs.medication.service.IntravitrealMedicationService;
import de.bbajor.pvs.patient.dto.HealthInsuranceDto;
import de.bbajor.pvs.patient.dto.PatientDto;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.HealthInsuranceService;
import de.bbajor.pvs.patient.service.PatientMapper;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;
import jakarta.transaction.Transactional;

@Component
@Scope("prototype")
public class PatientPresenter {

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

    @Transactional
    public PatientDto save(PatientDto dtoToSave) {
        if (dtoToSave.getId() != null) {
            Patient existingPatient = patientService.findEntityById(dtoToSave.getId());
            patientMapper.updateEntityFromDto(dtoToSave, existingPatient);
            return patientService.save(existingPatient);
        } else {
            Patient newEntity = patientMapper.toEntity(dtoToSave);
            if (newEntity.getAddress() != null) {
                newEntity.getAddress().setId(null);
            }
            if (newEntity.getHealthInsurance() != null && newEntity.getHealthInsurance().getId() != null) {
                newEntity.setHealthInsurance(healthInsuranceService.findById(newEntity.getHealthInsurance()));
            }
            return patientService.save(newEntity);
        }
    }

    public PatientDto readDataFromEgk() throws Exception {
        PatientDto patientDto = egkReader.readPatientFromCard();
        HealthInsuranceDto healthInsurance = egkReader.readHealthInsuranceFromCard();
        patientDto.setHealthInsurance(healthInsurance);
        return patientDto;
    }

    public List<PatientDto> getPatients() {
        return patientService.findAll();
    }

    public List<HealthInsuranceDto> getHealthInsurances() {
        return healthInsuranceService.findAll();
    }

    public List<MedicationDto> getDrugs() {
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
