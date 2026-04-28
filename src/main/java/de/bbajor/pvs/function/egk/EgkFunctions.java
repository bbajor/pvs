package de.bbajor.pvs.function.egk;

import de.bbajor.pvs.common.function.FunctionRequest;
import de.bbajor.pvs.common.function.FunctionResponse;
import de.bbajor.pvs.egk.api.dto.EgkDataDto;
import de.bbajor.pvs.egk.service.EgkDataService;
import de.bbajor.pvs.function.core.FunctionWrapper;
import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Spring Cloud Functions for eGK Service.
 */
@Configuration
@RequiredArgsConstructor
public class EgkFunctions {
    
    private final EgkDataService egkDataService;
    private final CurrentInstitutionService currentInstitutionService;
    
    @Bean
    public Function<ProcessEgkDataRequest, ProcessEgkDataResponse> processEgkData() {
        return FunctionWrapper.wrap(currentInstitutionService,
            request -> {
                Patient patient = egkDataService.processPatientData(request.getEgkData());
                HealthInsurance healthInsurance = egkDataService.processHealthInsuranceData(request.getEgkData());
                patient.setHealthInsurance(healthInsurance);
                
                ProcessEgkDataResponse response = new ProcessEgkDataResponse();
                response.setPatient(patient);
                return response;
            },
            "processEgkData"
        );
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ProcessEgkDataRequest extends FunctionRequest {
        private EgkDataDto egkData;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ProcessEgkDataResponse extends FunctionResponse {
        private Patient patient;
    }
}


