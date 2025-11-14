package de.bbajor.pvs.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud Gateway Configuration for routing to serverless functions.
 * 
 * Routes HTTP requests to appropriate functions based on path patterns.
 * This acts as the API Gateway for the serverless microservices architecture.
 * 
 * NOTE: Gateway is incompatible with Spring MVC (Vaadin Flow).
 * This configuration is only active when:
 * - spring.main.web-application-type=reactive is set, OR
 * - gateway.enabled=true property is set
 * 
 * For now, Gateway is disabled by default. It will be used when:
 * - UI is migrated to Hilla (React), OR
 * - Gateway runs as separate service
 */
@Configuration
@ConditionalOnProperty(name = "gateway.enabled", havingValue = "true", matchIfMissing = false)
public class GatewayConfig {
    
    /**
     * Configure routes to serverless functions.
     * 
     * Routes are configured to forward requests to OpenFaaS or other FaaS platforms.
     * In development, functions can run locally via Spring Cloud Function Web adapter.
     */
    @Bean
    public RouteLocator functionRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            // Patient Service Functions
            .route("patient-create", r -> r.path("/api/functions/patient/create")
                .uri("http://localhost:31112/function/patient-service-createPatient"))
            .route("patient-update", r -> r.path("/api/functions/patient/update")
                .uri("http://localhost:31112/function/patient-service-updatePatient"))
            .route("patient-find", r -> r.path("/api/functions/patient/find")
                .uri("http://localhost:31112/function/patient-service-findPatient"))
            .route("patient-search", r -> r.path("/api/functions/patient/search")
                .uri("http://localhost:31112/function/patient-service-searchPatients"))
            .route("patient-list", r -> r.path("/api/functions/patient/list")
                .uri("http://localhost:31112/function/patient-service-getAllPatients"))
            
            // Institution Service Functions
            .route("institution-create", r -> r.path("/api/functions/institution/create")
                .uri("http://localhost:31112/function/institution-service-createInstitution"))
            .route("institution-get", r -> r.path("/api/functions/institution/get")
                .uri("http://localhost:31112/function/institution-service-getInstitution"))
            .route("institution-list", r -> r.path("/api/functions/institution/list")
                .uri("http://localhost:31112/function/institution-service-listInstitutions"))
            
            // AI Service Functions
            .route("ai-transcribe", r -> r.path("/api/functions/ai/transcribe")
                .uri("http://localhost:31112/function/ai-service-transcribeVoice"))
            .route("ai-extract", r -> r.path("/api/functions/ai/extract")
                .uri("http://localhost:31112/function/ai-service-extractPatientData"))
            
            // eGK Service Functions
            .route("egk-process", r -> r.path("/api/functions/egk/process")
                .uri("http://localhost:31112/function/egk-service-processEgkData"))
            
            // Treatment Service Functions
            .route("treatment-approve", r -> r.path("/api/functions/treatment/approve")
                .uri("http://localhost:31112/function/treatment-service-approveTreatment"))
            
            // Task Service Functions
            .route("task-complete", r -> r.path("/api/functions/task/complete")
                .uri("http://localhost:31112/function/task-service-completeTask"))
            
            // Appointment Service Functions
            .route("appointment-schedule", r -> r.path("/api/functions/appointment/schedule")
                .uri("http://localhost:31112/function/appointment-service-scheduleAppointment"))
            .route("appointment-cancel", r -> r.path("/api/functions/appointment/cancel")
                .uri("http://localhost:31112/function/appointment-service-cancelAppointment"))
            .route("appointment-list", r -> r.path("/api/functions/appointment/list")
                .uri("http://localhost:31112/function/appointment-service-getAppointments"))
            .route("appointment-find-slot", r -> r.path("/api/functions/appointment/find-slot")
                .uri("http://localhost:31112/function/appointment-service-findNextAvailableSlot"))
            
            // Analytics Service Functions
            .route("analytics-statistics", r -> r.path("/api/functions/analytics/statistics")
                .uri("http://localhost:31112/function/analytics-service-getStatistics"))
            
            // Scheduled Task Function (internal, not exposed via HTTP)
            // This is triggered by cron scheduler, not HTTP requests
            
            .build();
    }
}

