package de.bbajor.pvs.patient.ui.view;

import de.bbajor.pvs.patient.dto.PatientDto;

public interface PatientChangeListener {
    void onPatientChanged(PatientDto patientDto);
}
