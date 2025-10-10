package de.bbajor.pvs.patient.presenter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;

@Component
public class PatientListPresenter {

    @Autowired
    private PatientService patientService;
    @Autowired
    private PatientPresenter patientDialogPresenter;

    public List<Patient> findAll() {
        return patientService.getAll();
    }

    public List<Patient> findAllBy(String searchString) {
        return patientService.findPatients(searchString);
    }

    public PatientPresenter getDialogPresenter() {
        return patientDialogPresenter;
    }
}
