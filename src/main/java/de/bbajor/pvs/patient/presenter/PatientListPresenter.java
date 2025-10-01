package de.bbajor.pvs.patient.presenter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.patient.dto.PatientDto;
import de.bbajor.pvs.patient.service.PatientService;

@Component
public class PatientListPresenter {

    @Autowired
    private PatientService patientService;
    @Autowired
    private PatientPresenter patientDialogPresenter;

    public List<PatientDto> findAll() {
        return patientService.getAll();
    }

    public List<PatientDto> findAllBy(String searchString) {
        return patientService.findPatients(searchString);
    }

    public PatientPresenter getDialogPresenter() {
        return patientDialogPresenter;
    }
}
