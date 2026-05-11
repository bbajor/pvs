package de.bbajor.pvs.security.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayMetricsAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayNoLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayReactiveLoadBalancerClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import de.bbajor.pvs.ai.controller.ExtractionController;
import de.bbajor.pvs.ai.extraction.ExtractionOrchestrator;
import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.patient.model.Patient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = {
                ApiSecurityConfigTest.TestApplication.class,
                ExtractionController.class,
                ApiSecurityConfig.class,
                ApiSecurityConfigTest.SecurityTestConfig.class
        })
@AutoConfigureMockMvc(addFilters = true)
class ApiSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExtractionOrchestrator extractionOrchestrator;

    @Autowired
    private CurrentInstitutionService currentInstitutionService;

    @Test
    void aiEndpointsRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(post("/api/ai/extraction/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"patient notes\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(extractionOrchestrator);
    }

    @Test
    void aiEndpointsAllowAuthenticatedRequestsWithInstitution() throws Exception {
        when(currentInstitutionService.hasInstitution()).thenReturn(true);

        mockMvc.perform(post("/api/ai/extraction/patient")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"patient notes\"}"))
                .andExpect(status().isOk());

        verify(extractionOrchestrator).extract("patient notes", Patient.class);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            GatewayAutoConfiguration.class,
            GatewayClassPathWarningAutoConfiguration.class,
            GatewayMetricsAutoConfiguration.class,
            GatewayReactiveLoadBalancerClientAutoConfiguration.class,
            GatewayNoLoadBalancerClientAutoConfiguration.class
    })
    @EnableWebSecurity
    static class TestApplication {
    }

    @TestConfiguration
    static class SecurityTestConfig {

        @Bean
        ExtractionOrchestrator extractionOrchestrator() {
            return mock(ExtractionOrchestrator.class);
        }

        @Bean
        CurrentInstitutionService currentInstitutionService() {
            return mock(CurrentInstitutionService.class);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }
}
