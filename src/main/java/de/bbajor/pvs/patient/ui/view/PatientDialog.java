package de.bbajor.pvs.patient.ui.view;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.binder.ValidationException;

import de.bbajor.pvs.ai.extraction.ExtractionResult;
import de.bbajor.pvs.ai.service.ExtractionClient;
import de.bbajor.pvs.ai.service.VoiceTranscriptionService;
import de.bbajor.pvs.ai.ui.EntityVerificationDialog;
import de.bbajor.pvs.ai.ui.VoiceInputDialog;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.presenter.PatientPresenter;
import lombok.RequiredArgsConstructor;

public class PatientDialog extends Dialog {

    private List<PatientChangeListener> listeners = new ArrayList<>();
    private final Button saveButton = new Button();

    private final PatientPresenter presenter;
    private final PatientForm form;
    private final ExtractionClient extractionClient;
    private final VoiceTranscriptionService transcriptionService;

    public PatientDialog(PatientPresenter presenter, Patient patient) {
        this(presenter, patient, null, null);
    }

    public PatientDialog(PatientPresenter presenter, Patient patient, ExtractionClient extractionClient, VoiceTranscriptionService transcriptionService) {
        this.extractionClient = extractionClient;
        this.transcriptionService = transcriptionService;
        this.presenter = presenter;
        if (patient == null) {
            patient = new Patient();
        }

        setWidth("1400px");
        setHeight("1000px");

        // Create the components
        form = new PatientForm(presenter.getHealthInsurances(), patient, e -> valueChanged(e));

        var readBtn = new Button("Aus Gesundheitskarte einlesen", event -> {
            try {
                form.setValue(presenter.readDataFromEgk());
            } catch (Exception e) {
                Notification.show("Einlesen der Karte nicht erfolgreich: " + e.getMessage());
            }
        });
        readBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        
        var voiceInputBtn = new Button("Spracheingabe", event -> openVoiceInputDialog());
        voiceInputBtn.setIcon(VaadinIcon.VOLUME_UP.create());
        voiceInputBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        
        var saveLbl = patient == null || patient.getId() == null ? "Erstellen"
                : "Aktualisieren";
        saveButton.setText(saveLbl);
        saveButton.setEnabled(form.isValidateOk());
        saveButton.addClickListener(event -> {
            try {
                save();
            } catch (ValidationException e) {
                Notification.show("Patientendaten konnten nicht gespeichert werden:" + e.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var cancelBtn = new Button("Abbrechen", event -> close());

        // Configure the dialog
        String title = patient == null || patient.getId() == null ? "Neuer Patient"
                : "Patient " + patient.toString();
        setHeaderTitle(title);
        add(form);
        getFooter().add(cancelBtn, readBtn, voiceInputBtn, saveButton);
    }

    public void addChangeListener(PatientChangeListener listener) {
        listeners.add(listener);
    }

    protected void notifyListeners() {
        listeners.forEach(e -> e.onPatientChanged(form.getValue()));
    }

    private void save() throws ValidationException {
        if (form.isValidateOk()) {
            form.writeIfValid();
            presenter.savePatient(form.getValue());
            notifyListeners();
            close();
        } else {
            Notification.show("Es fehlen noch Angaben. Bitte ergänzen.");
        }
    }

    public void valueChanged(ValueChangeEvent<?> event) {
        saveButton.setEnabled(form.isValidateOk());
    }

    private void openVoiceInputDialog() {
        if (transcriptionService == null) {
            Notification.show("Transkriptionsservice nicht verfügbar");
            return;
        }
        VoiceInputDialog voiceDialog = new VoiceInputDialog(transcriptionService);
        voiceDialog.setOnExtractionRequestedListener(transcribedText -> {
            voiceDialog.close();
            // Call extraction endpoint
            extractPatientData(transcribedText);
        });
        voiceDialog.open();
    }

    private void extractPatientData(String text) {
        if (text == null || text.trim().isEmpty()) {
            Notification.show("Kein Text zur Extraktion vorhanden", 3000, Notification.Position.MIDDLE);
            return;
        }

        Notification.show("Extraktion wird durchgeführt...", 2000, Notification.Position.MIDDLE);
        
        try {
            ExtractionResult<Patient> result = extractionClient.extractPatient(text);
            
            if (result != null && result.getEntity() != null) {
                // Show verification dialog
                EntityVerificationDialog<Patient> verificationDialog = 
                        new EntityVerificationDialog<>(result);
                
                verificationDialog.setOnConfirmedListener(confirmedPatient -> {
                    // Merge extracted data with existing form data
                    Patient currentPatient = form.getValue();

                    if (currentPatient == null) {
                        currentPatient = new Patient();
                        form.setValue(currentPatient);
                    } 
                    
                    // Update fields if extracted values are present
                    if (confirmedPatient.getFirstName() != null) {
                        currentPatient.setFirstName(confirmedPatient.getFirstName());
                    }
                    if (confirmedPatient.getLastName() != null) {
                        currentPatient.setLastName(confirmedPatient.getLastName());
                    }
                    if (confirmedPatient.getBirth() != null) {
                        currentPatient.setBirth(confirmedPatient.getBirth());
                    }
                    if (confirmedPatient.getAddress() != null) {
                        currentPatient.setAddress(confirmedPatient.getAddress());
                    }
                    if (confirmedPatient.getHealthInsurance() != null) {
                        currentPatient.setHealthInsurance(confirmedPatient.getHealthInsurance());
                    }
                    if (confirmedPatient.getInsuranceNumber() != null) {
                        currentPatient.setInsuranceNumber(confirmedPatient.getInsuranceNumber());
                    }
                    
                    form.setValue(currentPatient);
                    Notification.show("Patientendaten übernommen", 3000, Notification.Position.MIDDLE);
                });
                
                verificationDialog.open();
            } else {
                Notification.show("Extraktion fehlgeschlagen: Keine Daten gefunden", 5000,
                        Notification.Position.MIDDLE);
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("302")) {
                Notification.show("Authentifizierungsfehler: Bitte Seite neu laden und erneut versuchen", 5000,
                        Notification.Position.MIDDLE);
            } else {
                Notification.show("Fehler bei der Extraktion: " + (errorMsg != null ? errorMsg : e.getClass().getSimpleName()), 5000,
                        Notification.Position.MIDDLE);
            }
        }
    }

}