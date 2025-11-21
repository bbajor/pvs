package de.bbajor.pvs.patient.presenter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
    
    public Slice<Patient> findAll(Pageable pageable) {
        return patientService.getAll(pageable);
    }
    
    public Slice<Patient> findAllBy(String searchString, Pageable pageable) {
        return patientService.findPatients(searchString, pageable);
    }

    public PatientPresenter getDialogPresenter() {
        return patientDialogPresenter;
    }
}
