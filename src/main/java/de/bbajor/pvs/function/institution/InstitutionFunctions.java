package de.bbajor.pvs.function.institution;

import de.bbajor.pvs.function.core.FunctionWrapper;
import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.service.InstitutionService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Spring Cloud Functions for Institution Service.
 */
@Configuration
@RequiredArgsConstructor
public class InstitutionFunctions {
    
    private final InstitutionService institutionService;
    private final CurrentInstitutionService currentInstitutionService;
    
    @Bean
    public Function<InstitutionCreateRequest, InstitutionResponse> createInstitution() {
        return FunctionWrapper.wrap(currentInstitutionService,
            request -> {
                Institution institution = institutionService.createInstitution(request.getInstitutionName());
                InstitutionResponse response = new InstitutionResponse();
                response.setInstitution(institution);
                return response;
            },
            "createInstitution"
        );
    }
    
    @Bean
    public Function<InstitutionFindRequest, InstitutionResponse> getInstitution() {
        return FunctionWrapper.wrap(currentInstitutionService,
            request -> {
                Optional<Institution> institution = institutionService.findByCode(request.getInstitutionCode());
                InstitutionResponse response = new InstitutionResponse();
                institution.ifPresent(response::setInstitution);
                if (institution.isEmpty()) {
                    response.setErrorMessage("Institution not found: " + request.getInstitutionCode());
                    response.setSuccess(false);
                }
                return response;
            },
            "getInstitution"
        );
    }
    
    @Bean
    public Function<InstitutionListRequest, InstitutionListResponse> listInstitutions() {
        return FunctionWrapper.wrap(currentInstitutionService,
            request -> {
                List<Institution> institutions = institutionService.findAll();
                InstitutionListResponse response = new InstitutionListResponse();
                response.setInstitutions(institutions);
                return response;
            },
            "listInstitutions"
        );
    }
    
    // Request/Response classes
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class InstitutionCreateRequest extends InstitutionFunctionRequest {
        private String institutionName;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class InstitutionFindRequest extends InstitutionFunctionRequest {
        private String institutionCode;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class InstitutionListRequest extends InstitutionFunctionRequest {
        // No additional fields needed
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class InstitutionFunctionRequest extends de.bbajor.pvs.common.function.FunctionRequest {
        // Institution ID is inherited
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class InstitutionResponse extends de.bbajor.pvs.common.function.FunctionResponse {
        private Institution institution;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class InstitutionListResponse extends de.bbajor.pvs.common.function.FunctionResponse {
        private List<Institution> institutions;
    }
}


