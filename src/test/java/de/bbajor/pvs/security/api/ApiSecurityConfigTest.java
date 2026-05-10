package de.bbajor.pvs.security.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.SharedHttpSessionConfigurer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import de.bbajor.pvs.ai.controller.ExtractionController;
import de.bbajor.pvs.ai.extraction.ExtractionOrchestrator;
import de.bbajor.pvs.institution.service.CurrentInstitutionService;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        ApiSecurityConfig.class,
        ApiSecurityConfigTest.TestConfig.class
})
@TestPropertySource(properties = "app.security.cors-allowed-origins=http://localhost:3000")
class ApiSecurityConfigTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ExtractionOrchestrator extractionOrchestrator;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .apply(SharedHttpSessionConfigurer.sharedHttpSession())
                .build();
    }

    @Test
    void anonymousAiRequestsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/ai/extraction/patient")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"test\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(extractionOrchestrator);
    }

    @Configuration
    @EnableWebMvc
    static class TestConfig {

        @Bean
        ExtractionController extractionController(ExtractionOrchestrator extractionOrchestrator) {
            return new ExtractionController(extractionOrchestrator);
        }

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
