package de.bbajor.pvs.patientsearch.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.patientsearch.repository.PatientSearchRepository;

@Service
@PreAuthorize("isAuthenticated()")
public class PatientSearchService {

    private PatientSearchRepository patientSearchRepository;

    public List<Patient> findPatients(String filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findPatients'");
    }
    
}
