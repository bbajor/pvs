package de.bbajor.pvs.function.treatment;

import de.bbajor.pvs.common.function.FunctionRequest;
import de.bbajor.pvs.common.function.FunctionResponse;
import de.bbajor.pvs.function.core.FunctionWrapper;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.taskmanagement.service.TaskService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Spring Cloud Functions for Treatment Service.
 */
@Configuration
@RequiredArgsConstructor
public class TreatmentFunctions {
    
    private final TaskService taskService;
    
    @Bean
    public Function<ApproveTreatmentRequest, TreatmentResponse> approveTreatment() {
        return FunctionWrapper.wrap(
            request -> {
                String actorUserId = request.getSecurityContext() != null 
                    ? String.valueOf(request.getSecurityContext().getUserId()) 
                    : "SYSTEM";
                String actorUserName = request.getSecurityContext() != null 
                    ? request.getSecurityContext().getUsername() 
                    : "SYSTEM";
                
                taskService.approveTreatment(
                    request.getTreatmentId(),
                    actorUserId,
                    actorUserName,
                    request.isSecondApproval()
                );
                
                TreatmentResponse response = new TreatmentResponse();
                response.setSuccess(true);
                return response;
            },
            "approveTreatment"
        );
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ApproveTreatmentRequest extends FunctionRequest {
        private Long treatmentId;
        private boolean secondApproval = false;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TreatmentResponse extends FunctionResponse {
        private Treatment treatment;
    }
}


