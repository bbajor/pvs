package de.bbajor.pvs.ai.ui;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadHandler;

import de.bbajor.pvs.ai.service.VoiceTranscriptionService;

/**
 * Dialog für Spracheingabe mit MediaRecorder API und Whisper-Transkription.
 * Funktioniert browserübergreifend (Chrome, Firefox, Edge, Safari).
 */
public class VoiceInputDialog extends Dialog {

    private static final Logger LOG = LogManager.getLogger(VoiceInputDialog.class);
    private static final int MAX_RECORDING_DURATION_SECONDS = 60;

    private final TextArea transcriptionArea;
    private final Button startButton;
    private final Button stopButton;
    private final Button extractButton;
    private final ProgressBar progressBar;
    private final Div statusLabel;
    private final Span timerLabel;
    private final Div hintLabel;
    private final Upload hiddenUpload;
    private final VoiceTranscriptionService transcriptionService;
    private final UI currentUI;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private boolean isRecording = false;
    private String transcribedText = "";
    private OnExtractionRequestedListener extractionListener;
    private ScheduledFuture<?> timerTask;
    private int remainingSeconds = MAX_RECORDING_DURATION_SECONDS;

    public VoiceInputDialog(VoiceTranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
        this.currentUI = UI.getCurrent();
        setWidth("600px");
        setHeight("500px");
        setHeaderTitle("Spracheingabe");

        transcriptionArea = new TextArea("Transkription");
        transcriptionArea.setWidthFull();
        transcriptionArea.setHeight("200px");
        transcriptionArea.setReadOnly(true);

        startButton = new Button("Aufnahme starten");
        startButton.setIcon(VaadinIcon.MICROPHONE.create());
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startButton.addClickListener(e -> startRecording());

        stopButton = new Button("Aufnahme beenden");
        stopButton.setIcon(VaadinIcon.STOP.create());
        stopButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        stopButton.setEnabled(false);
        stopButton.addClickListener(e -> stopRecording());

        extractButton = new Button("Patientendaten übernehmen");
        extractButton.setIcon(VaadinIcon.MAGIC.create());
        extractButton.setEnabled(false);
        extractButton.addClickListener(e -> {
            if (extractionListener != null && !transcribedText.isEmpty()) {
                extractionListener.onExtractionRequested(transcribedText);
            }
        });
        
        transcriptionArea.addValueChangeListener(e -> {
            String text = e.getValue();
            extractButton.setEnabled(text != null && !text.trim().isEmpty());
            transcribedText = text != null ? text : "";
        });

        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setIndeterminate(true);

        statusLabel = new Div();
        statusLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");
        statusLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");

        timerLabel = new Span();
        timerLabel.setVisible(false);
        timerLabel.getStyle().set("font-size", "var(--lumo-font-size-m)");
        timerLabel.getStyle().set("font-weight", "bold");
        timerLabel.getStyle().set("color", "var(--lumo-error-color)");

        hintLabel = new Div();
        hintLabel.setText("Hinweis: Die maximale Aufnahmedauer beträgt " + MAX_RECORDING_DURATION_SECONDS + " Sekunden. " +
                "Funktioniert in allen modernen Browsern (Chrome, Firefox, Edge, Safari).");
        hintLabel.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        hintLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
        hintLabel.getStyle().set("font-style", "italic");

        Button cancelButton = new Button("Abbrechen", e -> close());

        hiddenUpload = createHiddenUpload();

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.add(startButton, stopButton, timerLabel);
        buttonLayout.setFlexGrow(1, startButton);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        content.setWidthFull();
        content.add(transcriptionArea, hintLabel, buttonLayout, progressBar, statusLabel, hiddenUpload);
        add(content);

        getFooter().add(cancelButton, extractButton);

        initializeMediaRecorder();
    }

    private Upload createHiddenUpload() {
        UploadHandler uploadHandler = UploadHandler.inMemory((metadata, bytes) -> {
            LOG.info("=== UPLOAD HANDLER CALLED ===");
            LOG.info("Received audio upload, size: {} bytes", bytes.length);
            LOG.info("Metadata: {}", metadata);
            
            if (bytes.length == 0) {
                LOG.error("Received empty audio data");
                if (currentUI != null) {
                    currentUI.access(() -> handleError("Aufnahme ist leer. Bitte erneut versuchen."));
                }
                return;
            }
            
            if (currentUI != null) {
                currentUI.access(() -> updateStatus("Audio wird an Whisper-Container gesendet...", false));
            }
            
            try {
                LOG.info("Calling transcription service with {} bytes", bytes.length);
                
                // Check if transcription service is available
                if (transcriptionService == null) {
                    LOG.error("Transcription service is null");
                    if (currentUI != null) {
                        currentUI.access(() -> handleError("Transkriptionsservice nicht verfügbar"));
                    }
                    return;
                }
                
                VoiceTranscriptionService.TranscriptionResult result = transcriptionService
                        .transcribe(bytes, "audio/webm");
                
                LOG.info("Transcription result: {}", result != null ? 
                        (result.getText() != null ? "Text length: " + result.getText().length() + ", provider: " + result.getProvider() : "null text") : 
                        "null result");
                
                if (currentUI != null) {
                    currentUI.access(() -> {
                        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
                            setTranscriptionResult(result.getText());
                        } else {
                            String errorMsg = result != null && result.getText() == null ? 
                                    "Whisper-Container konnte keine Transkription erstellen" : 
                                    "Keine Transkription erhalten";
                            LOG.warn(errorMsg);
                            handleError(errorMsg);
                        }
                    });
                } else if (VaadinSession.getCurrent() != null) {
                    VaadinSession.getCurrent().getUIs().forEach(ui -> ui.access(() -> {
                        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
                            setTranscriptionResult(result.getText());
                        } else {
                            handleError("Keine Transkription erhalten");
                        }
                    }));
                }
            } catch (Exception e) {
                LOG.error("Error processing audio upload", e);
                final String errorMsg = "Transkriptionsfehler: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                if (currentUI != null) {
                    currentUI.access(() -> handleError(errorMsg));
                }
            }
        });

        Upload upload = new Upload(uploadHandler);
        upload.setMaxFiles(1);
        upload.setMaxFileSize(50 * 1024 * 1024);
        upload.setAutoUpload(true); // Enable auto-upload
        upload.getElement()
                .addEventListener("file-reject", event -> {
                    String errorMessage = event.getEventData().getString("event.detail.error");
                    String message = (errorMessage == null || errorMessage.isBlank())
                            ? "Unbekannter Fehler"
                            : errorMessage;
                    LOG.error("File upload rejected: {}", message);
                    handleError("Upload abgelehnt: " + message);
                })
                .addEventData("event.detail.error");
        
        upload.getElement().getStyle().set("display", "none");
        upload.setId("voice-recording-upload");
        
        return upload;
    }

    private void initializeMediaRecorder() {
        // Simplified JavaScript - store element reference and recreate recorder each time
        getElement().executeJs(
                "if (!window.pvsVR) {" +
                "  window.pvsVR = { stream: null, elem: null, currentRec: null };" +
                "}" +
                "window.pvsVR.elem = $0;" +
                "  " +
                "window.pvsVR.init = function() {" +
                "  return navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {" +
                "    if (window.pvsVR.stream) window.pvsVR.stream.getTracks().forEach(t => t.stop());" +
                "    window.pvsVR.stream = stream;" +
                "  });" +
                "};" +
                "  " +
                "window.pvsVR.start = function() {" +
                "  if (!window.pvsVR.stream) return false;" +
                "  if (window.pvsVR.currentRec && window.pvsVR.currentRec.state !== 'inactive') return false;" +
                "  const chunks = [];" +
                "  const elem = window.pvsVR.elem;" +
                "  const rec = new MediaRecorder(window.pvsVR.stream, { mimeType: 'audio/webm' });" +
                "  rec.ondataavailable = e => { if (e.data && e.data.size > 0) chunks.push(e.data); };" +
                "  rec.onstop = function() {" +
                "    if (chunks.length === 0) return;" +
                "    const blob = new Blob(chunks, { type: 'audio/webm' });" +
                "    const reader = new FileReader();" +
                "    reader.onloadend = function() {" +
                "      const base64 = reader.result.split(',')[1];" +
                "      if (elem && elem.$server) elem.$server.processAudioData(base64);" +
                "    };" +
                "    reader.readAsDataURL(blob);" +
                "  };" +
                "  window.pvsVR.currentRec = rec;" +
                "  rec.start(1000);" +
                "  return true;" +
                "};" +
                "  " +
                "window.pvsVR.stop = function() {" +
                "  if (!window.pvsVR.currentRec || window.pvsVR.currentRec.state !== 'recording') return false;" +
                "  window.pvsVR.currentRec.stop();" +
                "  return true;" +
                "};"
        );
    }

    private void startRecording() {
        if (isRecording) {
            LOG.warn("Already recording, ignoring start request");
            return;
        }
        
        LOG.info("Starting recording...");
        updateStatus("Starte Aufnahme...", false);
        
        getElement().executeJs(
                "if (!window.pvsVR || !window.pvsVR.stream) {" +
                "  return window.pvsVR.init().then(() => window.pvsVR.start()).catch(() => false);" +
                "} else {" +
                "  return Promise.resolve(window.pvsVR.start());" +
                "}")
                .then(Boolean.class, started -> {
                    if (started != null && started) {
                        isRecording = true;
                        startButton.setEnabled(false);
                        stopButton.setEnabled(true);
                        updateStatus("Aufnahme läuft...", false);
                        startRecordingTimer();
                    } else {
                        LOG.warn("Recording could not be started");
                        updateStatus("Aufnahme konnte nicht gestartet werden", true);
                        Notification.show(
                                "Mikrofon konnte nicht gestartet werden. Bitte Berechtigung erteilen.",
                                5000,
                                Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
    }

    private void stopRecording() {
        LOG.info("Stop recording called, isRecording: {}", isRecording);
        
        stopRecordingTimer();
        
        // Update UI immediately to prevent double-clicks
        isRecording = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        updateStatus("Stoppe Aufnahme...", false);
        
        getElement().executeJs("return window.pvsVR ? window.pvsVR.stop() : false;")
                .then(Boolean.class, stopped -> {
                    try {
                        LOG.info("Stop result: {}", stopped);
                        if (stopped != null && stopped) {
                            updateStatus("Verarbeite Aufnahme...", false);
                            progressBar.setVisible(true);
                        } else {
                            // If stop failed, try to reset state
                            LOG.warn("Stop command returned false, resetting UI state");
                            updateStatus("Aufnahme beendet", false);
                            progressBar.setVisible(false);
                        }
                    } catch (Exception e) {
                        LOG.error("Exception in stop recording callback", e);
                        updateStatus("Fehler beim Stoppen der Aufnahme", true);
                        progressBar.setVisible(false);
                    }
                }, errorMessage -> {
                    LOG.error("JavaScript error in stop recording: {}", errorMessage);
                    updateStatus("Fehler beim Stoppen der Aufnahme", true);
                    progressBar.setVisible(false);
                });
    }

    private void startRecordingTimer() {
        remainingSeconds = MAX_RECORDING_DURATION_SECONDS;
        timerLabel.setText(String.format("(%ds)", remainingSeconds));
        timerLabel.setVisible(true);
        
        if (timerTask != null) {
            timerTask.cancel(false);
        }
        
        timerTask = scheduler.scheduleAtFixedRate(() -> {
            remainingSeconds--;
            if (remainingSeconds > 0) {
                currentUI.access(() -> {
                    timerLabel.setText(String.format("(%ds)", remainingSeconds));
                });
            } else {
                currentUI.access(() -> {
                    timerLabel.setVisible(false);
                    if (isRecording) {
                        LOG.info("Recording time limit reached");
                        stopRecording();
                    }
                });
                timerTask.cancel(false);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void stopRecordingTimer() {
        if (timerTask != null) {
            timerTask.cancel(false);
            timerTask = null;
        }
        timerLabel.setVisible(false);
        remainingSeconds = MAX_RECORDING_DURATION_SECONDS;
    }

    @com.vaadin.flow.component.ClientCallable
    private void handleRecordingError(String errorMessage) {
        LOG.error("Recording error from JavaScript: {}", errorMessage);
        handleError(errorMessage);
    }

    @com.vaadin.flow.component.ClientCallable
    private void processAudioData(String base64AudioData) {
        LOG.info("Received audio data from JavaScript, length: {} characters", base64AudioData.length());
        
        try {
            // Decode base64 to byte array
            byte[] audioBytes = java.util.Base64.getDecoder().decode(base64AudioData);
            LOG.info("Decoded audio data to {} bytes", audioBytes.length);
            
            if (audioBytes.length == 0) {
                handleError("Aufnahme ist leer. Bitte erneut versuchen.");
                return;
            }
            
            updateStatus("Audio wird an Whisper-Container gesendet...", false);
            progressBar.setVisible(true);
            
            // Call transcription service
            if (transcriptionService == null) {
                LOG.error("Transcription service is null");
                handleError("Transkriptionsservice nicht verfügbar");
                return;
            }
            
            VoiceTranscriptionService.TranscriptionResult result = transcriptionService
                    .transcribe(audioBytes, "audio/webm");
            
            LOG.info("Transcription result: {}", result != null ? 
                    (result.getText() != null ? "Text length: " + result.getText().length() + ", provider: " + result.getProvider() : "null text") : 
                    "null result");
            
            if (result != null && result.getText() != null && !result.getText().isEmpty()) {
                setTranscriptionResult(result.getText());
            } else {
                String errorMsg = result != null && result.getText() == null ? 
                        "Whisper-Container konnte keine Transkription erstellen" : 
                        "Keine Transkription erhalten";
                LOG.warn(errorMsg);
                handleError(errorMsg);
            }
        } catch (Exception e) {
            LOG.error("Error processing audio data", e);
            handleError("Transkriptionsfehler: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    private void handleError(String error) {
        LOG.error("Handling error: {}", error);
        Notification.show("Fehler: " + error, 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        progressBar.setVisible(false);
        updateStatus("Fehler: " + error, true);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        extractButton.setEnabled(false);
    }

    private void setTranscriptionResult(String text) {
        LOG.info("Setting transcription result");
        transcribedText = text != null ? text : "";
        transcriptionArea.setValue(transcribedText);
        transcriptionArea.setReadOnly(false);
        progressBar.setVisible(false);
        
        boolean hasValidText = text != null && !text.trim().isEmpty();
        extractButton.setEnabled(hasValidText);
        
        if (hasValidText) {
            updateStatus("Transkription abgeschlossen", false);
            statusLabel.getStyle().set("color", "var(--lumo-success-color)");
        } else {
            updateStatus("Keine Transkription verfügbar", true);
        }
        
        startButton.setEnabled(true);
    }

    private void updateStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.getStyle().set("color", isError ? 
                "var(--lumo-error-color)" : "var(--lumo-secondary-text-color)");
    }

    public void setOnExtractionRequestedListener(OnExtractionRequestedListener listener) {
        this.extractionListener = listener;
    }

    @Override
    public void close() {
        stopRecordingTimer();
        getElement().executeJs(
                "if (window.pvsVR && window.pvsVR.stream) {" +
                "  window.pvsVR.stream.getTracks().forEach(track => track.stop());" +
                "}"
        );
        scheduler.shutdown();
        super.close();
    }

    @FunctionalInterface
    public interface OnExtractionRequestedListener {
        void onExtractionRequested(String transcribedText);
    }
}
