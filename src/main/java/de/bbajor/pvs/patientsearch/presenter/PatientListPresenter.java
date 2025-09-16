package de.bbajor.pvs.patientsearch.presenter;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.service.PatientService;
import de.bbajor.pvs.patientsearch.dto.PatientDto;

@Component
public class PatientListPresenter {

    private final PatientService patientService;
    private final EgkReader egkReader;

    public PatientListPresenter(PatientService patientService, EgkReader egkReader) {
        this.patientService = patientService;
        this.egkReader = egkReader;
    }

    public List<PatientDto> findAll() {
        return patientService.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public PatientDialogPresenter getDialogPresenter() {
        return new PatientDialogPresenter(patientService, egkReader);
    }

    private PatientDto mapToDto(Patient entity) {
        PatientDto dto = new PatientDto();
        dto.setPatientId(entity.getId())
                .setFirstName(entity.getFirstName())
                .setLastName(entity.getLastName())
                .setBirth(entity.getBirth());
        // TODO: map other fields
        return dto;
    }

    public List<PatientDto> findAllBy(String searchString) {
        return StringUtils.isEmpty(searchString) ? findAll()
                : patientService.findPatients(searchString)
                        .stream()
                        .map(this::mapToDto)
                        .toList();
    }
}
