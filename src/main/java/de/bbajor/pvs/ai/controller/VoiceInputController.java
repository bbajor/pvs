package de.bbajor.pvs.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.bbajor.pvs.ai.service.VoiceTranscriptionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/voice")
@RequiredArgsConstructor
public class VoiceInputController {

    private final VoiceTranscriptionService transcriptionService;

    @PostMapping("/transcribe")
    public ResponseEntity<TranscriptionResponse> transcribe(@RequestParam("audio") MultipartFile audioFile) {
        org.apache.logging.log4j.LogManager.getLogger(VoiceInputController.class)
                .info("Received transcription request, file size: {} bytes, content type: {}", 
                        audioFile.getSize(), audioFile.getContentType());
        try {
            byte[] audioData = audioFile.getBytes();
            org.apache.logging.log4j.LogManager.getLogger(VoiceInputController.class)
                    .info("Calling transcription service with {} bytes", audioData.length);
            
            VoiceTranscriptionService.TranscriptionResult result = transcriptionService
                    .transcribe(audioData, audioFile.getContentType());
            
            org.apache.logging.log4j.LogManager.getLogger(VoiceInputController.class)
                    .info("Transcription completed, provider: {}, text length: {}", 
                            result.getProvider(), result.getText() != null ? result.getText().length() : 0);
            
            return ResponseEntity.ok(new TranscriptionResponse(result.getText(), result.getProvider(), null));
        } catch (Exception e) {
            org.apache.logging.log4j.LogManager.getLogger(VoiceInputController.class)
                    .error("Transcription failed", e);
            return ResponseEntity.ok(new TranscriptionResponse(null, null, e.getMessage()));
        }
    }

    public static class TranscriptionResponse {
        private final String text;
        private final String provider;
        private final String error;

        public TranscriptionResponse(String text, String provider, String error) {
            this.text = text;
            this.provider = provider;
            this.error = error;
        }

        public String getText() {
            return text;
        }

        public String getProvider() {
            return provider;
        }

        public String getError() {
            return error;
        }
    }

}

