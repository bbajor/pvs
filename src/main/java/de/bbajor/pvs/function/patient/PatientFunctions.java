package de.bbajor.pvs.function.patient;

import de.bbajor.pvs.function.core.FunctionWrapper;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Function;

/**
 * Spring Cloud Functions for Patient Service.
 * 
 * These functions can be deployed as serverless functions to OpenFaaS, AWS Lambda, etc.
 */
@Configuration
@RequiredArgsConstructor
public class PatientFunctions {
    
    private final PatientService patientService;
    
    /**
     * Create a new patient.
     */
    @Bean
    public Function<PatientCreateRequest, PatientResponse> createPatient() {
        return FunctionWrapper.wrap(
            request -> {
                Patient patient = request.getPatient();
                Patient saved = patientService.save(patient);
                
                PatientResponse response = new PatientResponse();
                response.setPatient(saved);
                return response;
            },
            "createPatient"
        );
    }
    
    /**
     * Update an existing patient.
     */
    @Bean
    public Function<PatientUpdateRequest, PatientResponse> updatePatient() {
        return FunctionWrapper.wrap(
            request -> {
                Patient existing = patientService.findById(request.getPatientId());
                // Update fields from request
                Patient updated = request.getPatient();
                // Copy ID to ensure we're updating the right patient
                updated.setId(existing.getId());
                Patient saved = patientService.save(updated);
                
                PatientResponse response = new PatientResponse();
                response.setPatient(saved);
                return response;
            },
            "updatePatient"
        );
    }
    
    /**
     * Find a patient by ID.
     */
    @Bean
    public Function<PatientFindRequest, PatientResponse> findPatient() {
        return FunctionWrapper.wrap(
            request -> {
                Patient patient = patientService.findById(request.getPatientId());
                
                PatientResponse response = new PatientResponse();
                response.setPatient(patient);
                return response;
            },
            "findPatient"
        );
    }
    
    /**
     * Search for patients by name.
     */
    @Bean
    public Function<PatientSearchRequest, PatientResponse> searchPatients() {
        return FunctionWrapper.wrap(
            request -> {
                List<Patient> patients = patientService.findPatients(request.getSearchTerm());
                
                PatientResponse response = new PatientResponse();
                response.setPatients(patients);
                return response;
            },
            "searchPatients"
        );
    }
    
    /**
     * Get all patients for the current institution.
     */
    @Bean
    public Function<PatientFunctionRequest, PatientResponse> getAllPatients() {
        return FunctionWrapper.wrap(
            request -> {
                List<Patient> patients = patientService.getAll();
                
                PatientResponse response = new PatientResponse();
                response.setPatients(patients);
                return response;
            },
            "getAllPatients"
        );
    }
}


