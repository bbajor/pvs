package de.bbajor.pvs.patientsearch.presenter;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.service.HealthInsuranceService;
import de.bbajor.pvs.base.service.PatientService;
import de.bbajor.pvs.base.util.ModelToDtoMapper;
import de.bbajor.pvs.egk.reader.EgkReader;
import de.bbajor.pvs.intravitreal.treatment.service.IvomDiagnosisService;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.medication.service.IntravitrealMedicationService;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;

@Component
public class PatientListPresenter {

    private final IntravitrealMedicationService ivomDrugService;
    private final TreatmentPlanService ivomPlanService;
    private final SurgicalCenterService surgeryUnitService;
    private final PatientService patientService;
    private final HealthInsuranceService healthInsuranceService;
    private final IvomDiagnosisService ivomDiagnosisService;

    private final ModelToDtoMapper modelToDtoMapper;
    private final EgkReader egkReader;

    public PatientListPresenter(PatientService patientService, HealthInsuranceService healthInsuranceService,
            EgkReader egkReader, ModelToDtoMapper modelToDtoMapper, SurgicalCenterService surgeryUnitService,
            TreatmentPlanService ivomPlanService, IntravitrealMedicationService ivomDrugService,
            IvomDiagnosisService ivomDiagnosisService) {
        this.patientService = patientService;
        this.healthInsuranceService = healthInsuranceService;
        this.egkReader = egkReader;
        this.modelToDtoMapper = modelToDtoMapper;
        this.surgeryUnitService = surgeryUnitService;
        this.ivomPlanService = ivomPlanService;
        this.ivomDrugService = ivomDrugService;
        this.ivomDiagnosisService = ivomDiagnosisService;
    }

    public List<PatientDto> findAll() {
        return patientService.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public PatientDialogPresenter getDialogPresenter() {
        return new PatientDialogPresenter(patientService, healthInsuranceService, egkReader, modelToDtoMapper,
                surgeryUnitService, ivomPlanService, ivomDrugService, ivomDiagnosisService);
    }

    private PatientDto mapToDto(Patient entity) {
        return modelToDtoMapper.toDto(entity);
    }

    public List<PatientDto> findAllBy(String searchString) {
        return StringUtils.isEmpty(searchString) ? findAll()
                : patientService.findPatients(searchString)
                        .stream()
                        .map(this::mapToDto)
                        .toList();
    }
}
