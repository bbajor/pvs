package de.bbajor.pvs.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.bbajor.pvs.ai.extraction.ExtractionOrchestrator;
import de.bbajor.pvs.ai.extraction.ExtractionResult;
import de.bbajor.pvs.patient.model.Patient;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/extraction")
@RequiredArgsConstructor
public class ExtractionController {

    private final ExtractionOrchestrator extractionOrchestrator;

    @PostMapping("/patient")
    public ResponseEntity<ExtractionResponse> extractPatient(@RequestBody ExtractionRequest request) {
        try {
            ExtractionResult<Patient> result = extractionOrchestrator.extract(request.getText(), Patient.class);
            return ResponseEntity.ok(new ExtractionResponse(result, null));
        } catch (Exception e) {
            return ResponseEntity.ok(new ExtractionResponse(null, e.getMessage()));
        }
    }

    public static class ExtractionRequest {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class ExtractionResponse {
        private final ExtractionResult<Patient> result;
        private final String error;

        public ExtractionResponse(ExtractionResult<Patient> result, String error) {
            this.result = result;
            this.error = error;
        }

        public ExtractionResult<Patient> getResult() {
            return result;
        }

        public String getError() {
            return error;
        }
    }

}

