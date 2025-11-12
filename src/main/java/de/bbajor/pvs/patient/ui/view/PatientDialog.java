package de.bbajor.pvs.patient.ui.view;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.binder.ValidationException;

import de.bbajor.pvs.ai.extraction.ExtractionResult;
import de.bbajor.pvs.ai.service.ExtractionClient;
import de.bbajor.pvs.ai.service.VoiceTranscriptionService;
import de.bbajor.pvs.ai.ui.EntityVerificationDialog;
import de.bbajor.pvs.ai.ui.VoiceInputDialog;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.model.PatientHistory;
import de.bbajor.pvs.patient.model.PatientRecord;
import de.bbajor.pvs.patient.presenter.PatientPresenter;

public class PatientDialog extends Dialog {

    private List<PatientChangeListener> listeners = new ArrayList<>();
    private final Button saveButton = new Button();

    private final PatientPresenter presenter;
    private final PatientForm form;
    private final ExtractionClient extractionClient;
    private final VoiceTranscriptionService transcriptionService;
    private final de.bbajor.pvs.security.domain.UserAccountRepository userAccountRepository;

    public PatientDialog(PatientPresenter presenter, Patient patient) {
        this(presenter, patient, null, null, null);
    }

    public PatientDialog(PatientPresenter presenter, Patient patient, ExtractionClient extractionClient, 
            VoiceTranscriptionService transcriptionService, de.bbajor.pvs.security.domain.UserAccountRepository userAccountRepository) {
        this.extractionClient = extractionClient;
        this.transcriptionService = transcriptionService;
        this.presenter = presenter;
        this.userAccountRepository = userAccountRepository;
        if (patient == null) {
            patient = new Patient();
        }

        // Stelle sicher, dass InstitutionContext gesetzt ist
        ensureInstitutionContext();

        setWidth("1400px");
        setHeight("1000px");

        // Create the components
        form = new PatientForm(presenter.getHealthInsurances(), patient, 
                presenter.getLocations(), e -> valueChanged(e));

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
        
        // Create tabs for Stammdaten and Patientengeschichte
        Tab stammdatenTab = new Tab("Stammdaten");
        Tab geschichteTab = new Tab("Patientengeschichte");
        Tabs tabs = new Tabs(stammdatenTab, geschichteTab);
        
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        
        // Stammdaten tab content
        VerticalLayout stammdatenContent = new VerticalLayout(form);
        stammdatenContent.setSizeFull();
        stammdatenContent.setPadding(false);
        
        // Patientengeschichte tab content
        VerticalLayout geschichteContent = createPatientHistoryContent(patient);
        geschichteContent.setSizeFull();
        geschichteContent.setPadding(true);
        
        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            Tab selected = event.getSelectedTab();
            if (selected == stammdatenTab) {
                content.add(stammdatenContent);
            } else if (selected == geschichteTab) {
                content.add(geschichteContent);
            }
        });
        
        content.add(stammdatenContent);
        add(tabs, content);
        getFooter().add(cancelBtn, readBtn, voiceInputBtn, saveButton);
    }

    /**
     * Creates the patient history tab content with a list of patient records.
     */
    private VerticalLayout createPatientHistoryContent(Patient patient) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);
        
        if (patient == null || patient.getId() == null) {
            layout.add(new Span("Keine Patientengeschichte verfügbar. Bitte speichern Sie zuerst den Patienten."));
            return layout;
        }
        
        // Get patient history
        PatientHistory history = patient.getPatientHistory();
        List<PatientRecord> records = Collections.emptyList();
        
        if (history != null && history.getPatientRecords() != null) {
            records = history.getPatientRecords().stream()
                    .filter(PatientRecord::isActive)
                    .sorted((a, b) -> {
                        if (a.getDateOfRecord() == null && b.getDateOfRecord() == null) return 0;
                        if (a.getDateOfRecord() == null) return 1;
                        if (b.getDateOfRecord() == null) return -1;
                        return b.getDateOfRecord().compareTo(a.getDateOfRecord()); // Newest first
                    })
                    .toList();
        }
        
        if (records.isEmpty()) {
            layout.add(new Span("Keine Einträge in der Patientengeschichte vorhanden."));
            return layout;
        }
        
        // Create grid with patient records
        Grid<PatientRecord> grid = new Grid<>(PatientRecord.class, false);
        grid.setSizeFull();
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
        
        grid.addColumn(record -> record.getDateOfRecord() != null 
                ? record.getDateOfRecord().format(dateFormatter) : "-")
                .setHeader("Datum")
                .setAutoWidth(true);
        
        grid.addColumn(record -> record.getReasonForVisit() != null 
                ? (record.getReasonForVisit().getReason() != null ? record.getReasonForVisit().getReason() : "-") : "-")
                .setHeader("Grund")
                .setAutoWidth(true);
        
        grid.addColumn(record -> {
            if (record.getPatientAnamnesis() != null && record.getPatientAnamnesis().getAdditionalInformation() != null 
                    && !record.getPatientAnamnesis().getAdditionalInformation().isEmpty()) {
                return record.getPatientAnamnesis().getAdditionalInformation();
            }
            return record.getDescription() != null && !record.getDescription().isEmpty() 
                ? record.getDescription() : "-";
        })
        .setHeader("Beschreibung")
        .setAutoWidth(true)
        .setFlexGrow(1);
        
        // Extract diagnosis from anamnesis knownDiseases
        grid.addColumn(record -> {
            if (record.getPatientAnamnesis() != null && record.getPatientAnamnesis().getKnownDiseases() != null 
                    && !record.getPatientAnamnesis().getKnownDiseases().isEmpty()) {
                return record.getPatientAnamnesis().getKnownDiseases().stream()
                        .map(disease -> {
                            String diseaseName = disease.getName() != null ? disease.getName() : "-";
                            if (disease.getIcdEntry() != null) {
                                // Try to get ICD code from primary keys
                                String icdCode = null;
                                if (disease.getIcdEntry().getIcdPrimaryKeys1() != null 
                                        && !disease.getIcdEntry().getIcdPrimaryKeys1().isEmpty()) {
                                    icdCode = disease.getIcdEntry().getIcdPrimaryKeys1().iterator().next().getKeyNumber();
                                } else if (disease.getIcdEntry().getTextContent() != null 
                                        && !disease.getIcdEntry().getTextContent().isEmpty()) {
                                    // Fallback to text content if no primary key available
                                    icdCode = disease.getIcdEntry().getTextContent();
                                }
                                if (icdCode != null && !icdCode.isEmpty()) {
                                    return diseaseName + " (" + icdCode + ")";
                                }
                            }
                            return diseaseName;
                        })
                        .filter(name -> name != null && !name.isEmpty() && !name.equals("-"))
                        .findFirst()
                        .orElse("-");
            }
            return "-";
        })
        .setHeader("Diagnose")
        .setAutoWidth(true);
        
        // IOP is not yet stored in the data structure - placeholder for future extension
        grid.addColumn(record -> {
            // TODO: Extract IOP from anamnesis when data structure is extended
            // For now, check if IOP is mentioned in description or additionalInformation
            String text = "";
            if (record.getDescription() != null) {
                text += record.getDescription() + " ";
            }
            if (record.getPatientAnamnesis() != null && record.getPatientAnamnesis().getAdditionalInformation() != null) {
                text += record.getPatientAnamnesis().getAdditionalInformation() + " ";
            }
            // Try to extract IOP value from text (simple pattern matching)
            if (text.matches(".*IOP.*?\\d+.*") || text.matches(".*Augeninnendruck.*?\\d+.*")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:IOP|Augeninnendruck).*?(\\d+)\\s*mmHg?");
                java.util.regex.Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            return "-";
        })
        .setHeader("IOP (mmHg)")
        .setAutoWidth(true);
        
        grid.setItems(records);
        
        // Add double-click listener to open detail view
        grid.addItemDoubleClickListener(event -> {
            PatientRecord record = event.getItem();
            if (record != null) {
                openPatientRecordDetail(patient, record);
            }
        });
        
        layout.add(grid);
        return layout;
    }

    /**
     * Opens a dialog with the ophthalmology appointment view for a specific patient record.
     * Shows all details similar to OphthalmologyAppointmentView.
     */
    private void openPatientRecordDetail(Patient patient, PatientRecord record) {
        Dialog detailDialog = new Dialog();
        detailDialog.setWidth("90%");
        detailDialog.setHeight("90%");
        detailDialog.setHeaderTitle("Patientenakte - " + 
            (record.getDateOfRecord() != null 
                ? record.getDateOfRecord().format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN))
                : "Unbekanntes Datum"));
        
        // Create content similar to OphthalmologyAppointmentView
        com.vaadin.flow.component.tabs.TabSheet tabSheet = new com.vaadin.flow.component.tabs.TabSheet();
        tabSheet.setSizeFull();
        
        // Anamnese Tab
        com.vaadin.flow.component.orderedlayout.VerticalLayout anamnesisLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
        anamnesisLayout.setPadding(true);
        anamnesisLayout.setSpacing(true);
        
        if (record.getPatientAnamnesis() != null && record.getPatientAnamnesis().getAdditionalInformation() != null 
                && !record.getPatientAnamnesis().getAdditionalInformation().isEmpty()) {
            com.vaadin.flow.component.textfield.TextArea anamnesisText = new com.vaadin.flow.component.textfield.TextArea("Allgemeine Anamnese");
            anamnesisText.setValue(record.getPatientAnamnesis().getAdditionalInformation());
            anamnesisText.setReadOnly(true);
            anamnesisText.setWidthFull();
            anamnesisText.setMinHeight("150px");
            anamnesisLayout.add(anamnesisText);
        } else if (record.getDescription() != null && !record.getDescription().isEmpty()) {
            com.vaadin.flow.component.textfield.TextArea anamnesisText = new com.vaadin.flow.component.textfield.TextArea("Allgemeine Anamnese");
            anamnesisText.setValue(record.getDescription());
            anamnesisText.setReadOnly(true);
            anamnesisText.setWidthFull();
            anamnesisText.setMinHeight("150px");
            anamnesisLayout.add(anamnesisText);
        } else {
            anamnesisLayout.add(new Span("Keine Anamnese verfügbar."));
        }
        
        // Known Diseases
        if (record.getPatientAnamnesis() != null && record.getPatientAnamnesis().getKnownDiseases() != null 
                && !record.getPatientAnamnesis().getKnownDiseases().isEmpty()) {
            com.vaadin.flow.component.html.H3 diseasesHeader = new com.vaadin.flow.component.html.H3("Bekannte Erkrankungen");
            anamnesisLayout.add(diseasesHeader);
            for (de.bbajor.pvs.patient.model.Disease disease : record.getPatientAnamnesis().getKnownDiseases()) {
                String diseaseText = disease.getName() != null ? disease.getName() : "-";
                if (disease.getIcdEntry() != null) {
                    // Try to get ICD code from primary keys
                    String icdCode = null;
                    if (disease.getIcdEntry().getIcdPrimaryKeys1() != null 
                            && !disease.getIcdEntry().getIcdPrimaryKeys1().isEmpty()) {
                        icdCode = disease.getIcdEntry().getIcdPrimaryKeys1().iterator().next().getKeyNumber();
                    } else if (disease.getIcdEntry().getTextContent() != null 
                            && !disease.getIcdEntry().getTextContent().isEmpty()) {
                        // Fallback to text content if no primary key available
                        icdCode = disease.getIcdEntry().getTextContent();
                    }
                    if (icdCode != null && !icdCode.isEmpty()) {
                        diseaseText += " (ICD: " + icdCode + ")";
                    }
                }
                anamnesisLayout.add(new Span("• " + diseaseText));
            }
        }
        
        tabSheet.add("Anamnese", anamnesisLayout);
        
        // Augenvordergrund Tab (placeholder - would need extended data structure)
        com.vaadin.flow.component.orderedlayout.VerticalLayout anteriorLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
        anteriorLayout.setPadding(true);
        anteriorLayout.add(new Span("Detaillierte Befunde zum Augenvordergrund werden hier angezeigt, sobald die Datenstruktur erweitert wurde."));
        tabSheet.add("Augenvordergrund", anteriorLayout);
        
        // Augenhintergrund Tab (placeholder - would need extended data structure)
        com.vaadin.flow.component.orderedlayout.VerticalLayout posteriorLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
        posteriorLayout.setPadding(true);
        posteriorLayout.add(new Span("Detaillierte Befunde zum Augenhintergrund werden hier angezeigt, sobald die Datenstruktur erweitert wurde."));
        tabSheet.add("Augenhintergrund", posteriorLayout);
        
        // Weitere Details Tab
        com.vaadin.flow.component.orderedlayout.VerticalLayout otherDetailsLayout = new com.vaadin.flow.component.orderedlayout.VerticalLayout();
        otherDetailsLayout.setPadding(true);
        otherDetailsLayout.setSpacing(true);
        
        if (record.getDateOfRecord() != null) {
            otherDetailsLayout.add(new Span("Datum: " + 
                record.getDateOfRecord().format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN))));
        }
        
        if (record.getReasonForVisit() != null) {
            String reason = record.getReasonForVisit().getReason() != null 
                ? record.getReasonForVisit().getReason() : "-";
            otherDetailsLayout.add(new Span("Grund des Besuchs: " + reason));
            if (record.getReasonForVisit().getDescription() != null && !record.getReasonForVisit().getDescription().isEmpty()) {
                otherDetailsLayout.add(new Span("Beschreibung: " + record.getReasonForVisit().getDescription()));
            }
        }
        
        if (record.getDescription() != null && !record.getDescription().isEmpty()) {
            com.vaadin.flow.component.textfield.TextArea notes = new com.vaadin.flow.component.textfield.TextArea("Zusätzliche Hinweise");
            notes.setValue(record.getDescription());
            notes.setReadOnly(true);
            notes.setWidthFull();
            notes.setMinHeight("150px");
            otherDetailsLayout.add(notes);
        }
        
        tabSheet.add("Weitere Details", otherDetailsLayout);
        
        detailDialog.add(tabSheet);
        
        Button closeButton = new Button("Schließen", e -> detailDialog.close());
        detailDialog.getFooter().add(closeButton);
        
        detailDialog.open();
    }

    public void addChangeListener(PatientChangeListener listener) {
        listeners.add(listener);
    }

    protected void notifyListeners() {
        listeners.forEach(e -> e.onPatientChanged(form.getValue()));
    }

    private void save() throws ValidationException {
        // Stelle sicher, dass InstitutionContext gesetzt ist vor dem Speichern
        ensureInstitutionContext();
        
        if (form.isValidateOk()) {
            form.writeIfValid();
            presenter.savePatient(form.getValue());
            notifyListeners();
            close();
        } else {
            Notification.show("Es fehlen noch Angaben. Bitte ergänzen.");
        }
    }
    
    /**
     * Stellt sicher, dass der InstitutionContext gesetzt ist.
     * Dies ist notwendig, da Vaadin Button-Klicks kein BeforeEnterEvent auslösen,
     * sodass der Context möglicherweise nicht gesetzt ist.
     */
    private void ensureInstitutionContext() {
        // Nur setzen, wenn noch nicht gesetzt
        if (de.bbajor.pvs.institution.context.InstitutionContext.hasInstitution()) {
            return;
        }
        
        org.springframework.security.core.Authentication authentication = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof de.bbajor.pvs.institution.security.InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                de.bbajor.pvs.institution.context.InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof 
                   de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter adapter) {
            // Authentication wurde aus Session deserialisiert
            if (userAccountRepository != null) {
                try {
                    String username = adapter.getUsername();
                    de.bbajor.pvs.security.domain.UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                    
                    if (userAccount != null && userAccount.getInstitution() != null) {
                        Long institutionId = userAccount.getInstitution().getId();
                        de.bbajor.pvs.institution.context.InstitutionContext.setInstitutionId(institutionId);
                    }
                } catch (Exception e) {
                    // Fehler beim Wiederherstellen des Contexts - ignorieren
                }
            }
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