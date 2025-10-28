package de.bbajor.pvs.ai.service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.bbajor.pvs.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VoiceTranscriptionService {

    private static final Logger LOG = LogManager.getLogger(VoiceTranscriptionService.class);
    private final AiProperties aiProperties;
    private final WhisperInstallationService whisperInstallationService;
    private final AiUsageService aiUsageService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TranscriptionResult transcribe(byte[] audioData, String contentType) {
        // Try local whisper first
        if (aiProperties.getWhisper().getLocal().isEnabled()) {
            try {
                if (!whisperInstallationService.checkWhisperServerAvailable()) {
                    LOG.warn("Local Whisper server not available");
                    if (aiProperties.getWhisper().getLocal().isAutoInstall()) {
                        try {
                            whisperInstallationService.installWhisper();
                            whisperInstallationService.startWhisperServer();
                            // Wait a bit for server to be ready
                            Thread.sleep(5000);
                        } catch (Exception e) {
                            LOG.error("Failed to auto-install Whisper", e);
                            return tryRemoteTranscription(audioData);
                        }
                    } else {
                        return tryRemoteTranscription(audioData);
                    }
                }

                LOG.info("Calling transcribeLocal with {} bytes of audio data", audioData.length);
                String localResult = transcribeLocal(audioData);
                if (localResult != null) {
                    LOG.info("Local transcription successful, result length: {}", localResult.length());
                    aiUsageService.logUsage("local-whisper", "transcription", null, true, null);
                    return new TranscriptionResult(localResult, "local-whisper");
                } else {
                    LOG.warn("Local transcription returned null result");
                }
            } catch (Exception e) {
                LOG.error("Local transcription failed", e);
                aiUsageService.logUsage("local-whisper", "transcription", null, false, e.getMessage());
            }
        }

        // Fallback to remote
        return tryRemoteTranscription(audioData);
    }

    private String transcribeLocal(byte[] audioData) {
        try {
            String url = "http://" + aiProperties.getWhisper().getLocal().getHost() + ":"
                    + aiProperties.getWhisper().getLocal().getPort() + "/transcribe";

            LOG.info("Sending transcription request to: {}, audio size: {} bytes", url, audioData.length);

            // Create proper multipart/form-data request
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            
            // Build multipart body
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("--").append(boundary).append("\r\n");
            bodyBuilder.append("Content-Disposition: form-data; name=\"audio\"; filename=\"audio.webm\"\r\n");
            bodyBuilder.append("Content-Type: audio/webm\r\n\r\n");
            
            byte[] headerBytes = bodyBuilder.toString().getBytes(StandardCharsets.UTF_8);
            String footer = "\r\n--" + boundary + "--\r\n";
            byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
            
            // Create request body as InputStream to avoid large byte array concatenation
            byte[] requestBody = new byte[headerBytes.length + audioData.length + footerBytes.length];
            System.arraycopy(headerBytes, 0, requestBody, 0, headerBytes.length);
            System.arraycopy(audioData, 0, requestBody, headerBytes.length, audioData.length);
            System.arraycopy(footerBytes, 0, requestBody, headerBytes.length + audioData.length, footerBytes.length);

            LOG.debug("Request body size: {} bytes (header: {}, audio: {}, footer: {})", 
                    requestBody.length, headerBytes.length, audioData.length, footerBytes.length);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(Duration.ofSeconds(120)) // Increased timeout for model loading and transcription
                    .POST(BodyPublishers.ofByteArray(requestBody))
                    .build();

            LOG.info("Executing HTTP request to Whisper container at {}...", url);
            LOG.debug("Request details - Method: POST, Content-Type: multipart/form-data; boundary={}, Body size: {} bytes", 
                    boundary, requestBody.length);
            LOG.debug("First 200 bytes of request: {}", 
                    new String(requestBody, 0, Math.min(200, requestBody.length), StandardCharsets.UTF_8));
            
            HttpResponse<String> response;
            long requestStartTime = System.currentTimeMillis();
            try {
                LOG.info("HTTP send() called, waiting for response...");
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                long elapsed = System.currentTimeMillis() - requestStartTime;
                LOG.info("Whisper container responded after {} ms with status: {}, body length: {}", 
                        elapsed, response.statusCode(), 
                        response.body() != null ? response.body().length() : 0);
            } catch (Exception httpError) {
                long elapsed = System.currentTimeMillis() - requestStartTime;
                LOG.error("HTTP request to Whisper container failed after {} ms", elapsed, httpError);
                throw httpError;
            }
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                LOG.debug("Response body: {}", responseBody);
                JsonNode json = objectMapper.readTree(responseBody);
                
                if (json.has("error")) {
                    String error = json.get("error").asText();
                    LOG.error("Whisper container returned error: {}", error);
                    return null;
                }
                
                String text = json.has("text") ? json.get("text").asText() : null;
                LOG.info("Transcription successful, text length: {}", text != null ? text.length() : 0);
                return text;
            } else {
                LOG.error("Local Whisper API returned status: {}, body: {}", response.statusCode(), response.body());
                return null;
            }
        } catch (java.net.http.HttpTimeoutException e) {
            LOG.error("Timeout calling local Whisper API after 120 seconds", e);
            return null;
        } catch (java.net.ConnectException e) {
            LOG.error("Could not connect to Whisper container at {}:{}", 
                    aiProperties.getWhisper().getLocal().getHost(),
                    aiProperties.getWhisper().getLocal().getPort(), e);
            return null;
        } catch (Exception e) {
            LOG.error("Error calling local Whisper API", e);
            return null;
        }
    }

    private TranscriptionResult tryRemoteTranscription(byte[] unused) {
        if (!aiProperties.getWhisper().getRemote().isEnabled()) {
            throw new IllegalStateException("Both local and remote transcription are unavailable");
        }

        // Check quota
        long usage = aiUsageService.getUsageCountForCurrentMonth("aleph-alpha");
        if (usage >= aiProperties.getWhisper().getRemote().getMonthlyQuota()) {
            throw new IllegalStateException("Monthly quota exceeded for remote transcription");
        }

        // TODO: Implement remote API call to Aleph Alpha or other DSGVO-compliant provider
        // For now, throw exception
        throw new UnsupportedOperationException("Remote transcription not yet implemented");
    }

    public static class TranscriptionResult {
        private final String text;
        private final String provider;

        public TranscriptionResult(String text, String provider) {
            this.text = text;
            this.provider = provider;
        }

        public String getText() {
            return text;
        }

        public String getProvider() {
            return provider;
        }
    }

}

