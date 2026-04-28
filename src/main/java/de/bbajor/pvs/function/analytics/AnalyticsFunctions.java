package de.bbajor.pvs.function.analytics;

import de.bbajor.pvs.analytics.dto.AnalyticsData;
import de.bbajor.pvs.analytics.service.AnalyticsService;
import de.bbajor.pvs.common.function.FunctionRequest;
import de.bbajor.pvs.common.function.FunctionResponse;
import de.bbajor.pvs.function.core.FunctionWrapper;
import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Spring Cloud Functions for Analytics Service.
 */
@Configuration
@RequiredArgsConstructor
public class AnalyticsFunctions {
    
    private final AnalyticsService analyticsService;
    private final CurrentInstitutionService currentInstitutionService;
    
    @Bean
    public Function<GetStatisticsRequest, GetStatisticsResponse> getStatistics() {
        return FunctionWrapper.wrap(currentInstitutionService,
            request -> {
                Long institutionId = request.getInstitutionId();
                if (institutionId == null) {
                    GetStatisticsResponse response = new GetStatisticsResponse();
                    response.setErrorMessage("Institution ID is required");
                    response.setSuccess(false);
                    return response;
                }
                
                // Use getAllAnalyticsData which requires institutionId from context
                // The FunctionWrapper sets InstitutionContext from request
                AnalyticsData data = analyticsService.getAllAnalyticsData();
                
                GetStatisticsResponse response = new GetStatisticsResponse();
                response.setAnalyticsData(data);
                return response;
            },
            "getStatistics"
        );
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GetStatisticsRequest extends FunctionRequest {
        // No additional fields needed
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GetStatisticsResponse extends FunctionResponse {
        private AnalyticsData analyticsData;
    }
}


