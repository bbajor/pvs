package de.bbajor.pvs.function.ai;

import de.bbajor.pvs.ai.extraction.ExtractionOrchestrator;
import de.bbajor.pvs.ai.extraction.ExtractionResult;
import de.bbajor.pvs.ai.service.VoiceTranscriptionService;
import de.bbajor.pvs.common.function.FunctionRequest;
import de.bbajor.pvs.common.function.FunctionResponse;
import de.bbajor.pvs.function.core.FunctionWrapper;
import de.bbajor.pvs.patient.model.Patient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Spring Cloud Functions for AI Service (Voice Transcription and Data Extraction).
 */
@Configuration
@RequiredArgsConstructor
public class AiFunctions {
    
    private final VoiceTranscriptionService transcriptionService;
    private final ExtractionOrchestrator extractionOrchestrator;
    
    @Bean
    public Function<TranscribeVoiceRequest, TranscribeVoiceResponse> transcribeVoice() {
        return FunctionWrapper.wrap(
            request -> {
                VoiceTranscriptionService.TranscriptionResult result = 
                        transcriptionService.transcribe(request.getAudioData(), request.getContentType());
                
                TranscribeVoiceResponse response = new TranscribeVoiceResponse();
                response.setText(result.getText());
                response.setProvider(result.getProvider());
                return response;
            },
            "transcribeVoice"
        );
    }
    
    @Bean
    public Function<ExtractPatientDataRequest, ExtractPatientDataResponse> extractPatientData() {
        return FunctionWrapper.wrap(
            request -> {
                ExtractionResult<Patient> result = extractionOrchestrator.extract(
                        request.getText(), Patient.class);
                
                ExtractPatientDataResponse response = new ExtractPatientDataResponse();
                response.setPatient(result.getEntity());
                response.setConfidence(result.getConfidence());
                response.setFieldConfidences(result.getFieldConfidences());
                return response;
            },
            "extractPatientData"
        );
    }
    
    // Request/Response classes
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TranscribeVoiceRequest extends FunctionRequest {
        private byte[] audioData;
        private String contentType;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TranscribeVoiceResponse extends FunctionResponse {
        private String text;
        private String provider;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ExtractPatientDataRequest extends FunctionRequest {
        private String text;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ExtractPatientDataResponse extends FunctionResponse {
        private Patient patient;
        private Double confidence;
        private java.util.Map<String, Double> fieldConfidences;
    }
}


