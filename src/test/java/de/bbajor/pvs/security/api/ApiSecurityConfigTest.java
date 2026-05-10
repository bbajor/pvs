package de.bbajor.pvs.security.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import de.bbajor.pvs.ai.controller.ExtractionController;
import de.bbajor.pvs.ai.extraction.ExtractionOrchestrator;
import de.bbajor.pvs.institution.service.CurrentInstitutionService;

@WebMvcTest(controllers = ExtractionController.class)
@Import(ApiSecurityConfig.class)
@TestPropertySource(properties = "app.security.cors-allowed-origins=http://localhost:3000")
class ApiSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExtractionOrchestrator extractionOrchestrator;

    @MockBean
    private CurrentInstitutionService currentInstitutionService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void anonymousAiRequestsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/ai/extraction/patient")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"test\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(extractionOrchestrator);
    }
}
