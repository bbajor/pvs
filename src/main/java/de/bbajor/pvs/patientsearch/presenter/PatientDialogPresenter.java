package de.bbajor.pvs.patientsearch.presenter;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.service.PatientService;
import de.bbajor.pvs.egk.reader.EgkReader;
import de.bbajor.pvs.patientsearch.dto.HealthInsuranceDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;

@Component
public class PatientDialogPresenter {

    private final PatientService patientService;
    private final EgkReader egkReader;
    private PatientDto workingCopy;
    private Patient original;

    public PatientDialogPresenter(PatientService patientService, EgkReader egkReader) {
        this.patientService = patientService;
        this.egkReader = egkReader;
    }

    public void loadPatientById(Integer id) {
        if (id != null) {
            this.original = patientService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + id));
            this.workingCopy = copyFromEntity(original);
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
            Patient newEntity = mapToEntity(new Patient(), workingCopy);
            patientService.save(newEntity);
        } else { // existing patient
            mapToEntity(original, workingCopy);
            patientService.save(original);
        }
    }

    public PatientDto getWorkingCopy() {
        if (workingCopy == null) {
            workingCopy = new PatientDto();
        }
        return workingCopy;
    }

    private PatientDto copyFromEntity(Patient e) {
        PatientDto dto = new PatientDto();
        dto.setBirth(e.getBirth())
                .setEmail(e.getEmail())
                .setFirstName(e.getFirstName())
                .setLastName(e.getLastName())
                .setPhone(e.getPhone());
        // TODO: map other fields
        return dto;
    }

    private Patient mapToEntity(Patient entity, PatientDto dto) {

        if (dto == null) {
            return null;
        }

        if (entity == null) {
            entity = new Patient();
        }
        entity
                .setFirstName(dto.getFirstName())
                .setLastName(dto.getLastName())
                .setBirth(dto.getBirth())
                .setEmail(dto.getEmail())
                .setPhone(dto.getPhone());
        // TODO: map other fields
        return entity;
    }

    public void readDataFromEgk() {
        try {
            PatientDto patientData = egkReader.readPatientFromCard();
            getWorkingCopy().setBirth(patientData.getBirth())
                    .setEmail(patientData.getEmail())
                    .setFirstName(patientData.getFirstName())
                    .setLastName(patientData.getLastName())
                    .setPhone(patientData.getPhone())
                    .setPatientAddress(patientData.getPatientAddress())
                    .setInsuranceId(patientData.getInsuranceId());
            HealthInsuranceDto healthInsurance = egkReader.readHealthInsuranceFromCard();
            getWorkingCopy().setHealthInsurance(healthInsurance);
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Lesen der eGK: " + e.getMessage(), e);
        }
    }

}
