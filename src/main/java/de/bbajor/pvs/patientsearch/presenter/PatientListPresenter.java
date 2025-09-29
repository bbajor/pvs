package de.bbajor.pvs.patientsearch.presenter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.service.PatientService;
import de.bbajor.pvs.patientsearch.dto.PatientDto;

@Component
public class PatientListPresenter {

    @Autowired
    private PatientService patientService;
    @Autowired
    private PatientDialogPresenter patientDialogPresenter;

    public List<PatientDto> findAll() {
        return patientService.getAll();
    }

    public List<PatientDto> findAllBy(String searchString) {
        return patientService.findPatients(searchString);
    }

    public PatientDialogPresenter getDialogPresenter() {
        return patientDialogPresenter;
    }
}
