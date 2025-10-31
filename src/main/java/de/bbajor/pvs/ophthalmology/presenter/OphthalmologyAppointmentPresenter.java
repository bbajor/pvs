package de.bbajor.pvs.ophthalmology.presenter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;

@Component
public class OphthalmologyAppointmentPresenter {

    @Autowired
    private PatientService patientService;

    public List<Patient> getPatients() {
        return patientService.getAll();
    }
}
