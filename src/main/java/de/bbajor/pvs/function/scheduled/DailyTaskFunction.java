package de.bbajor.pvs.function.scheduled;

import de.bbajor.pvs.common.function.FunctionRequest;
import de.bbajor.pvs.common.function.FunctionResponse;
import de.bbajor.pvs.common.security.SecurityContext;
import de.bbajor.pvs.function.core.FunctionWrapper;
import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.taskmanagement.service.TaskService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Function;

/**
 * Scheduled Task Function for daily task creation.
 * 
 * This function replaces the @Scheduled annotation and can be triggered
 * by OpenFaaS Cron, AWS EventBridge, or any other scheduler.
 */
@Configuration
@RequiredArgsConstructor
public class DailyTaskFunction {
    
    private static final Logger LOG = LogManager.getLogger(DailyTaskFunction.class);
    
    private final TaskService taskService;
    private final InstitutionRepository institutionRepository;
    private final CurrentInstitutionService currentInstitutionService;
    
    /**
     * Daily task creation function.
     * Can be triggered by cron scheduler (e.g., OpenFaaS Cron).
     */
    @Bean
    public Function<DailyTaskRequest, DailyTaskResponse> scheduledDailyTask() {
        return FunctionWrapper.wrap(currentInstitutionService,
            request -> {
                LOG.info("Daily task function started");
                
                // Get all active institutions
                List<Institution> activeInstitutions = institutionRepository.findAll().stream()
                        .filter(Institution::isActive)
                        .toList();
                
                LOG.info("Found {} active institutions", activeInstitutions.size());
                
                int successCount = 0;
                int errorCount = 0;
                
                for (Institution institution : activeInstitutions) {
                    try {
                        LOG.info("Creating tasks for institution: {} (ID: {})", 
                                institution.getInstitutionCode(), institution.getId());
                        taskService.createDailyTaskIfAnyForInstitution(institution.getId());
                        successCount++;
                    } catch (Exception e) {
                        LOG.error("Error creating tasks for institution {}: {}", 
                                institution.getInstitutionCode(), e.getMessage(), e);
                        errorCount++;
                    }
                }
                
                DailyTaskResponse response = new DailyTaskResponse();
                response.setProcessedInstitutions(activeInstitutions.size());
                response.setSuccessCount(successCount);
                response.setErrorCount(errorCount);
                response.setSuccess(errorCount == 0);
                
                if (errorCount > 0) {
                    response.setErrorMessage(String.format(
                            "Completed with %d errors out of %d institutions", 
                            errorCount, activeInstitutions.size()));
                }
                
                LOG.info("Daily task function completed: {} success, {} errors", 
                        successCount, errorCount);
                
                return response;
            },
            "scheduledDailyTask"
        );
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class DailyTaskRequest extends FunctionRequest {
        // System function - no institution ID required
        // Security context should be SYSTEM
        public DailyTaskRequest() {
            setSecurityContext(SecurityContext.system());
        }
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class DailyTaskResponse extends FunctionResponse {
        private int processedInstitutions;
        private int successCount;
        private int errorCount;
    }
}


