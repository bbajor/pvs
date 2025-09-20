package de.bbajor.pvs.patientsearch.presenter;

import java.util.List;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.misc.ModelToDtoMapper;
import de.bbajor.pvs.base.service.PatientService;
import de.bbajor.pvs.egk.reader.EgkReader;
import de.bbajor.pvs.patientsearch.dto.HealthInsuranceDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;

@Component
public class PatientDialogPresenter {

    private final PatientService patientService;
    private final EgkReader egkReader;
    private final ModelToDtoMapper modelToDtoMapper;
    private PatientDto workingCopy;
    private Patient original;


    public PatientDialogPresenter(PatientService patientService, EgkReader egkReader, ModelToDtoMapper modelToDtoMapper) {
        this.patientService = patientService;
        this.egkReader = egkReader;
        this.modelToDtoMapper = modelToDtoMapper;
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

    public void saveChanges() {
        if (workingCopy == null) {
            throw new IllegalStateException("No data loaded into dialog");
        }
        if (original == null) { // new patient
            Patient newEntity = modelToDtoMapper.toEntity(workingCopy);
            patientService.save(newEntity);
        } else { // existing patient
            modelToDtoMapper.updateDtoFromEntity(original, workingCopy);
            patientService.save(original);
        }
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
            getWorkingCopy().setBirth(patientData.getBirth())
                    .setEmail(patientData.getEmail())
                    .setFirstName(patientData.getFirstName())
                    .setLastName(patientData.getLastName())
                    .setPhone(patientData.getPhone())
                    .setAddress(patientData.getAddress())
                    .setInsuranceId(patientData.getInsuranceId());
            HealthInsuranceDto healthInsurance = egkReader.readHealthInsuranceFromCard();
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

}
